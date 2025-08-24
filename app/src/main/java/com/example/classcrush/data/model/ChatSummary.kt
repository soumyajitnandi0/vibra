package com.example.classcrush.data.model

data class ChatSummary(
    val chatId: String = "",
    val otherUserId: String = "",
    val otherUserName: String = "",
    val lastMessage: String = "",
    val lastTimestamp: Long = System.currentTimeMillis()
)

