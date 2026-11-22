package com.daljoalab.memorycapsule.feature.guardianlink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daljoalab.memorycapsule.core.data.repository.PairingRepository
import com.daljoalab.memorycapsule.core.data.repository.UserRepository
import com.daljoalab.memorycapsule.core.model.ConsentType
import com.daljoalab.memorycapsule.core.model.PairingTarget
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
class GuardianLinkViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val pairingRepository: PairingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GuardianLinkUiState())
    val uiState: StateFlow<GuardianLinkUiState> = _uiState.asStateFlow()

    fun startCodeEntry() = _uiState.update { it.copy(step = GuardianLinkStep.ENTER_CODE, errorMessage = null) }

    fun onCodeChange(code: String) {
        val previous = _uiState.value.code
        val digits = code.filter(Char::isDigit).take(CODE_LENGTH)
        _uiState.update { it.copy(code = digits, errorMessage = null) }
        if (digits.length == CODE_LENGTH && previous.length < CODE_LENGTH) resolve()
    }

    fun resolve() = runStep(requireCode = true) { state ->
        val target = pairingRepository.resolve(state.code)
        _uiState.update { it.copy(step = GuardianLinkStep.CONFIRM, target = target) }
    }

    fun link() = runStep(requireCode = true) { state ->
        val target = state.target ?: return@runStep
        userRepository.grantConsents(listOf(ConsentType.GUARDIAN_DATA_VIEW), linkedSeniorId = target.seniorId)
        pairingRepository.link(state.code, RELATION)
        _uiState.update { it.copy(linked = true) }
    }

    fun backToCodeEntry() = _uiState.update { it.copy(step = GuardianLinkStep.ENTER_CODE, target = null) }

    fun back(): Boolean {
        val previous = when (_uiState.value.step) {
            GuardianLinkStep.CHOOSE_METHOD -> return false
            GuardianLinkStep.ENTER_CODE -> GuardianLinkStep.CHOOSE_METHOD
            GuardianLinkStep.CONFIRM -> GuardianLinkStep.ENTER_CODE
        }
        _uiState.update { it.copy(step = previous, errorMessage = null) }
        return true
    }

    private fun runStep(requireCode: Boolean, block: suspend (GuardianLinkUiState) -> Unit) {
        val state = _uiState.value
        if (state.isSubmitting || (requireCode && state.code.length != CODE_LENGTH)) return
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                block(state)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.toUserMessage()) }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    private companion object {
        const val CODE_LENGTH = 6

        const val RELATION = "가족"
    }
}

enum class GuardianLinkStep { CHOOSE_METHOD, ENTER_CODE, CONFIRM }

data class GuardianLinkUiState(
    val step: GuardianLinkStep = GuardianLinkStep.CHOOSE_METHOD,
    val code: String = "",
    val target: PairingTarget? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val linked: Boolean = false,
)
