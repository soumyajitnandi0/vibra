package com.example.classcrush.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.classcrush.data.model.Gender
import com.example.classcrush.data.model.User
import com.example.classcrush.data.repository.AuthRepository
import com.example.classcrush.data.repository.UserRepository
import com.example.classcrush.data.service.CloudinaryService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val cloudinaryService: CloudinaryService
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        // Check if Cloudinary is properly configured
        if (!cloudinaryService.isConfigured()) {
            _uiState.value = _uiState.value.copy(
                error = "Cloudinary is not properly configured. Please check your credentials."
            )
        }
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun updateAge(age: Int) {
        _uiState.value = _uiState.value.copy(age = age)
    }

    fun updateGender(gender: Gender) {
        _uiState.value = _uiState.value.copy(gender = gender)
    }

    fun updateInterestedIn(interestedIn: Gender) {
        _uiState.value = _uiState.value.copy(interestedIn = interestedIn)
    }

    fun updateDepartment(department: String) {
        _uiState.value = _uiState.value.copy(department = department)
    }

    fun updateYear(year: String) {
        _uiState.value = _uiState.value.copy(year = year)
    }

    fun updateCollege(college: String) {
        _uiState.value = _uiState.value.copy(college = college)
    }

    fun updateProfileImage(uri: Uri) {
        _uiState.value = _uiState.value.copy(profileImageUri = uri)
    }

    fun updateBio(bio: String) {
        if (bio.length <= 500) {
            _uiState.value = _uiState.value.copy(bio = bio)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val currentUser = authRepository.currentUser

            if (currentUser == null) {
                _uiState.value = currentState.copy(error = "User not authenticated")
                return@launch
            }

            if (!cloudinaryService.isConfigured()) {
                _uiState.value = currentState.copy(
                    error = "Image service not configured. Please contact support."
                )
                return@launch
            }

            _uiState.value = currentState.copy(isLoading = true, error = null)

            try {
                // Upload profile image to Cloudinary
                var profileImageUrl = ""
                var profileImagePublicId = ""

                currentState.profileImageUri?.let { uri ->
                    userRepository.uploadProfileImage(currentUser.uid, uri)
                        .onSuccess { uploadResult ->
                            profileImageUrl = uploadResult.secureUrl
                            profileImagePublicId = uploadResult.publicId
                        }
                        .onFailure { exception ->
                            _uiState.value = currentState.copy(
                                isLoading = false,
                                error = "Failed to upload image: ${exception.message}"
                            )
                            return@launch
                        }
                }

                // Create user profile with Cloudinary data
                val user = User(
                    id = currentUser.uid,
                    name = currentState.name.trim(),
                    email = currentUser.email ?: "",
                    age = currentState.age,
                    gender = currentState.gender,
                    interestedIn = currentState.interestedIn,
                    department = currentState.department.trim(),
                    year = currentState.year.trim(),
                    college = currentState.college.trim(),
                    bio = currentState.bio.trim(),
                    profileImageUrl = profileImageUrl,
                    profileImagePublicId = profileImagePublicId,
                    createdAt = System.currentTimeMillis(),
                    isOnline = true,
                    lastSeen = System.currentTimeMillis()
                )

                userRepository.createUser(user)
                    .onSuccess {
                        _uiState.value = currentState.copy(
                            isLoading = false,
                            isCompleted = true
                        )
                    }
                    .onFailure { exception ->
                        // If user creation fails, try to delete the uploaded image
                        if (profileImagePublicId.isNotEmpty()) {
                            cloudinaryService.deleteImage(profileImagePublicId)
                        }

                        _uiState.value = currentState.copy(
                            isLoading = false,
                            error = "Failed to create profile: ${exception.message}"
                        )
                    }

            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isLoading = false,
                    error = "An unexpected error occurred: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class OnboardingUiState(
    val name: String = "",
    val age: Int = 0,
    val gender: Gender = Gender.MALE,
    val interestedIn: Gender = Gender.FEMALE,
    val department: String = "",
    val year: String = "",
    val college: String = "",
    val bio: String = "",
    val profileImageUri: Uri? = null,
    val isLoading: Boolean = false,
    val isCompleted: Boolean = false,
    val error: String? = null
)
