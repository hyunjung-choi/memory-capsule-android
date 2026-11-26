package com.daljoalab.memorycapsule.feature.guardianauth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daljoalab.memorycapsule.core.data.repository.AuthRepository
import com.daljoalab.memorycapsule.core.data.repository.UserRepository
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
class GuardianAuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GuardianAuthUiState())
    val uiState: StateFlow<GuardianAuthUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) = _uiState.update { it.copy(email = email.trim(), errorMessage = null) }

    fun onPasswordChange(password: String) = _uiState.update { it.copy(password = password, errorMessage = null) }

    fun onPasswordConfirmChange(password: String) =
        _uiState.update { it.copy(passwordConfirm = password, errorMessage = null) }

    fun signIn() {
        if (_uiState.value.canSignIn) submit { authRepository.signInWithEmail(it.email, it.password) }
    }

    fun signUp() {
        if (_uiState.value.canSignUp) submit { authRepository.signUpWithEmail(it.email, it.password) }
    }

    private fun submit(authenticate: suspend (GuardianAuthUiState) -> Unit) {
        val state = _uiState.value
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                authenticate(state)
                val me = userRepository.getMe()
                val result = when {
                    me == null -> GuardianAuthResult.NEEDS_PROFILE
                    me.role == UserRole.GUARDIAN -> GuardianAuthResult.SIGNED_IN
                    else -> {
                        authRepository.signOut()
                        _uiState.update { it.copy(isSubmitting = false, errorMessage = "어르신으로 가입된 계정이에요.") }
                        return@launch
                    }
                }
                _uiState.update { it.copy(isSubmitting = false, result = result) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmitting = false, errorMessage = e.toUserMessage()) }
            }
        }
    }
}

enum class GuardianAuthResult {
    NEEDS_PROFILE,
    SIGNED_IN,
}

data class GuardianAuthUiState(
    val email: String = "",
    val password: String = "",
    val passwordConfirm: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val result: GuardianAuthResult? = null,
) {
    private val isEmailValid: Boolean get() = EMAIL_PATTERN.matches(email)

    val emailError: String? get() = "이메일 형식을 확인해 주세요.".takeIf { email.isNotEmpty() && !isEmailValid }
    val passwordError: String?
        get() = "${MIN_PASSWORD_LENGTH}자 이상 입력해 주세요.".takeIf { password.isNotEmpty() && password.length < MIN_PASSWORD_LENGTH }
    val passwordConfirmError: String?
        get() = "비밀번호가 서로 달라요.".takeIf { passwordConfirm.isNotEmpty() && passwordConfirm != password }

    val canSignIn: Boolean get() = isEmailValid && password.isNotEmpty() && !isSubmitting
    val canSignUp: Boolean
        get() = isEmailValid && password.length >= MIN_PASSWORD_LENGTH && passwordConfirm == password && !isSubmitting

    private companion object {
        const val MIN_PASSWORD_LENGTH = 6
        val EMAIL_PATTERN = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")
    }
}
