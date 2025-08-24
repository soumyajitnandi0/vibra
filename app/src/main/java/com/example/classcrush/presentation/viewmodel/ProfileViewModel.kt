package com.example.classcrush.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.classcrush.data.model.User
import com.example.classcrush.data.repository.AuthRepository
import com.example.classcrush.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadUserProfile() {
        viewModelScope.launch {
            val currentUser = authRepository.currentUser
            if (currentUser == null) {
                _uiState.value = _uiState.value.copy(error = "User not authenticated")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            userRepository.getUser(currentUser.uid)
                .onSuccess { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = user
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message
                    )
                }
        }
    }

    fun updateProfileImage(imageUri: Uri) {
        viewModelScope.launch {
            val currentUser = authRepository.currentUser
            if (currentUser == null) {
                _uiState.value = _uiState.value.copy(error = "User not authenticated")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isUpdatingImage = true, error = null)

            userRepository.updateProfileImage(currentUser.uid, imageUri)
                .onSuccess { newImageUrl ->
                    // Reload user profile to get updated data
                    loadUserProfile()
                    _uiState.value = _uiState.value.copy(isUpdatingImage = false)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isUpdatingImage = false,
                        error = "Failed to update profile image: ${exception.message}"
                    )
                }
        }
    }

    fun updateBio(newBio: String) {
        viewModelScope.launch {
            val currentUser = authRepository.currentUser
            if (currentUser == null) {
                _uiState.value = _uiState.value.copy(error = "User not authenticated")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isUpdating = true, error = null)

            userRepository.updateUserField(currentUser.uid, "bio", newBio.trim())
                .onSuccess {
                    // Update local state
                    val updatedUser = _uiState.value.user?.copy(bio = newBio.trim())
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        user = updatedUser
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        error = "Failed to update bio: ${exception.message}"
                    )
                }
        }
    }

    fun updateOnlineStatus(isOnline: Boolean) {
        viewModelScope.launch {
            val currentUser = authRepository.currentUser
            if (currentUser != null) {
                userRepository.updateOnlineStatus(currentUser.uid, isOnline)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            // Update offline status before logging out
            val currentUser = authRepository.currentUser
            if (currentUser != null) {
                userRepository.updateOnlineStatus(currentUser.uid, false)
            }

            authRepository.signOut()
            _uiState.value = ProfileUiState(isLoggedOut = true)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun saveProfile(updated: User) {
        viewModelScope.launch {
            val currentUser = authRepository.currentUser
            if (currentUser == null || currentUser.uid != updated.id) {
                _uiState.value = _uiState.value.copy(error = "User not authenticated")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isUpdating = true, error = null)

            userRepository.updateUser(updated)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isUpdating = false, user = updated)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        error = "Failed to save profile: ${exception.message}"
                    )
                }
        }
    }
}

data class ProfileUiState(
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val isUpdatingImage: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val isLoggedOut: Boolean = false
)
