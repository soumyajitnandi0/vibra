package com.example.classcrush.data.model

data class GossipModel(
    val senderId: String = "",
    val senderName: String = "",
    val message: String? = null,
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

