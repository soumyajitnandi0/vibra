# Firebase-Based Messaging Implementation for ClassCrush

This document outlines the complete Firebase-based messaging system that replaces the previous Socket.IO implementation.

## Overview

The new messaging system provides real-time messaging capabilities between matched users using Firebase Realtime Database with:
- Instant message delivery through Firebase listeners
- Image sharing via Cloudinary
- Chat summaries and previews
- No external server dependencies

## Architecture

### Client-Side (Android)
- **UserSession**: Manages user session data
- **ChatUtils**: Generates stable chat IDs
- **ChatRepository**: Firebase-based message operations
- **ChatViewModel**: UI state management
- **ChatActivity**: Traditional Android chat UI
- **ChatScreen**: Compose-based chat UI

### Data Models
- **GossipModel**: Individual chat messages
- **ChatSummary**: Chat preview information
- **UserSession**: User authentication state

## Implementation Details

### 1. UserSession Management

**Location**: `app/src/main/java/com/example/classcrush/data/model/UserSession.kt`

**Usage**: Call this after user login to store user information
```kotlin
// After successful login
UserSession.save(context, userId, userName)

// Retrieve user info
val currentUserId = UserSession.id(context)
val currentUserName = UserSession.name(context)
```

### 2. Chat ID Generation

**Location**: `app/src/main/java/com/example/classcrush/data/model/ChatUtils.kt`

**Usage**: Generate stable chat IDs for user pairs
```kotlin
val chatId = ChatUtils.chatIdOf(user1Id, user2Id)
// Result: "uid1_uid2" (order-independent)
```

### 3. Message Models

**GossipModel**: Individual chat messages
```kotlin
data class GossipModel(
    val senderId: String = "",
    val senderName: String = "",
    val message: String? = null,
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
```

**ChatSummary**: Chat preview information
```kotlin
data class ChatSummary(
    val chatId: String = "",
    val otherUserId: String = "",
    val otherUserName: String = "",
    val lastMessage: String = "",
    val lastTimestamp: Long = System.currentTimeMillis()
)
```

### 4. Firebase Database Structure

```
chats/
  {chatId}/
    messages/
      -Nk...: { senderId, senderName, message?, imageUrl?, timestamp }

chat_summaries/
  {userId}/
    {chatId}: { chatId, otherUserId, otherUserName, lastMessage, lastTimestamp }
```

### 5. Cloudinary Configuration

**Setup Required**:
1. Create a Cloudinary account
2. Create an unsigned upload preset
3. Update the constants in ChatActivity:

```kotlin
private val CLOUD_NAME = "YOUR_CLOUD_NAME"
private val UPLOAD_PRESET = "YOUR_UNSIGNED_PRESET"
```

## Usage Examples

### Opening a Chat from Matches

```kotlin
val intent = Intent(context, ChatActivity::class.java)
intent.putExtra("otherUserId", matchedUserId)
intent.putExtra("otherUserName", matchedUserName)
startActivity(intent)
```

### Using ChatScreen (Compose)

```kotlin
ChatScreen(
    matchId = chatId,
    otherUser = otherUser,
    onNavigateBack = { /* navigation logic */ },
    onNavigateToSafety = { user -> /* safety logic */ }
)
```

### Using ChatActivity (Traditional Views)

```kotlin
val intent = Intent(this, ChatActivity::class.java)
intent.putExtra("otherUserId", otherUserId)
intent.putExtra("otherUserName", otherUserName)
startActivity(intent)
```

## Firebase Rules (Optional)

For production use, consider adding these Firebase Realtime Database rules:

```json
{
  "rules": {
    "chats": {
      "$chatId": {
        "messages": {
          ".read": "auth != null",
          ".write": "auth != null"
        }
      }
    },
    "chat_summaries": {
      "$userId": {
        ".read": "auth != null && auth.uid == $userId",
        ".write": "auth != null && auth.uid == $userId"
      }
    }
  }
}
```

## Migration from Socket.IO

### What Was Removed
- `SocketService.kt` - Socket.IO client service
- `SocketModule.kt` - Dependency injection for Socket.IO
- `socket-server/` - Node.js Socket.IO server
- All Socket.IO dependencies in build.gradle.kts

### What Was Added
- `UserSession.kt` - User session management
- `ChatUtils.kt` - Chat ID generation utilities
- `GossipModel.kt` - New message model
- `ChatSummary.kt` - Chat preview model
- `ChatActivity.kt` - Traditional Android chat UI
- `ChatsListActivity.kt` - Chat list management
- Firebase-based `ChatRepository.kt`
- Updated `ChatViewModel.kt` and `ChatScreen.kt`

### Dependencies Updated
- Removed: `io.socket:socket.io-client:2.1.0`
- Added: `org.json:json:20231013`
- Kept: `com.squareup.okhttp3:okhttp:4.12.0` (for Cloudinary)

## Benefits of the New System

1. **No External Server**: All communication goes through Firebase
2. **Simplified Architecture**: Fewer moving parts and dependencies
3. **Better Reliability**: Firebase's infrastructure vs custom server
4. **Easier Deployment**: No need to deploy and maintain Node.js server
5. **Cost Effective**: Firebase free tier is generous for most apps
6. **Real-time Updates**: Firebase listeners provide instant updates

## Troubleshooting

### Common Issues

1. **Messages not appearing**: Check Firebase rules and internet connection
2. **Image upload fails**: Verify Cloudinary credentials and preset
3. **Chat summaries not updating**: Check Firebase database structure
4. **User session missing**: Ensure UserSession.save() is called after login

### Debug Steps

1. Check Firebase console for database updates
2. Verify user authentication status
3. Check Cloudinary dashboard for upload issues
4. Review Android logs for error messages

## Future Enhancements

### Planned Features
1. **Push Notifications**: Firebase Cloud Messaging integration
2. **Message Status**: Read receipts and delivery confirmation
3. **Typing Indicators**: Real-time typing status
4. **Message Reactions**: Emoji reactions to messages
5. **Voice Messages**: Audio recording and playback
6. **Video Messages**: Video recording and sharing

### Performance Optimizations
1. **Message Pagination**: Load messages in chunks
2. **Image Caching**: Implement Coil caching strategies
3. **Offline Support**: Firebase offline persistence
4. **Message Search**: Full-text search capabilities

## Support

For issues or questions about this implementation:
1. Check Firebase documentation
2. Review Cloudinary setup guide
3. Verify Android manifest permissions
4. Check build.gradle.kts dependencies

