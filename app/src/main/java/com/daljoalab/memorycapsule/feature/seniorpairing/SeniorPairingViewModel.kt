package com.daljoalab.memorycapsule.feature.seniorpairing

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daljoalab.memorycapsule.core.data.repository.PairingRepository
import com.daljoalab.memorycapsule.core.data.repository.UserRepository
import com.daljoalab.memorycapsule.core.model.ConsentType
import com.daljoalab.memorycapsule.core.model.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SeniorPairingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val pairingRepository: PairingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SeniorPairingUiState>(SeniorPairingUiState.Loading)
    val uiState: StateFlow<SeniorPairingUiState> = _uiState.asStateFlow()

    private var knownLinkIds: Set<String> = emptySet()
    private var codeJob: Job? = null

    init {
        load()
    }

    fun load() {
        _uiState.value = SeniorPairingUiState.Loading
        viewModelScope.launch {
            runCatchingToFailure {
                val links = pairingRepository.getLinks()
                if (links.isEmpty()) {
                    startPairing()
                } else {
                    _uiState.value = SeniorPairingUiState.AlreadyLinked(links.map { it.displayName })
                }
            }
        }
    }

    fun addAnotherGuardian() {
        _uiState.value = SeniorPairingUiState.Loading
        viewModelScope.launch { runCatchingToFailure { startPairing() } }
    }

    private suspend fun startPairing() {
        val granted = userRepository.getMe()?.grantedConsents.orEmpty()
        if (granted.containsAll(SHARE_CONSENTS)) issueCode() else _uiState.value = SeniorPairingUiState.NeedsConsent
    }

    fun grantShareConsent() {
        _uiState.value = SeniorPairingUiState.Loading
        viewModelScope.launch {
            runCatchingToFailure {
                userRepository.grantConsents(SHARE_CONSENTS)
                issueCode()
            }
        }
    }

    fun reissueCode() {
        _uiState.value = SeniorPairingUiState.Loading
        viewModelScope.launch { runCatchingToFailure { issueCode() } }
    }

    private suspend fun issueCode() {
        knownLinkIds = pairingRepository.getLinks().map { it.linkId }.toSet()
        val code = pairingRepository.issueCode()
        val expiresAt = SystemClock.elapsedRealtime() + code.validForMillis
        codeJob?.cancel()
        codeJob = viewModelScope.launch {
            var lastLinkCheck = 0L
            while (isActive) {
                val now = SystemClock.elapsedRealtime()
                val remaining = ((expiresAt - now) / 1000).coerceAtLeast(0)
                _uiState.value = SeniorPairingUiState.ShowingCode(code.code, remaining.toInt())
                if (now - lastLinkCheck >= LINK_CHECK_INTERVAL_MS) {
                    lastLinkCheck = now
                    if (checkLinked()) return@launch
                }
                if (remaining == 0L) return@launch
                delay(1_000)
            }
        }
    }

    private suspend fun checkLinked(): Boolean {
        val newLink = runCatching { pairingRepository.getLinks() }.getOrNull()
            ?.firstOrNull { it.linkId !in knownLinkIds }
            ?: return false
        _uiState.value = SeniorPairingUiState.Linked(newLink.displayName)
        return true
    }

    private suspend fun runCatchingToFailure(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _uiState.value = SeniorPairingUiState.Failed(e.toUserMessage())
        }
    }

    private companion object {
        const val LINK_CHECK_INTERVAL_MS = 3_000L
        val SHARE_CONSENTS = listOf(ConsentType.GUARDIAN_SHARE, ConsentType.SCHEDULE_SHARE)
    }
}

sealed interface SeniorPairingUiState {
    data object Loading : SeniorPairingUiState
    data object NeedsConsent : SeniorPairingUiState

    data class AlreadyLinked(val guardianNames: List<String?>) : SeniorPairingUiState

    data class ShowingCode(val code: String, val remainingSeconds: Int) : SeniorPairingUiState

    data class Linked(val guardianName: String?) : SeniorPairingUiState
    data class Failed(val message: String) : SeniorPairingUiState
}
