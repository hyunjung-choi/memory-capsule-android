package com.daljoalab.memorycapsule.feature.senioronboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daljoalab.memorycapsule.core.data.repository.AuthRepository
import com.daljoalab.memorycapsule.core.data.repository.UserRepository
import com.daljoalab.memorycapsule.core.model.ConsentType
import com.daljoalab.memorycapsule.core.model.UserRole
import com.daljoalab.memorycapsule.core.model.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SeniorOnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeniorOnboardingUiState())
    val uiState: StateFlow<SeniorOnboardingUiState> = _uiState.asStateFlow()

    fun onPrivacyCheckedChange(checked: Boolean) = _uiState.update { it.copy(privacyChecked = checked) }

    fun confirmConsent() {
        if (!_uiState.value.privacyChecked) return
        _uiState.update { it.copy(step = SeniorOnboardingStep.NAME) }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name.take(MAX_NAME_LENGTH), errorMessage = null) }

    fun submit() {
        val state = _uiState.value
        if (state.isSubmitting || state.name.isBlank()) return
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                authRepository.signInAnonymously()
                val me = userRepository.getMe() ?: userRepository.register(UserRole.SENIOR, state.name)
                if (me.role != UserRole.SENIOR) {
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = "보호자로 가입된 계정이에요.") }
                    return@launch
                }
                userRepository.grantConsents(VOICE_CONSENTS)
                _uiState.update { it.copy(isSubmitting = false, completed = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmitting = false, errorMessage = e.toUserMessage()) }
            }
        }
    }

    private companion object {
        const val MAX_NAME_LENGTH = 20
        val VOICE_CONSENTS = listOf(ConsentType.VOICE_TRANSFER, ConsentType.TRANSCRIPT_STORE, ConsentType.MEMORY_CARD)
    }
}

enum class SeniorOnboardingStep { CONSENT, NAME }

data class SeniorOnboardingUiState(
    val step: SeniorOnboardingStep = SeniorOnboardingStep.CONSENT,
    val privacyChecked: Boolean = false,
    val name: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val completed: Boolean = false,
)
