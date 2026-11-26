package com.daljoalab.memorycapsule.feature.guardianhome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daljoalab.memorycapsule.core.data.repository.AuthRepository
import com.daljoalab.memorycapsule.core.data.repository.PairingRepository
import com.daljoalab.memorycapsule.core.model.GuardianLink
import com.daljoalab.memorycapsule.core.model.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GuardianHomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val pairingRepository: PairingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<GuardianHomeUiState>(GuardianHomeUiState.Loading)
    val uiState: StateFlow<GuardianHomeUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = try {
                GuardianHomeUiState.Loaded(pairingRepository.getLinks())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                GuardianHomeUiState.Failed(e.toUserMessage())
            }
        }
    }

    fun signOut() = authRepository.signOut()
}

sealed interface GuardianHomeUiState {
    data object Loading : GuardianHomeUiState
    data class Loaded(val links: List<GuardianLink>) : GuardianHomeUiState
    data class Failed(val message: String) : GuardianHomeUiState
}
