package com.daljoalab.memorycapsule.feature.launch

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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LaunchViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LaunchUiState>(LaunchUiState.Loading)
    val uiState: StateFlow<LaunchUiState> = _uiState.asStateFlow()

    init {
        decide()
    }

    fun decide() {
        _uiState.value = LaunchUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                LaunchUiState.Decided(findDestination())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LaunchUiState.Failed(e.toUserMessage())
            }
        }
    }

    private suspend fun findDestination(): LaunchDestination {
        if (!authRepository.isSignedIn) return LaunchDestination.ROLE_SELECT
        val me = userRepository.getMe()
        return when {
            me == null && authRepository.isAnonymous -> LaunchDestination.SENIOR_ONBOARDING
            me == null -> LaunchDestination.GUARDIAN_PROFILE
            me.role == UserRole.SENIOR -> LaunchDestination.SENIOR_HOME
            else -> LaunchDestination.GUARDIAN_HOME
        }
    }
}

enum class LaunchDestination {
    ROLE_SELECT,
    SENIOR_ONBOARDING,
    SENIOR_HOME,
    GUARDIAN_PROFILE,
    GUARDIAN_HOME,
}

sealed interface LaunchUiState {
    data object Loading : LaunchUiState
    data class Decided(val destination: LaunchDestination) : LaunchUiState
    data class Failed(val message: String) : LaunchUiState
}
