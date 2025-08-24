package com.example.classcrush.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.classcrush.data.model.CloudinaryImage
import com.example.classcrush.data.repository.AuthRepository
import com.example.classcrush.data.repository.UserRepository
import com.example.classcrush.data.service.CloudinaryUploadResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImageUploadViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageUploadUiState())
    val uiState: StateFlow<ImageUploadUiState> = _uiState.asStateFlow()

    fun uploadProfileImage(imageUri: Uri) {
        viewModelScope.launch {
            val currentUser = authRepository.currentUser
            if (currentUser == null) {
                _uiState.value = _uiState.value.copy(error = "User not authenticated")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isUploading = true, error = null)

            userRepository.uploadProfileImage(currentUser.uid, imageUri)
                .onSuccess { uploadResult ->
                    _uiState.value = _uiState.value.copy(
                        isUploading = false,
                        uploadResult = uploadResult,
                        isSuccess = true
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isUploading = false,
                        error = exception.message
                    )
                }
        }
    }

    fun uploadAdditionalImage(imageUri: Uri) {
        viewModelScope.launch {
            val currentUser = authRepository.currentUser
            if (currentUser == null) {
                _uiState.value = _uiState.value.copy(error = "User not authenticated")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isUploading = true, error = null)

            userRepository.uploadAdditionalImage(currentUser.uid, imageUri)
                .onSuccess { uploadResult ->
                    val cloudinaryImage = CloudinaryImage(
                        publicId = uploadResult.publicId,
                        secureUrl = uploadResult.secureUrl,
                        thumbnailUrl = uploadResult.thumbnailUrl,
                        mediumUrl = uploadResult.mediumUrl,
                        uploadedAt = System.currentTimeMillis()
                    )

                    _uiState.value = _uiState.value.copy(
                        isUploading = false,
                        uploadResult = uploadResult,
                        cloudinaryImage = cloudinaryImage,
                        isSuccess = true
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isUploading = false,
                        error = exception.message
                    )
                }
        }
    }

    fun deleteImage(publicId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true, error = null)

            userRepository.deleteImage(publicId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        isDeleted = true
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        error = exception.message
                    )
                }
        }
    }

    fun clearState() {
        _uiState.value = ImageUploadUiState()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class ImageUploadUiState(
    val isUploading: Boolean = false,
    val isDeleting: Boolean = false,
    val isSuccess: Boolean = false,
    val isDeleted: Boolean = false,
    val uploadResult: CloudinaryUploadResult? = null,
    val cloudinaryImage: CloudinaryImage? = null,
    val error: String? = null
)
