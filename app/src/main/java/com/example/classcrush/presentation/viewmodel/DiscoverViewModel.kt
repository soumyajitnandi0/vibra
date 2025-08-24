package com.example.classcrush.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.classcrush.data.model.User
import com.example.classcrush.data.repository.AuthRepository
import com.example.classcrush.data.repository.ChatRepository
import com.example.classcrush.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private var currentUser: User? = null
    // Track swipes in current session to prevent duplicates
    private val sessionSwipedUsers = mutableSetOf<String>()
    private var dailySwipeCount = 0
    private var lastSwipeResetDate = 0L
    private var retryAttempts = 0
    private val maxRetryAttempts = 3

    init {
        android.util.Log.d("DiscoverViewModel", "DiscoverViewModel initialized")
        loadCurrentUser()
        resetDailySwipeCountIfNeeded()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                android.util.Log.d("DiscoverViewModel", "Loading current user...")
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val firebaseUser = authRepository.currentUser
                if (firebaseUser == null) {
                    android.util.Log.e("DiscoverViewModel", "No authenticated user found")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "User not authenticated. Please log in again."
                    )
                    return@launch
                }

                android.util.Log.d("DiscoverViewModel", "Firebase user found: ${firebaseUser.uid}")

                userRepository.getUser(firebaseUser.uid)
                    .onSuccess { user ->
                        if (user == null) {
                            android.util.Log.e("DiscoverViewModel", "User profile not found in database")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "User profile not found. Please complete your profile."
                            )
                            return@onSuccess
                        }

                        android.util.Log.d("DiscoverViewModel", "Current user loaded successfully: ${user.name}")
                        android.util.Log.d("DiscoverViewModel", "User details - College: ${user.college}, Gender: ${user.gender}, Interested in: ${user.interestedIn}")

                        // Validate user profile completeness
                        if (!isUserProfileComplete(user)) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Please complete your profile before discovering matches"
                            )
                            return@onSuccess
                        }

                        currentUser = user
                        // Don't call loadPotentialMatches here automatically
                        _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                    }
                    .onFailure { exception ->
                        android.util.Log.e("DiscoverViewModel", "Failed to load current user: ${exception.message}", exception)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to load profile: ${exception.message}"
                        )
                    }
            } catch (e: Exception) {
                android.util.Log.e("DiscoverViewModel", "Error in loadCurrentUser: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "An unexpected error occurred: ${e.message}"
                )
            }
        }
    }

    fun loadPotentialMatches() {
        viewModelScope.launch {
            try {
                android.util.Log.d("DiscoverViewModel", "loadPotentialMatches called")

                val user = currentUser
                if (user == null) {
                    android.util.Log.w("DiscoverViewModel", "Current user is null, reloading user first")
                    loadCurrentUser()
                    return@launch
                }

                // Check daily swipe limit
                if (hasReachedDailyLimit()) {
                    android.util.Log.w("DiscoverViewModel", "Daily swipe limit reached")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "You've reached your daily swipe limit. Come back tomorrow!"
                    )
                    return@launch
                }

                android.util.Log.d("DiscoverViewModel", "Loading potential matches for: ${user.name}")
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                userRepository.getPotentialMatches(user)
                    .onSuccess { users ->
                        android.util.Log.d("DiscoverViewModel", "Successfully loaded ${users.size} potential matches")
                        
                        // Filter out users swiped in current session
                        val filteredUsers = users.filter { potentialMatch ->
                            !sessionSwipedUsers.contains(potentialMatch.id)
                        }
                        
                        android.util.Log.d("DiscoverViewModel", "After session filtering: ${filteredUsers.size} matches (removed ${users.size - filteredUsers.size} session swipes)")

                        if (filteredUsers.isEmpty()) {
                            android.util.Log.w("DiscoverViewModel", "No potential matches found after filtering")
                        } else {
                            android.util.Log.d("DiscoverViewModel", "Final potential matches: ${filteredUsers.map { it.name }}")
                        }

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            potentialMatches = filteredUsers,
                            currentIndex = 0,
                            error = null
                        )
                    }
                    .onFailure { exception ->
                        android.util.Log.e("DiscoverViewModel", "Failed to load matches: ${exception.message}", exception)
                        
                        // Enhanced error handling with user-friendly messages
                        val errorMessage = when {
                            exception.message?.contains("permission", ignoreCase = true) == true -> 
                                "Permission denied. Please log out and log back in."
                            exception.message?.contains("network", ignoreCase = true) == true -> 
                                "Network error. Please check your connection and try again."
                            exception.message?.contains("timeout", ignoreCase = true) == true -> 
                                "Connection timed out. Please try again."
                            exception.message?.contains("authentication", ignoreCase = true) == true -> 
                                "Authentication error. Please log in again."
                            else -> {
                                android.util.Log.d("DiscoverViewModel", "User debug info: ${getCurrentUserInfo()}")
                                "Unable to load profiles. Please try refreshing or check your profile completion."
                            }
                        }
                        
                        // Auto-retry for network errors
                        if (exception.message?.contains("network", ignoreCase = true) == true && 
                            retryAttempts < maxRetryAttempts) {
                            retryAttempts++
                            android.util.Log.d("DiscoverViewModel", "Auto-retrying... Attempt $retryAttempts/$maxRetryAttempts")
                            kotlinx.coroutines.delay(2000L * retryAttempts) // Exponential backoff
                            loadPotentialMatches()
                        } else {
                            retryAttempts = 0 // Reset for next manual retry
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = errorMessage
                            )
                        }
                    }
            } catch (e: Exception) {
                android.util.Log.e("DiscoverViewModel", "Error in loadPotentialMatches: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "An unexpected error occurred: ${e.message}"
                )
            }
        }
    }

    fun likeUser(likedUser: User) {
        viewModelScope.launch {
            try {
                android.util.Log.d("DiscoverViewModel", "Liking user: ${likedUser.name}")

                // Check daily swipe limit
                if (hasReachedDailyLimit()) {
                    android.util.Log.w("DiscoverViewModel", "Daily swipe limit reached")
                    _uiState.value = _uiState.value.copy(
                        error = "You've reached your daily swipe limit. Come back tomorrow!"
                    )
                    return@launch
                }

                currentUser?.let { user ->
                    // Increment daily swipe count
                    dailySwipeCount++
                    
                    // Undo removed: no swipe history tracking

                    // Calculate match percentage based on common interests
                    val matchPercentage = calculateMatchPercentage(user, likedUser)

                    // Add to session tracking immediately to prevent re-appearance
                    sessionSwipedUsers.add(likedUser.id)
                    
                    // Update current user object locally
                    currentUser = user.copy(
                        likedUsers = user.likedUsers + likedUser.id
                    )
                    
                    userRepository.likeUser(user.id, likedUser.id)
                        .onSuccess { isMatch ->
                            android.util.Log.d("DiscoverViewModel", "Like successful, isMatch: $isMatch")

                            if (isMatch) {
                                android.util.Log.d("DiscoverViewModel", "It's a match! Showing dialog...")
                                // Match record is created inside UserRepository.likeUser when mutual
                                _uiState.value = _uiState.value.copy(
                                    newMatch = likedUser,
                                    showMatchDialog = true,
                                    matchPercentage = matchPercentage
                                )
                            }
                            moveToNextCard()
                        }
                        .onFailure { exception ->
                            android.util.Log.e("DiscoverViewModel", "Failed to like user: ${exception.message}", exception)
                            
                            val errorMessage = when {
                                exception.message?.contains("permission", ignoreCase = true) == true -> 
                                    "Permission denied. Please check your login status."
                                exception.message?.contains("network", ignoreCase = true) == true -> 
                                    "Network error. Please try again."
                                else -> "Failed to record like: ${exception.message}"
                            }
                            
                            _uiState.value = _uiState.value.copy(error = errorMessage)
                        }
                } ?: run {
                    android.util.Log.e("DiscoverViewModel", "Current user is null in likeUser")
                    _uiState.value = _uiState.value.copy(
                        error = "Current user not found. Please restart the app."
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("DiscoverViewModel", "Error in likeUser: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "An unexpected error occurred: ${e.message}"
                )
            }
        }
    }

    fun dislikeUser(dislikedUser: User) {
        viewModelScope.launch {
            try {
                android.util.Log.d("DiscoverViewModel", "Disliking user: ${dislikedUser.name}")

                // Check daily swipe limit
                if (hasReachedDailyLimit()) {
                    android.util.Log.w("DiscoverViewModel", "Daily swipe limit reached")
                    _uiState.value = _uiState.value.copy(
                        error = "You've reached your daily swipe limit. Come back tomorrow!"
                    )
                    return@launch
                }

                currentUser?.let { user ->
                    // Increment daily swipe count
                    dailySwipeCount++
                    
                    // Undo removed: no swipe history tracking

                    // Add to session tracking immediately to prevent re-appearance
                    sessionSwipedUsers.add(dislikedUser.id)
                    
                    // Update current user object locally
                    currentUser = user.copy(
                        dislikedUsers = user.dislikedUsers + dislikedUser.id
                    )
                    
                    userRepository.dislikeUser(user.id, dislikedUser.id)
                        .onSuccess {
                            android.util.Log.d("DiscoverViewModel", "Dislike successful")
                            moveToNextCard()
                        }
                        .onFailure { exception ->
                            android.util.Log.e("DiscoverViewModel", "Failed to dislike user: ${exception.message}", exception)
                            
                            val errorMessage = when {
                                exception.message?.contains("permission", ignoreCase = true) == true -> 
                                    "Permission denied. Please check your login status."
                                exception.message?.contains("network", ignoreCase = true) == true -> 
                                    "Network error. Please try again."
                                else -> "Failed to record pass: ${exception.message}"
                            }
                            
                            _uiState.value = _uiState.value.copy(error = errorMessage)
                        }
                } ?: run {
                    android.util.Log.e("DiscoverViewModel", "Current user is null in dislikeUser")
                    _uiState.value = _uiState.value.copy(
                        error = "Current user not found. Please restart the app."
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("DiscoverViewModel", "Error in dislikeUser: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "An unexpected error occurred: ${e.message}"
                )
            }
        }
    }

    // Undo feature removed

    private fun moveToNextCard() {
        val currentState = _uiState.value
        val nextIndex = currentState.currentIndex + 1

        android.util.Log.d("DiscoverViewModel", "Moving to next card, current index: ${currentState.currentIndex}, next: $nextIndex, total matches: ${currentState.potentialMatches.size}")

        if (nextIndex >= currentState.potentialMatches.size) {
            // Instead of reloading all matches, filter out swiped users from current list
            val remainingMatches = currentState.potentialMatches.filter { user ->
                !sessionSwipedUsers.contains(user.id)
            }
            
            android.util.Log.d("DiscoverViewModel", "Reached end of current matches. Remaining after session filter: ${remainingMatches.size}")
            
            if (remainingMatches.isEmpty()) {
                // Only reload if no matches left after filtering
                android.util.Log.d("DiscoverViewModel", "No remaining matches, reloading from database...")
                loadPotentialMatches()
            } else {
                // Use remaining matches and reset index
                _uiState.value = currentState.copy(
                    potentialMatches = remainingMatches,
                    currentIndex = 0
                )
                android.util.Log.d("DiscoverViewModel", "Using ${remainingMatches.size} remaining matches, reset to index 0")
            }
        } else {
            _uiState.value = currentState.copy(currentIndex = nextIndex)
            android.util.Log.d("DiscoverViewModel", "Moved to card at index: $nextIndex")
        }
    }

    fun dismissMatchDialog() {
        android.util.Log.d("DiscoverViewModel", "Dismissing match dialog")
        _uiState.value = _uiState.value.copy(
            showMatchDialog = false,
            newMatch = null,
            matchPercentage = null
        )
    }

    fun clearError() {
        android.util.Log.d("DiscoverViewModel", "Clearing error")
        _uiState.value = _uiState.value.copy(error = null)
    }

    // Enhanced daily swipe management
    private fun resetDailySwipeCountIfNeeded() {
        val currentTime = System.currentTimeMillis()
        val oneDayInMillis = 24 * 60 * 60 * 1000L
        
        if (currentTime - lastSwipeResetDate > oneDayInMillis) {
            dailySwipeCount = 0
            lastSwipeResetDate = currentTime
            android.util.Log.d("DiscoverViewModel", "Daily swipe count reset")
        }
    }

    // Get daily swipe count (for gamification)
    fun getDailySwipeCount(): Int {
        resetDailySwipeCountIfNeeded()
        return dailySwipeCount
    }

    // Check if user has reached daily limit
    fun hasReachedDailyLimit(): Boolean {
        resetDailySwipeCountIfNeeded()
        return dailySwipeCount >= DAILY_SWIPE_LIMIT
    }

    // Calculate match percentage based on common interests
    private fun calculateMatchPercentage(user1: User, user2: User): Int {
        var matchPoints = 0
        val totalPoints = 100

        // Same department (30 points)
        if (user1.department == user2.department) {
            matchPoints += 30
        }

        // Same year (20 points)
        if (user1.year == user2.year) {
            matchPoints += 20
        }

        // Same college (15 points)
        if (user1.college == user2.college) {
            matchPoints += 15
        }

        // Age compatibility (10 points)
        val ageDifference = abs(user1.age - user2.age)
        if (ageDifference <= 2) {
            matchPoints += 10
        } else if (ageDifference <= 5) {
            matchPoints += 5
        }

        // Bio similarity (25 points) - simple keyword matching
        val bio1 = user1.bio.lowercase()
        val bio2 = user2.bio.lowercase()
        val commonWords = bio1.split(" ").intersect(bio2.split(" ").toSet())
        matchPoints += minOf(25, commonWords.size * 5)

        return minOf(100, matchPoints)
    }

    // Add a manual refresh function
    fun refreshMatches() {
        android.util.Log.d("DiscoverViewModel", "Manual refresh requested")
        // Reset retry attempts for manual refresh
        retryAttempts = 0
        // Clear session swipes for fresh start (optional - you may want to keep them)
        // sessionSwipedUsers.clear()
        // Clear current matches and reload
        _uiState.value = _uiState.value.copy(
            potentialMatches = emptyList(),
            currentIndex = 0,
            error = null
        )
        loadPotentialMatches()
    }

    // Validate if user profile is complete enough for matching
    private fun isUserProfileComplete(user: User): Boolean {
        return user.name.isNotBlank() &&
                user.college.isNotBlank() &&
                user.department.isNotBlank() &&
                user.year.isNotBlank() &&
                user.age >= 18 &&
                user.profileImagePublicId.isNotBlank()
    }

    // Add function to check current user status
    fun getCurrentUserInfo(): String {
        return currentUser?.let { user ->
            """
            Current User Debug Info:
            - Name: ${user.name}
            - College: '${user.college}'
            - Gender: ${user.gender}
            - Interested In: ${user.interestedIn}
            - Has Profile Image: ${user.profileImagePublicId.isNotEmpty()}
            - Profile Image ID: '${user.profileImagePublicId}'
            - Liked Users: ${user.likedUsers.size} (${user.likedUsers.joinToString(", ")})
            - Disliked Users: ${user.dislikedUsers.size} (${user.dislikedUsers.joinToString(", ")})
            - Blocked Users: ${user.blockedUsers.size} (${user.blockedUsers.joinToString(", ")})
            - Matches: ${user.matches.size} (${user.matches.joinToString(", ")})
            - Age: ${user.age}
            - Department: '${user.department}'
            - Year: '${user.year}'
            - Daily Swipe Count: $dailySwipeCount
            - Daily Limit Reached: ${hasReachedDailyLimit()}
            - Profile Complete: ${isUserProfileComplete(user)}
            """.trimIndent()
        } ?: "No current user loaded"
    }

    companion object {
        private const val DAILY_SWIPE_LIMIT = 50 // Reduced for better gamification
    }
}

data class DiscoverUiState(
    val isLoading: Boolean = false,
    val potentialMatches: List<User> = emptyList(),
    val currentIndex: Int = 0,
    val newMatch: User? = null,
    val showMatchDialog: Boolean = false,
    val matchPercentage: Int? = null,
    val error: String? = null
)

data class SwipeAction(
    val user: User,
    val action: SwipeType,
    val timestamp: Long
)

enum class SwipeType {
    LIKE, PASS
}