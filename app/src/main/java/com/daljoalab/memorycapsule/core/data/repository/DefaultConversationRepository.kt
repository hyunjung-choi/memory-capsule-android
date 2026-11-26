package com.daljoalab.memorycapsule.core.data.repository

import com.daljoalab.memorycapsule.core.model.Decision
import com.daljoalab.memorycapsule.core.model.LiveSession
import com.daljoalab.memorycapsule.core.model.Reaction
import com.daljoalab.memorycapsule.core.model.RiskLevel
import com.daljoalab.memorycapsule.core.model.SessionEndReason
import com.daljoalab.memorycapsule.core.model.SessionEndResult
import com.daljoalab.memorycapsule.core.model.TranscriptDirection
import com.daljoalab.memorycapsule.core.model.TranscriptLine
import com.daljoalab.memorycapsule.core.model.Utterance
import com.daljoalab.memorycapsule.core.network.datasource.ConversationNetworkDataSource
import com.daljoalab.memorycapsule.core.network.model.NetworkClient
import com.daljoalab.memorycapsule.core.network.model.NetworkDecision
import com.daljoalab.memorycapsule.core.network.model.NetworkDecisionRequest
import com.daljoalab.memorycapsule.core.network.model.NetworkSessionCreateRequest
import com.daljoalab.memorycapsule.core.network.model.NetworkSessionCreateResponse
import com.daljoalab.memorycapsule.core.network.model.NetworkSessionEndRequest
import com.daljoalab.memorycapsule.core.network.model.NetworkSessionEndResponse
import com.daljoalab.memorycapsule.core.network.model.NetworkTranscriptPart
import com.daljoalab.memorycapsule.core.network.model.NetworkUtterance
import com.daljoalab.memorycapsule.core.network.model.NetworkVoiceUsage
import javax.inject.Inject

class DefaultConversationRepository @Inject constructor(
    private val network: ConversationNetworkDataSource,
) : ConversationRepository {

    override suspend fun createSession(offerSdp: String, appVersion: String): LiveSession =
        network.createSession(
            NetworkSessionCreateRequest(
                sdp = offerSdp,
                client = NetworkClient(platform = "android", appVersion = appVersion),
            ),
        ).toModel()

    override suspend fun decideFinal(
        sessionId: String,
        utteranceId: String,
        inputText: String,
        outputText: String,
        previousReaction: Reaction,
        utterance: Utterance,
    ): Decision =
        network.decide(
            NetworkDecisionRequest(
                sessionId = sessionId,
                utteranceId = utteranceId,
                inputText = inputText.takeLast(MAX_INPUT_CHARS),
                outputText = outputText.takeLast(MAX_OUTPUT_CHARS),
                previousReaction = previousReaction.toNetwork(),
                final = true,
                utterance = NetworkUtterance(
                    text = utterance.text.takeLast(MAX_INPUT_CHARS),
                    startMs = utterance.startMs,
                    endMs = utterance.endMs,
                ),
            ),
        ).toModel()

    override suspend fun endSession(
        sessionId: String,
        reason: SessionEndReason,
        durationSec: Int,
        usageSeconds: Double?,
        transcript: List<TranscriptLine>,
    ): SessionEndResult =
        network.endSession(
            sessionId,
            NetworkSessionEndRequest(
                endReason = reason.name.lowercase(),
                durationSec = durationSec.coerceIn(0, MAX_SECONDS),
                usage = usageSeconds?.let { NetworkVoiceUsage(it.coerceIn(0.0, MAX_SECONDS.toDouble())) },
                transcript = transcript
                    .filter { it.text.isNotBlank() }
                    .takeLast(MAX_TRANSCRIPT_LINES)
                    .map { it.toNetwork() },
            ),
        ).toModel()

    private companion object {
        const val MAX_INPUT_CHARS = 1600
        const val MAX_OUTPUT_CHARS = 800
        const val MAX_LINE_CHARS = 16000
        const val MAX_TRANSCRIPT_LINES = 2000
        const val MAX_SECONDS = 86400

        fun NetworkSessionCreateResponse.toModel() = LiveSession(
            id = session.id,
            maxSeconds = session.maxSeconds,
            answerSdp = transport.sdp,
        )

        fun NetworkDecision.toModel() = Decision(
            reaction = reaction.toReaction(),
            riskLevel = when (riskLevel) {
                "R0" -> RiskLevel.R0
                "R1" -> RiskLevel.R1
                "R2" -> RiskLevel.R2
                "R3" -> RiskLevel.R3
                else -> RiskLevel.UNKNOWN
            },
            safetyPhraseId = safetyPhraseId,
            needsReview = assessmentStatus == "needs_review",
            endIntent = endIntent == "END",
        )

        fun String?.toReaction(): Reaction? = when (this) {
            "neutral" -> Reaction.NEUTRAL
            "happy" -> Reaction.HAPPY
            "empathy" -> Reaction.EMPATHY
            "surprised_happy" -> Reaction.SURPRISED_HAPPY
            "surprised_concern" -> Reaction.SURPRISED_CONCERN
            else -> null
        }

        fun Reaction.toNetwork(): String = name.lowercase()

        fun TranscriptLine.toNetwork() = NetworkTranscriptPart(
            direction = direction.name.lowercase(),
            utteranceId = utteranceId,
            text = text.take(MAX_LINE_CHARS),
            startMs = startMs.coerceAtLeast(0),
            endMs = endMs.coerceAtLeast(startMs.coerceAtLeast(0)),
        )

        fun NetworkSessionEndResponse.toModel() = SessionEndResult(
            cardsCreated = cardsCreated,
            cardsUpdated = cardsUpdated,
        )
    }
}
