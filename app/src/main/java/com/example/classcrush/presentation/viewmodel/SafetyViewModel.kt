package com.example.classcrush.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.classcrush.data.repository.AuthRepository
import com.example.classcrush.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SafetyViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SafetyUiState())
    val uiState: StateFlow<SafetyUiState> = _uiState.asStateFlow()

    fun reportUser(reportedUserId: String, reason: String) {
        viewModelScope.launch {
            val currentUser = authRepository.currentUser
            if (currentUser == null) {
                _uiState.value = _uiState.value.copy(error = "User not authenticated")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            userRepository.reportUser(currentUser.uid, reportedUserId, reason)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isReported = true
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to report user: ${exception.message}"
                    )
                }
        }
    }

    fun blockUser(blockedUserId: String) {
        viewModelScope.launch {
            val currentUser = authRepository.currentUser
            if (currentUser == null) {
                _uiState.value = _uiState.value.copy(error = "User not authenticated")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            userRepository.blockUser(currentUser.uid, blockedUserId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isBlocked = true
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to block user: ${exception.message}"
                    )
                }
        }
    }

    fun unmatchUser(unmatchedUserId: String) {
        viewModelScope.launch {
            val currentUser = authRepository.currentUser
            if (currentUser == null) {
                _uiState.value = _uiState.value.copy(error = "User not authenticated")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            userRepository.unmatchUser(currentUser.uid, unmatchedUserId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isUnmatched = true
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to unmatch user: ${exception.message}"
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class SafetyUiState(
    val isLoading: Boolean = false,
    val isReported: Boolean = false,
    val isBlocked: Boolean = false,
    val isUnmatched: Boolean = false,
    val error: String? = null
)
