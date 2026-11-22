package com.daljoalab.memorycapsule.feature.conversation

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daljoalab.memorycapsule.BuildConfig
import com.daljoalab.memorycapsule.core.data.repository.ConversationRepository
import com.daljoalab.memorycapsule.core.model.ApiError
import com.daljoalab.memorycapsule.core.model.ApiException
import com.daljoalab.memorycapsule.core.model.Decision
import com.daljoalab.memorycapsule.core.model.LiveSession
import com.daljoalab.memorycapsule.core.model.RiskLevel
import com.daljoalab.memorycapsule.core.model.SessionEndReason
import com.daljoalab.memorycapsule.core.model.SessionEndResult
import com.daljoalab.memorycapsule.core.model.TranscriptDirection
import com.daljoalab.memorycapsule.core.model.TranscriptLine
import com.daljoalab.memorycapsule.core.model.Utterance
import com.daljoalab.memorycapsule.core.voice.LiveEvent
import com.daljoalab.memorycapsule.core.voice.LiveVoiceClient
import com.daljoalab.memorycapsule.core.voice.LiveVoiceException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val liveVoiceClient: LiveVoiceClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    private var session: LiveSession? = null
    private var endReason = SessionEndReason.USER
    private var startedAtMs = 0L
    private var usageSeconds: Double? = null

    private val inputLines = mutableListOf<TranscriptLine>()
    private val outputLines = mutableListOf<TranscriptLine>()
    private val allInput = StringBuilder()
    private val allOutput = StringBuilder()
    private val openTurn = StringBuilder()
    private var openTurnStartMs = 0L
    private var openTurnLastDeltaMs = 0L
    private var lastInputDeltaStartMs: Long? = null
    private var answerStartedDuringTurn = false
    private var turnCount = 0
    private var lastOutputAtMs = 0L

    private val sessionJobs = mutableListOf<Job>()
    private val pendingDecisions = mutableListOf<Job>()
    private var silenceJob: Job? = null
    private var answerIdleJob: Job? = null
    private var endIntentJob: Job? = null
    private var finishing = false

    init {
        viewModelScope.launch { liveVoiceClient.events.collect(::handle) }
    }

    fun startConversation() {
        val phase = _uiState.value.phase
        if (phase !is ConversationPhase.Ready && phase !is ConversationPhase.Finished && phase !is ConversationPhase.Failed) {
            return
        }
        resetSession()
        _uiState.value = ConversationUiState(phase = ConversationPhase.Connecting)
        sessionJobs += viewModelScope.launch {
            try {
                val offer = liveVoiceClient.createOffer()
                val created = conversationRepository.createSession(offer, BuildConfig.VERSION_NAME)
                session = created
                liveVoiceClient.applyAnswer(created.answerSdp)
                sessionJobs += launch {
                    delay(SESSION_START_TIMEOUT_MS)
                    if (_uiState.value.phase == ConversationPhase.Connecting) {
                        fail("대화 시작 응답을 받지 못했어요. 잠시 후 다시 해 주세요.")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logDebug("연결 실패", e)
                fail(e.toUserMessage())
            }
        }
    }

    fun onMicPermissionDenied(permanently: Boolean) {
        val message = if (permanently) {
            "마이크가 꺼져 있어요.\n설정에서 마이크를 허용해 주세요."
        } else {
            "마이크가 꺼져 있어요.\n마이크를 켜야 기억이와\n이야기할 수 있어요."
        }
        _uiState.update { it.copy(phase = ConversationPhase.Failed(message, needsMicSettings = permanently)) }
    }

    fun stopConversation() = stop(SessionEndReason.USER)

    fun returnToReady() {
        val phase = _uiState.value.phase
        if (phase is ConversationPhase.Finished || phase is ConversationPhase.Failed) {
            _uiState.value = ConversationUiState()
        }
    }

    private fun stop(reason: SessionEndReason) {
        if (!_uiState.value.phase.isTalking) return
        endReason = reason
        _uiState.update { it.copy(phase = ConversationPhase.Closing) }
        liveVoiceClient.requestClose()
        sessionJobs += viewModelScope.launch {
            delay(CLOSE_WAIT_MS)
            finish()
        }
    }

    private fun handle(event: LiveEvent) {
        when (event) {
            LiveEvent.Started -> onStarted()
            is LiveEvent.InputDelta -> onInputDelta(event)
            is LiveEvent.InputDone -> finalizeTurn(endMs = event.endMs)
            is LiveEvent.OutputDelta -> onOutputDelta(event)
            is LiveEvent.UsageUpdated -> usageSeconds = event.seconds
            is LiveEvent.Closed -> {
                event.usageSeconds?.let { usageSeconds = it }
                onConnectionEnded()
            }
            LiveEvent.Disconnected -> onConnectionEnded()
        }
    }

    private fun onStarted() {
        if (_uiState.value.phase != ConversationPhase.Connecting) return
        startedAtMs = SystemClock.elapsedRealtime()
        _uiState.update { it.copy(phase = ConversationPhase.Listening) }
        val maxSeconds = session?.maxSeconds ?: return
        sessionJobs += viewModelScope.launch {
            delay(maxSeconds * 1000L)
            stop(SessionEndReason.LIMIT)
        }
    }

    private fun onConnectionEnded() {
        when (_uiState.value.phase) {
            ConversationPhase.Closing -> finish()
            ConversationPhase.Listening, ConversationPhase.Answering -> {
                endReason = SessionEndReason.ERROR
                finish()
            }
            ConversationPhase.Connecting -> fail("음성 연결이 끊겼어요. 다시 해 주세요.")
            else -> Unit
        }
    }

    private fun onInputDelta(event: LiveEvent.InputDelta) {
        if (!_uiState.value.phase.isTalking) return
        val newTurn = openTurn.isEmpty()
        if (newTurn) openTurnStartMs = event.startMs ?: elapsedMs()
        val previousStartMs = lastInputDeltaStartMs
        val paused = event.startMs != null && previousStartMs != null &&
            event.startMs - previousStartMs > PAUSE_SPACING_MS
        lastInputDeltaStartMs = event.startMs ?: previousStartMs
        openTurn.appendSeparated(event.text, separate = paused)
        allInput.appendSeparated(event.text, separate = paused || newTurn)
        openTurnLastDeltaMs = elapsedMs()
        _uiState.update { it.copy(phase = ConversationPhase.Listening, seniorText = openTurn.toString()) }

        scheduleTurnEnd(if (answerStartedDuringTurn) ANSWER_GRACE_MS else SILENCE_TURN_END_MS)
    }

    private fun scheduleTurnEnd(delayMs: Long) {
        silenceJob?.cancel()
        silenceJob = viewModelScope.launch {
            delay(delayMs)
            finalizeTurn(endMs = null)
        }
    }

    private fun onOutputDelta(event: LiveEvent.OutputDelta) {
        if (!_uiState.value.phase.isTalking) return
        if (openTurn.isNotEmpty() && !answerStartedDuringTurn) {
            answerStartedDuringTurn = true
            scheduleTurnEnd(ANSWER_GRACE_MS)
        }
        val now = SystemClock.elapsedRealtime()
        val newAnswer = now - lastOutputAtMs > ANSWER_IDLE_MS
        lastOutputAtMs = now
        allOutput.appendSeparated(event.text, separate = newAnswer)

        val elapsed = elapsedMs()
        val last = outputLines.lastOrNull()
        if (newAnswer || last == null) {
            outputLines += TranscriptLine(TranscriptDirection.OUTPUT, null, event.text, elapsed, elapsed)
        } else {
            outputLines[outputLines.lastIndex] = last.copy(text = last.text + event.text, endMs = elapsed)
        }

        _uiState.update {
            it.copy(
                phase = ConversationPhase.Answering,
                assistantText = if (newAnswer) event.text else it.assistantText + event.text,
            )
        }
        answerIdleJob?.cancel()
        answerIdleJob = viewModelScope.launch {
            delay(ANSWER_IDLE_MS)
            _uiState.update {
                if (it.phase == ConversationPhase.Answering) it.copy(phase = ConversationPhase.Listening, seniorText = "") else it
            }
        }
    }

    private fun finalizeTurn(endMs: Long?) {
        silenceJob?.cancel()
        silenceJob = null
        answerStartedDuringTurn = false
        val text = openTurn.toString().trim().takeLast(MAX_UTTERANCE_CHARS)
        openTurn.clear()
        val currentSession = session ?: return
        if (text.isEmpty()) return

        val startMs = openTurnStartMs.coerceAtLeast(0)
        val utterance = Utterance(text, startMs, (endMs ?: openTurnLastDeltaMs).coerceAtLeast(startMs))
        turnCount += 1
        val utteranceId = String.format(Locale.ROOT, "u_%04d", turnCount)
        inputLines += TranscriptLine(TranscriptDirection.INPUT, utteranceId, text, utterance.startMs, utterance.endMs)

        val inputText = allInput.toString()
        val outputText = allOutput.toString()
        val job = viewModelScope.launch {
            val decision = retrying(DECISION_RETRY_DELAYS_MS, ::isRetryableDecisionError) {
                conversationRepository.decideFinal(
                    sessionId = currentSession.id,
                    utteranceId = utteranceId,
                    inputText = inputText,
                    outputText = outputText,
                    previousReaction = _uiState.value.reaction,
                    utterance = utterance,
                )
            }
            decision?.let(::applyDecision)
        }
        pendingDecisions += job
        job.invokeOnCompletion { pendingDecisions.remove(job) }
    }

    private fun applyDecision(decision: Decision) {
        if (decision.needsReview || decision.riskLevel == RiskLevel.UNKNOWN) return
        decision.reaction?.let { reaction -> _uiState.update { it.copy(reaction = reaction) } }
        SafetyPhrases.noticeFor(decision.safetyPhraseId, decision.riskLevel)?.let { notice ->
            _uiState.update { it.copy(safetyNotice = notice) }
        }
        val calm = decision.riskLevel == RiskLevel.R0 || decision.riskLevel == RiskLevel.R1
        if (calm && decision.endIntent) requestEndAfterGoodbye()
    }

    fun dismissSafetyNotice() {
        _uiState.update { it.copy(safetyNotice = null) }
    }

    private fun requestEndAfterGoodbye() {
        if (endIntentJob != null) return
        val requestedAt = SystemClock.elapsedRealtime()
        endIntentJob = viewModelScope.launch {
            while (_uiState.value.phase.isTalking) {
                delay(300)
                val now = SystemClock.elapsedRealtime()
                val saidGoodbye = lastOutputAtMs > requestedAt
                val quiet = now - maxOf(lastOutputAtMs, requestedAt) > ANSWER_IDLE_MS
                if ((saidGoodbye && quiet) || now - requestedAt > GOODBYE_MAX_WAIT_MS) {
                    stop(SessionEndReason.USER)
                    return@launch
                }
            }
        }
    }

    private fun finish() {
        if (finishing) return
        finishing = true
        finalizeTurn(endMs = null)
        cancelSessionTimers()
        liveVoiceClient.close()

        val currentSession = session ?: run {
            _uiState.update { it.copy(phase = ConversationPhase.Ready) }
            return
        }
        _uiState.update { it.copy(phase = ConversationPhase.Saving) }
        viewModelScope.launch {
            withTimeoutOrNull(DECISION_WAIT_MS) { pendingDecisions.toList().joinAll() }
            val durationSec = (usageSeconds ?: (elapsedMs() / 1000.0)).toInt()
            val result: SessionEndResult? = retrying(END_RETRY_DELAYS_MS, ::isRetryableEndError) {
                conversationRepository.endSession(
                    sessionId = currentSession.id,
                    reason = endReason,
                    durationSec = durationSec,
                    usageSeconds = usageSeconds,
                    transcript = (inputLines + outputLines).sortedBy { it.startMs },
                )
            }
            _uiState.update { it.copy(phase = ConversationPhase.Finished(result?.cardsCreated)) }
        }
    }

    private fun fail(message: String) {
        cancelSessionTimers()
        liveVoiceClient.close()
        _uiState.update { it.copy(phase = ConversationPhase.Failed(message)) }
    }

    private fun resetSession() {
        cancelSessionTimers()
        session = null
        endReason = SessionEndReason.USER
        startedAtMs = 0L
        usageSeconds = null
        inputLines.clear()
        outputLines.clear()
        allInput.clear()
        allOutput.clear()
        openTurn.clear()
        openTurnLastDeltaMs = 0L
        lastInputDeltaStartMs = null
        answerStartedDuringTurn = false
        turnCount = 0
        lastOutputAtMs = 0L
        finishing = false
    }

    private fun cancelSessionTimers() {
        sessionJobs.forEach { it.cancel() }
        sessionJobs.clear()
        listOfNotNull(silenceJob, answerIdleJob, endIntentJob).forEach { it.cancel() }
        silenceJob = null
        answerIdleJob = null
        endIntentJob = null
    }

    private fun elapsedMs(): Long =
        if (startedAtMs == 0L) 0L else SystemClock.elapsedRealtime() - startedAtMs

    private suspend fun <T> retrying(
        delaysMs: List<Long>,
        isRetryable: (ApiError) -> Boolean,
        block: suspend () -> T,
    ): T? {
        delaysMs.forEachIndexed { attempt, delayMs ->
            delay(delayMs)
            try {
                return block()
            } catch (e: ApiException) {
                logDebug("요청 실패 (${attempt + 1}회)", e)
                if (!isRetryable(e.error)) return null
            }
        }
        return null
    }

    override fun onCleared() {
        liveVoiceClient.close()
    }

    private companion object {
        const val TAG = "Conversation"
        const val SESSION_START_TIMEOUT_MS = 25_000L
        const val CLOSE_WAIT_MS = 4_000L
        const val SILENCE_TURN_END_MS = 2_000L
        const val ANSWER_IDLE_MS = 1_800L
        const val ANSWER_GRACE_MS = 1_500L
        const val PAUSE_SPACING_MS = 700L
        const val GOODBYE_MAX_WAIT_MS = 8_000L
        const val DECISION_WAIT_MS = 15_000L
        const val MAX_UTTERANCE_CHARS = 1600
        val DECISION_RETRY_DELAYS_MS = listOf(0L, 1_000L, 2_000L, 4_000L)
        val END_RETRY_DELAYS_MS = listOf(0L, 2_000L, 4_000L, 8_000L)

        fun isRetryableDecisionError(error: ApiError): Boolean = when (error) {
            is ApiError.HttpError -> error.status == 503 || error.status == 429
            ApiError.Timeout, ApiError.NoConnection -> true
            ApiError.InvalidResponse -> false
        }

        fun isRetryableEndError(error: ApiError): Boolean = when (error) {
            is ApiError.HttpError -> error.status == 503 ||
                (error.status == 409 && error.code == "decision_in_progress")
            ApiError.Timeout, ApiError.NoConnection -> true
            ApiError.InvalidResponse -> false
        }

        fun Exception.toUserMessage(): String = when (this) {
            is LiveVoiceException -> message ?: DEFAULT_ERROR
            is ApiException -> (error as? ApiError.HttpError)?.message ?: when (error) {
                ApiError.NoConnection -> "인터넷 연결을 확인해 주세요."
                ApiError.Timeout -> "응답이 늦어지고 있어요. 잠시 후 다시 해 주세요."
                else -> DEFAULT_ERROR
            }
            else -> DEFAULT_ERROR
        }

        const val DEFAULT_ERROR = "지금은 연결이 어려워요. 잠시 후 다시 해 주세요."

        fun StringBuilder.appendSeparated(text: String, separate: Boolean) {
            val needsSpace = separate && isNotEmpty() && !last().isWhitespace() && !text.first().isWhitespace()
            if (needsSpace) append(' ')
            append(text)
        }

        fun logDebug(message: String, e: Throwable) {
            if (BuildConfig.DEBUG) Log.w(TAG, message, e)
        }
    }
}
