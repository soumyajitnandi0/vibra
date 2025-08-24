package com.example.classcrush.data.repository

import com.example.classcrush.data.model.ChatSummary
import com.example.classcrush.data.model.GossipModel
import com.example.classcrush.data.model.Match
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val database: FirebaseDatabase
) {

    private val chatsRef = database.getReference("chats")
    private val matchesRef = database.getReference("matches")
    private val usersRef = database.getReference("users")
    private val summariesRef = database.getReference("chat_summaries")

    suspend fun joinChat(chatId: String, userId: String): Result<Unit> {
        return try {
            chatsRef.child(chatId).child("participants").child(userId).setValue(true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(chatId: String, message: GossipModel): Result<Unit> {
        return try {
            // Ensure the sender is marked as a participant (satisfies rules)
            if (message.senderId.isNotEmpty()) {
                chatsRef.child(chatId).child("participants").child(message.senderId).setValue(true).await()
            }

            // Save message to Firebase
            val messageRef = chatsRef.child(chatId).child("messages").push()
            val messageWithId = message.copy(timestamp = System.currentTimeMillis())
            messageRef.setValue(messageWithId).await()

            // Update chat summaries
            updateChatSummaries(chatId, messageWithId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun updateChatSummaries(chatId: String, lastMessage: GossipModel) {
        try {
            // Extract user IDs from chatId (format: "uid1_uid2")
            val userIds = chatId.split("_")
            if (userIds.size != 2) return

            val uid1 = userIds[0]
            val uid2 = userIds[1]
            val senderId = lastMessage.senderId

            // Determine which user is the "other" user for each participant
            val preview = lastMessage.message ?: "[image]"
            val timestamp = lastMessage.timestamp

            val updates = hashMapOf<String, Any>()

            // For user 1
            if (senderId == uid1) {
                // User 1 sent the message, so for user 1's summary, the other user is user 2
                val summary1 = ChatSummary(
                    chatId = chatId,
                    otherUserId = uid2,
                    otherUserName = lastMessage.senderName, // This should be the other user's name
                    lastMessage = preview,
                    lastTimestamp = timestamp
                )
                updates["/$uid1/$chatId"] = summary1

                // For user 2's summary, the other user is user 1
                val summary2 = ChatSummary(
                    chatId = chatId,
                    otherUserId = uid1,
                    otherUserName = lastMessage.senderName, // This should be the other user's name
                    lastMessage = preview,
                    lastTimestamp = timestamp
                )
                updates["/$uid2/$chatId"] = summary2
            } else {
                // User 2 sent the message
                val summary1 = ChatSummary(
                    chatId = chatId,
                    otherUserId = uid2,
                    otherUserName = lastMessage.senderName,
                    lastMessage = preview,
                    lastTimestamp = timestamp
                )
                updates["/$uid1/$chatId"] = summary1

                val summary2 = ChatSummary(
                    chatId = chatId,
                    otherUserId = uid1,
                    otherUserName = lastMessage.senderName,
                    lastMessage = preview,
                    lastTimestamp = timestamp
                )
                updates["/$uid2/$chatId"] = summary2
            }

            summariesRef.updateChildren(updates).await()
        } catch (e: Exception) {
            // Handle error silently for now
        }
    }

    fun getMessages(chatId: String): Flow<List<GossipModel>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<GossipModel>()
                for (messageSnapshot in snapshot.children) {
                    val message = messageSnapshot.getValue(GossipModel::class.java)
                    if (message != null) {
                        messages.add(message)
                    }
                }
                // Sort by timestamp
                messages.sortBy { it.timestamp }
                trySend(messages)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        }

        val messagesRef = chatsRef.child(chatId).child("messages")
        messagesRef.addValueEventListener(listener)

        awaitClose {
            messagesRef.removeEventListener(listener)
        }
    }

    fun getChatSummaries(userId: String): Flow<List<ChatSummary>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val summaries = mutableListOf<ChatSummary>()
                for (summarySnapshot in snapshot.children) {
                    val summary = summarySnapshot.getValue(ChatSummary::class.java)
                    if (summary != null) {
                        summaries.add(summary)
                    }
                }
                // Sort by last timestamp (most recent first)
                summaries.sortByDescending { it.lastTimestamp }
                trySend(summaries)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        }

        val userSummariesRef = summariesRef.child(userId)
        userSummariesRef.addValueEventListener(listener)

        awaitClose {
            userSummariesRef.removeEventListener(listener)
        }
    }

    suspend fun getMatches(userId: String): Result<List<Match>> {
        return try {
            // Get matches where user is either user1 or user2
            val snapshot1 = matchesRef.orderByChild("user1Id").equalTo(userId).get().await()
            val snapshot2 = matchesRef.orderByChild("user2Id").equalTo(userId).get().await()
            
            val matches = mutableListOf<Match>()
            
            // Process user1 matches
            for (matchSnapshot in snapshot1.children) {
                val match = matchSnapshot.getValue(Match::class.java)
                if (match != null && (match.status == null || match.status == "active")) {
                    matches.add(match)
                }
            }
            
            // Process user2 matches
            for (matchSnapshot in snapshot2.children) {
                val match = matchSnapshot.getValue(Match::class.java)
                if (match != null && (match.status == null || match.status == "active")) {
                    matches.add(match)
                }
            }
            
            // Deduplicate by pair of user IDs (order-insensitive) and keep most recent
            val deduped = matches
                .groupBy { m ->
                    val a = m.user1Id
                    val b = m.user2Id
                    if (a <= b) "$a-$b" else "$b-$a"
                }
                .map { (_, list) -> list.maxByOrNull { it.timestamp }!! }
                .sortedByDescending { it.timestamp }

            Result.success(deduped)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markMessagesAsRead(chatId: String, userId: String): Result<Unit> {
        return try {
            // In a real implementation, you might want to mark messages as read
            // For now, we'll just return success
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAllMessagesAsRead(userId: String): Result<Unit> {
        return try {
            val matches = getMatches(userId).getOrNull() ?: emptyList()
            
            matches.forEach { match ->
                // You could implement marking all messages as read here
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createMatch(user1Id: String, user2Id: String): Result<String> {
        return try {
            // Check if match already exists
            val existingMatch = findExistingMatch(user1Id, user2Id)
            if (existingMatch != null) {
                return Result.success(existingMatch)
            }

            val matchRef = matchesRef.push()
            val matchId = matchRef.key ?: ""
            val match = Match(
                id = matchId,
                user1Id = user1Id,
                user2Id = user2Id,
                timestamp = System.currentTimeMillis(),
                lastMessageTime = System.currentTimeMillis(),
                lastMessage = ""
            )
            matchRef.setValue(match).await()

            Result.success(matchId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun findExistingMatch(user1Id: String, user2Id: String): String? {
        return try {
            // Check both directions
            val snapshot1 = matchesRef.orderByChild("user1Id").equalTo(user1Id).get().await()
            snapshot1.children.forEach { child ->
                @Suppress("UNCHECKED_CAST")
                val match = child.getValue(Map::class.java) as? Map<String, Any>
                if (match?.get("user2Id") == user2Id) {
                    return child.key
                }
            }

            val snapshot2 = matchesRef.orderByChild("user1Id").equalTo(user2Id).get().await()
            snapshot2.children.forEach { child ->
                @Suppress("UNCHECKED_CAST")
                val match = child.getValue(Map::class.java) as? Map<String, Any>
                if (match?.get("user2Id") == user1Id) {
                    return child.key
                }
            }

            null
        } catch (e: Exception) {
            null
        }
    }
}