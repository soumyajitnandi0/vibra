package com.example.classcrush.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val age: Int = 18,
    val gender: Gender = Gender.MALE,
    val interestedIn: Gender = Gender.FEMALE,
    val department: String = "",
    val year: String = "",
    val bio: String = "",
    val profileImageUrl: String = "",
    val profileImagePublicId: String = "", // Cloudinary public ID
    val additionalImages: List<CloudinaryImage> = emptyList(), // Updated for Cloudinary
    val college: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val likedUsers: List<String> = emptyList(),
    val dislikedUsers: List<String> = emptyList(),
    val matches: List<String> = emptyList(),
    val blockedUsers: List<String> = emptyList(),
    val reportedUsers: List<String> = emptyList(),
    val fcmToken: String = ""
) : Parcelable

enum class Gender {
    MALE, FEMALE, OTHER, ALL
}

@Parcelize
data class CloudinaryImage(
    val publicId: String = "",
    val secureUrl: String = "",
    val thumbnailUrl: String = "",
    val mediumUrl: String = "",
    val uploadedAt: Long = System.currentTimeMillis()
) : Parcelable

@Parcelize
data class Match(
    val id: String = "",
    val user1Id: String = "",
    val user2Id: String = "",
    val timestamp: Long = 0L,
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val status: String? = "active" // <-- Add this line
): Parcelable
@Parcelize
data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val messageType: MessageType = MessageType.TEXT
) : Parcelable

enum class MessageType {
    TEXT, IMAGE, EMOJI
}
