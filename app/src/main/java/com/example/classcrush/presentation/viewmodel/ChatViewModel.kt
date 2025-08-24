package com.example.classcrush.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.classcrush.data.model.ChatSummary
import com.example.classcrush.data.model.GossipModel
import com.example.classcrush.data.repository.AuthRepository
import com.example.classcrush.data.repository.ChatRepository
import com.example.classcrush.data.service.CloudinaryService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val cloudinaryService: CloudinaryService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun loadMessages(chatId: String) {
        if (chatId.isEmpty()) {
            android.util.Log.w("ChatViewModel", "loadMessages called with empty chatId")
            return
        }
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                // Ensure current user is a participant for rules
                authRepository.currentUser?.uid?.let { uid ->
                    chatRepository.joinChat(chatId, uid)
                }

                chatRepository.getMessages(chatId).collect { messages ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        messages = messages
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error loading messages: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load messages: ${e.message}"
                )
            }
        }
    }

    fun sendMessage(chatId: String, messageText: String) {
        if (chatId.isEmpty()) {
            android.util.Log.w("ChatViewModel", "sendMessage called with empty chatId")
            return
        }
        
        if (messageText.trim().isEmpty()) {
            android.util.Log.w("ChatViewModel", "sendMessage called with empty message text")
            return
        }
        
        viewModelScope.launch {
            try {
                val currentUser = authRepository.currentUser
                if (currentUser == null) {
                    _uiState.value = _uiState.value.copy(error = "User not authenticated")
                    return@launch
                }

                _uiState.value = _uiState.value.copy(isSending = true, error = null)

                val message = GossipModel(
                    senderId = currentUser.uid,
                    senderName = currentUser.displayName ?: "Unknown User",
                    message = messageText.trim(),
                    timestamp = System.currentTimeMillis()
                )

                chatRepository.sendMessage(chatId, message)
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(isSending = false)
                    }
                    .onFailure { exception ->
                        android.util.Log.e("ChatViewModel", "Failed to send message: ${exception.message}", exception)
                        _uiState.value = _uiState.value.copy(
                            isSending = false,
                            error = "Failed to send message: ${exception.message}"
                        )
                    }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error in sendMessage: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    error = "An unexpected error occurred: ${e.message}"
                )
            }
        }
    }

    fun sendImageMessage(chatId: String, imageUri: Uri) {
        if (chatId.isEmpty()) {
            android.util.Log.w("ChatViewModel", "sendImageMessage called with empty chatId")
            return
        }
        
        viewModelScope.launch {
            try {
                val currentUser = authRepository.currentUser
                if (currentUser == null) {
                    _uiState.value = _uiState.value.copy(error = "User not authenticated")
                    return@launch
                }

                _uiState.value = _uiState.value.copy(isSending = true, error = null)

                // Upload image to Cloudinary
                cloudinaryService.uploadChatImage(currentUser.uid, imageUri)
                    .onSuccess { uploadResult ->
                        val message = GossipModel(
                            senderId = currentUser.uid,
                            senderName = currentUser.displayName ?: "Unknown User",
                            imageUrl = uploadResult.secureUrl,
                            timestamp = System.currentTimeMillis()
                        )

                        chatRepository.sendMessage(chatId, message)
                            .onSuccess {
                                _uiState.value = _uiState.value.copy(isSending = false)
                            }
                            .onFailure { exception ->
                                android.util.Log.e("ChatViewModel", "Failed to send image message: ${exception.message}", exception)
                                _uiState.value = _uiState.value.copy(
                                    isSending = false,
                                    error = "Failed to send image: ${exception.message}"
                                )
                            }
                    }
                    .onFailure { exception ->
                        android.util.Log.e("ChatViewModel", "Failed to upload image: ${exception.message}", exception)
                        _uiState.value = _uiState.value.copy(
                            isSending = false,
                            error = "Failed to upload image: ${exception.message}"
                        )
                    }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error in sendImageMessage: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    error = "An unexpected error occurred: ${e.message}"
                )
            }
        }
    }

    fun markMessagesAsRead(chatId: String) {
        if (chatId.isEmpty()) {
            android.util.Log.w("ChatViewModel", "markMessagesAsRead called with empty chatId")
            return
        }
        
        viewModelScope.launch {
            try {
                val currentUser = authRepository.currentUser
                if (currentUser != null) {
                    chatRepository.markMessagesAsRead(chatId, currentUser.uid)
                } else {
                    android.util.Log.w("ChatViewModel", "No authenticated user found for markMessagesAsRead")
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error marking messages as read: ${e.message}", e)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // Public method to get current user
    fun getCurrentUser() = authRepository.currentUser
}

data class ChatUiState(
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val messages: List<GossipModel> = emptyList(),
    val error: String? = null
)
