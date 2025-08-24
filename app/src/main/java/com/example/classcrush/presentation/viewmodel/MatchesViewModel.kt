package com.example.classcrush.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.classcrush.data.model.Match
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

@HiltViewModel
class MatchesViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MatchesUiState())
    val uiState: StateFlow<MatchesUiState> = _uiState.asStateFlow()

    fun loadMatches() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val currentUser = authRepository.currentUser
            if (currentUser == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "User not authenticated")
                return@launch
            }

            chatRepository.getMatches(currentUser.uid)
                .onSuccess { matches ->
                    val matchDetails = matches.filter { it.status == null || it.status == "active" }.mapNotNull { match ->
                        val otherUserId = if (match.user1Id == currentUser.uid) match.user2Id else match.user1Id
                        if (otherUserId.isNullOrEmpty()) return@mapNotNull null
                        val user = userRepository.getUser(otherUserId).getOrNull()
                        if (user != null) MatchItem(match, user) else null
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, matches = matchDetails)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load matches: ${exception.message}"
                    )
                }
        }
    }
}

data class MatchesUiState(
    val isLoading: Boolean = false,
    val matches: List<MatchItem> = emptyList(),
    val error: String? = null
)

data class MatchItem(
    val match: Match,
    val otherUser: User
)