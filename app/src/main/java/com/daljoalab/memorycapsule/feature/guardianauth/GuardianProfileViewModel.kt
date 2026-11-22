package com.daljoalab.memorycapsule.feature.guardianauth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class GuardianProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GuardianProfileUiState())
    val uiState: StateFlow<GuardianProfileUiState> = _uiState.asStateFlow()

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name.take(MAX_NAME_LENGTH), errorMessage = null) }

    fun onTermsCheckedChange(checked: Boolean) = _uiState.update { it.copy(termsChecked = checked) }

    fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val me = userRepository.getMe() ?: userRepository.register(UserRole.GUARDIAN, state.name)
                if (me.role != UserRole.GUARDIAN) {
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = "어르신으로 가입된 계정이에요.") }
                    return@launch
                }
                userRepository.grantConsents(listOf(ConsentType.GUARDIAN_TERMS, ConsentType.GUARDIAN_SCHEDULE_MANAGE))
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
    }
}

data class GuardianProfileUiState(
    val name: String = "",
    val termsChecked: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val completed: Boolean = false,
) {
    val canSubmit: Boolean get() = name.isNotBlank() && termsChecked && !isSubmitting
}
