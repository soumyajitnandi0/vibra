package com.example.classcrush.data.repository

import android.net.Uri
import com.example.classcrush.data.model.User
import com.example.classcrush.data.service.CloudinaryService
import com.example.classcrush.data.service.CloudinaryUploadResult
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
class UserRepository @Inject constructor(
    private val database: FirebaseDatabase,
    private val cloudinaryService: CloudinaryService
) {

    private val usersRef = database.getReference("users")
    private val swipesRef = database.getReference("swipes")
    private val matchesRef = database.getReference("matches")
    private val blocksRef = database.getReference("blocks")
    private val reportsRef = database.getReference("reports")

    suspend fun createUser(user: User): Result<Unit> {
        return try {
            usersRef.child(user.id).setValue(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUser(user: User): Result<Unit> {
        return try {
            usersRef.child(user.id).setValue(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserField(userId: String, field: String, value: Any): Result<Unit> {
        return try {
            usersRef.child(userId).child(field).setValue(value).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOnlineStatus(userId: String, isOnline: Boolean): Result<Unit> {
        return try {
            val updates = mapOf(
                "isOnline" to isOnline,
                "lastSeen" to System.currentTimeMillis()
            )
            usersRef.child(userId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUser(userId: String): Result<User?> {
        return try {
            val snapshot = usersRef.child(userId).get().await()
            val user = snapshot.getValue(User::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUserFlow(userId: String): Flow<User?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                trySend(user)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        usersRef.child(userId).addValueEventListener(listener)
        awaitClose { usersRef.child(userId).removeEventListener(listener) }
    }

    suspend fun uploadProfileImage(userId: String, imageUri: Uri): Result<CloudinaryUploadResult> {
        return try {
            cloudinaryService.uploadProfileImage(userId, imageUri, true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadAdditionalImage(userId: String, imageUri: Uri): Result<CloudinaryUploadResult> {
        return try {
            cloudinaryService.uploadProfileImage(userId, imageUri, false)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteImage(publicId: String): Result<Unit> {
        return try {
            cloudinaryService.deleteImage(publicId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfileImage(userId: String, imageUri: Uri): Result<String> {
        return try {
            cloudinaryService.uploadProfileImage(userId, imageUri, true)
                .map { result ->
                    val user = getUser(userId).getOrNull()
                    user?.let {
                        val updatedUser = it.copy(
                            profileImageUrl = result.secureUrl,
                            profileImagePublicId = result.publicId
                        )
                        updateUser(updatedUser)
                    }
                    result.secureUrl
                }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPotentialMatches(currentUser: User): Result<List<User>> {
        return try {
            android.util.Log.d("UserRepository", "Getting potential matches for: ${currentUser.name}")
            android.util.Log.d("UserRepository", "Current user details - College: '${currentUser.college}', Gender: ${currentUser.gender}, Interested in: ${currentUser.interestedIn}")

            // Get comprehensive swipe history from database for additional filtering
            val databaseSwipedIds = getSwipedUserIds(currentUser.id).getOrNull() ?: emptySet()
            android.util.Log.d("UserRepository", "Database swiped IDs: ${databaseSwipedIds.size} users")

            val snapshot = usersRef.get().await()
            val users = mutableListOf<User>()
            var totalUsers = 0
            var filteredUsers = 0

            snapshot.children.forEach { child ->
                totalUsers++
                try {
                    val user = child.getValue(User::class.java)
                    user?.let {
                        // Only add users that pass all filters (including database swipe check)
                        if (shouldShowUser(currentUser, it, databaseSwipedIds)) {
                            users.add(it)
                            filteredUsers++
                        }
                    }
                } catch (e: Exception) {
                    // Log individual user parsing errors but continue
                    android.util.Log.w("UserRepository", "Failed to parse user: ${e.message}")
                }
            }

            // Enhanced logging for debugging
            android.util.Log.d("UserRepository", """
                |=== MATCH FILTERING RESULTS ===
                |Current User: ${currentUser.name}
                |Current User College: "${currentUser.college}"
                |Current User Gender: ${currentUser.gender}
                |Current User Interested In: ${currentUser.interestedIn}
                |
                |Total users in database: $totalUsers
                |Users passing all filters: $filteredUsers
                |Final matches found: ${users.size}
                |
                |Sample of found users: ${users.take(3).map { "${it.name} (${it.college})" }}
                |================================
            """.trimMargin())

            // Return empty list with helpful message if no matches found
            if (users.isEmpty()) {
                android.util.Log.w("UserRepository", "No potential matches found. This could be due to:")
                android.util.Log.w("UserRepository", "1. Too strict filtering criteria")
                android.util.Log.w("UserRepository", "2. No other users with matching preferences")
                android.util.Log.w("UserRepository", "3. All potential matches already swiped")
                android.util.Log.w("UserRepository", "4. Database permission issues")
            }

            Result.success(users.shuffled())
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Failed to get potential matches: ${e.message}")
            
            // Enhanced error reporting
            val errorMessage = when {
                e.message?.contains("permission", ignoreCase = true) == true -> 
                    "Database permission denied. Please check your authentication status."
                e.message?.contains("network", ignoreCase = true) == true -> 
                    "Network error. Please check your internet connection."
                e.message?.contains("timeout", ignoreCase = true) == true -> 
                    "Request timed out. Please try again."
                else -> "Failed to load profiles: ${e.message}"
            }
            
            Result.failure(Exception(errorMessage))
        }
    }

    private fun shouldShowUser(currentUser: User, potentialMatch: User, databaseSwipedIds: Set<String> = emptySet()): Boolean {
        val logPrefix = "Filter Check [${potentialMatch.name}]:"

        // Basic checks
        if (potentialMatch.id == currentUser.id) {
            android.util.Log.d("UserRepository", "$logPrefix ❌ SKIP - Same user")
            return false
        }

        // Check if already swiped (liked or disliked) by current user
        if (currentUser.likedUsers.contains(potentialMatch.id)) {
            android.util.Log.d("UserRepository", "$logPrefix ❌ SKIP - Already liked by current user")
            return false
        }

        if (currentUser.dislikedUsers.contains(potentialMatch.id)) {
            android.util.Log.d("UserRepository", "$logPrefix ❌ SKIP - Already disliked by current user")
            return false
        }

        // Additional check: Database swipe records (comprehensive filtering)
        if (databaseSwipedIds.contains(potentialMatch.id)) {
            android.util.Log.d("UserRepository", "$logPrefix ❌ SKIP - Already swiped in database records")
            return false
        }

        // NOTE: We purposely do NOT filter out users who have swiped on the current user,
        // and we do NOT apply global swipe filtering. The product requirement is:
        // "exclude only profiles the current user has already swiped (left/right)."

        // Check blocking status
        if (currentUser.blockedUsers.contains(potentialMatch.id)) {
            android.util.Log.d("UserRepository", "$logPrefix ❌ SKIP - User blocked by current user")
            return false
        }

        if (potentialMatch.blockedUsers.contains(currentUser.id)) {
            android.util.Log.d("UserRepository", "$logPrefix ❌ SKIP - Current user blocked by this user")
            return false
        }

        // Check if already matched (to avoid showing matched users again)
        if (currentUser.matches.contains(potentialMatch.id) || potentialMatch.matches.contains(currentUser.id)) {
            android.util.Log.d("UserRepository", "$logPrefix ❌ SKIP - Already matched")
            return false
        }

        // Must have profile image
        if (potentialMatch.profileImagePublicId.isEmpty()) {
            android.util.Log.d("UserRepository", "$logPrefix ❌ SKIP - No profile image")
            return false
        }

        // More relaxed activity check (online within last 90 days) or if lastSeen is 0 (new user)
        val ninetyDaysAgo = System.currentTimeMillis() - (90 * 24 * 60 * 60 * 1000L)
        if (potentialMatch.lastSeen != 0L && potentialMatch.lastSeen < ninetyDaysAgo) {
            android.util.Log.d("UserRepository", "$logPrefix ❌ SKIP - User inactive (last seen: ${potentialMatch.lastSeen})")
            return false
        }

        // College match check
        val collegeMatch = isCollegeMatch(currentUser.college, potentialMatch.college)
        if (!collegeMatch) {
            android.util.Log.d("UserRepository", """
                |$logPrefix ❌ SKIP - College mismatch
                |  Current: "${currentUser.college}"
                |  Potential: "${potentialMatch.college}"
            """.trimMargin())
            return false
        } else {
            android.util.Log.d("UserRepository", """
                |$logPrefix ✅ PASS - College match
                |  Current: "${currentUser.college}"
                |  Potential: "${potentialMatch.college}"
            """.trimMargin())
        }

        // One-sided gender preference: show only the gender the current user selected
        val genderAllowed = when (currentUser.interestedIn) {
            com.example.classcrush.data.model.Gender.ALL -> true
            else -> potentialMatch.gender == currentUser.interestedIn
        }
        if (!genderAllowed) {
            android.util.Log.d("UserRepository", "$logPrefix ❌ SKIP - Does not match current user's gender preference (${currentUser.interestedIn})")
            return false
        } else {
            android.util.Log.d("UserRepository", "$logPrefix ✅ PASS - Matches current user's gender preference (${currentUser.interestedIn})")
        }

        android.util.Log.d("UserRepository", "$logPrefix ✅ FINAL PASS - All filters passed")
        return true
    }

    private fun isCollegeMatch(college1: String, college2: String): Boolean {
        // More flexible college matching
        val normalized1 = college1.trim().lowercase()
        val normalized2 = college2.trim().lowercase()
        
        // Return true if both are empty (allow users without college set)
        if (normalized1.isEmpty() && normalized2.isEmpty()) return true
        
        // Exact match
        if (normalized1 == normalized2) return true
        
        // Partial matching for common college name variations
        return when {
            normalized1.contains(normalized2) || normalized2.contains(normalized1) -> true
            // Add more flexible matching if needed
            else -> false
        }
    }

    private fun checkGenderPreferences(currentUser: User, potentialMatch: User): Boolean {
        val currentUserGender = currentUser.gender
        val currentUserInterestedIn = currentUser.interestedIn
        val potentialMatchGender = potentialMatch.gender
        val potentialMatchInterestedIn = potentialMatch.interestedIn

        // Check if current user likes potential match's gender
        val currentUserLikesPotentialMatch = when (currentUserInterestedIn) {
            com.example.classcrush.data.model.Gender.ALL -> true
            else -> currentUserInterestedIn == potentialMatchGender
        }

        // Check if potential match likes current user's gender
        val potentialMatchLikesCurrentUser = when (potentialMatchInterestedIn) {
            com.example.classcrush.data.model.Gender.ALL -> true
            else -> potentialMatchInterestedIn == currentUserGender
        }

        val isMatch = currentUserLikesPotentialMatch && potentialMatchLikesCurrentUser

        // Detailed logging for debugging
        android.util.Log.d("UserRepository", """
            |Gender Match Analysis:
            |  Current User (${currentUser.name}): ${currentUserGender} → interested in ${currentUserInterestedIn}
            |  Potential Match (${potentialMatch.name}): ${potentialMatchGender} → interested in ${potentialMatchInterestedIn}
            |  
            |  Does current user like potential match? $currentUserLikesPotentialMatch
            |    (${currentUserInterestedIn} matches ${potentialMatchGender} or is ALL)
            |  Does potential match like current user? $potentialMatchLikesCurrentUser  
            |    (${potentialMatchInterestedIn} matches ${currentUserGender} or is ALL)
            |    
            |  Final Result: $isMatch
        """.trimMargin())

        return isMatch
    }

    // Enhanced swipe tracking with database persistence
    suspend fun likeUser(currentUserId: String, likedUserId: String): Result<Boolean> {
        return try {
            // Record swipe action in database
            val swipeRef = swipesRef.push()
            val swipeId = swipeRef.key ?: ""
            val swipeData = mapOf(
                "id" to swipeId,
                "userId" to currentUserId,
                "targetUserId" to likedUserId,
                "direction" to "right",
                "timestamp" to System.currentTimeMillis()
            )
            swipeRef.setValue(swipeData).await()

            // Add to current user's liked list
            val currentUserSnapshot = usersRef.child(currentUserId).get().await()
            val currentUser = currentUserSnapshot.getValue(User::class.java)

            currentUser?.let { user ->
                val updatedLikedUsers = user.likedUsers.toMutableList()
                if (!updatedLikedUsers.contains(likedUserId)) {
                    updatedLikedUsers.add(likedUserId)
                    usersRef.child(currentUserId).child("likedUsers").setValue(updatedLikedUsers).await()
                }

                // Check if it's a mutual match
                val likedUserSnapshot = usersRef.child(likedUserId).get().await()
                val likedUser = likedUserSnapshot.getValue(User::class.java)

                val isMatch = likedUser?.likedUsers?.contains(currentUserId) == true

                if (isMatch) {
                    android.util.Log.d("UserRepository", "Mutual match detected! Creating match record...")

                    // Check if match already exists
                    val existingMatch = matchesRef.orderByChild("user1Id").equalTo(currentUserId).get().await().children.any {
                        @Suppress("UNCHECKED_CAST")
                        val match = it.getValue(Map::class.java) as? Map<String, Any>
                        match?.get("user2Id") == likedUserId && match?.get("status") == "active"
                    } || matchesRef.orderByChild("user1Id").equalTo(likedUserId).get().await().children.any {
                        @Suppress("UNCHECKED_CAST")
                        val match = it.getValue(Map::class.java) as? Map<String, Any>
                        match?.get("user2Id") == currentUserId && match?.get("status") == "active"
                    }

                    if (!existingMatch) {
                        // Create match record
                        val matchRef = matchesRef.push()
                        val matchId = matchRef.key ?: ""
                        val matchData = mapOf(
                            "id" to matchId,
                            "user1Id" to currentUserId,
                            "user2Id" to likedUserId,
                            "timestamp" to System.currentTimeMillis(),
                            "status" to "active",
                            "lastMessage" to "",
                            "lastMessageTime" to System.currentTimeMillis()
                        )
                        matchRef.setValue(matchData).await()
                    }

                    // Only update current user's matches - avoid cross-user writes
                    val currentUserMatches = user.matches.toMutableList()
                    if (!currentUserMatches.contains(likedUserId)) {
                        currentUserMatches.add(likedUserId)
                        usersRef.child(currentUserId).child("matches").setValue(currentUserMatches).await()
                    }
                }

                Result.success(isMatch)
            } ?: Result.failure(Exception("User not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Add dislikeUser method
    suspend fun dislikeUser(currentUserId: String, dislikedUserId: String): Result<Unit> {
        return try {
            // Record swipe action in database
            val swipeRef = swipesRef.push()
            val swipeId = swipeRef.key ?: ""
            val swipeData = mapOf(
                "id" to swipeId,
                "userId" to currentUserId,
                "targetUserId" to dislikedUserId,
                "direction" to "left",
                "timestamp" to System.currentTimeMillis()
            )
            swipeRef.setValue(swipeData).await()

            // Add to current user's disliked list
            val currentUserSnapshot = usersRef.child(currentUserId).get().await()
            val currentUser = currentUserSnapshot.getValue(User::class.java)

            currentUser?.let { user ->
                val updatedDislikedUsers = user.dislikedUsers.toMutableList()
                if (!updatedDislikedUsers.contains(dislikedUserId)) {
                    updatedDislikedUsers.add(dislikedUserId)
                    usersRef.child(currentUserId).child("dislikedUsers").setValue(updatedDislikedUsers).await()
                }

                Result.success(Unit)
            } ?: Result.failure(Exception("User not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Helper to check if two users are matched and eligible for chat
    suspend fun areUsersMatched(userId1: String, userId2: String): Boolean {
        return try {
            val snapshot1 = matchesRef.orderByChild("user1Id").equalTo(userId1).get().await()
            val snapshot2 = matchesRef.orderByChild("user2Id").equalTo(userId1).get().await()
            val matches = (snapshot1.children + snapshot2.children).mapNotNull { 
                @Suppress("UNCHECKED_CAST")
                it.getValue(Map::class.java) as? Map<String, Any>
            }
            matches.any { (it["user1Id"] == userId1 && it["user2Id"] == userId2 || it["user1Id"] == userId2 && it["user2Id"] == userId1) && it["status"] == "active" }
        } catch (e: Exception) {
            false
        }
    }

    // Enhanced blocking functionality
    suspend fun blockUser(currentUserId: String, blockedUserId: String): Result<Unit> {
        return try {
            // Record block in database
            val blockRef = blocksRef.push()
            val blockId = blockRef.key ?: ""
            val blockData = mapOf(
                "id" to blockId,
                "blockerId" to currentUserId,
                "blockedId" to blockedUserId,
                "timestamp" to System.currentTimeMillis()
            )
            blockRef.setValue(blockData).await()

            // Add to current user's blocked list
            val currentUserSnapshot = usersRef.child(currentUserId).get().await()
            val currentUser = currentUserSnapshot.getValue(User::class.java)

            currentUser?.let { user ->
                val updatedBlockedUsers = user.blockedUsers.toMutableList()
                if (!updatedBlockedUsers.contains(blockedUserId)) {
                    updatedBlockedUsers.add(blockedUserId)
                    usersRef.child(currentUserId).child("blockedUsers").setValue(updatedBlockedUsers).await()
                }

                // Remove from matches if they exist
                val updatedMatches = user.matches.toMutableList()
                if (updatedMatches.contains(blockedUserId)) {
                    updatedMatches.remove(blockedUserId)
                    usersRef.child(currentUserId).child("matches").setValue(updatedMatches).await()
                }

                // Remove from liked users if they exist
                val updatedLikedUsers = user.likedUsers.toMutableList()
                if (updatedLikedUsers.contains(blockedUserId)) {
                    updatedLikedUsers.remove(blockedUserId)
                    usersRef.child(currentUserId).child("likedUsers").setValue(updatedLikedUsers).await()
                }

                Result.success(Unit)
            } ?: Result.failure(Exception("User not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Unmatch functionality
    suspend fun unmatchUser(currentUserId: String, unmatchedUserId: String): Result<Unit> {
        return try {
            // Remove from current user's matches
            val currentUserSnapshot = usersRef.child(currentUserId).get().await()
            val currentUser = currentUserSnapshot.getValue(User::class.java)

            currentUser?.let { user ->
                val updatedMatches = user.matches.toMutableList()
                if (updatedMatches.contains(unmatchedUserId)) {
                    updatedMatches.remove(unmatchedUserId)
                    usersRef.child(currentUserId).child("matches").setValue(updatedMatches).await()
                }

                // Note: We only update current user's matches to avoid permission issues

                // Update match status in database
                val matchSnapshot = matchesRef.orderByChild("user1Id").equalTo(currentUserId).get().await()
                matchSnapshot.children.forEach { child ->
                    @Suppress("UNCHECKED_CAST")
                    val match = child.getValue(Map::class.java) as? Map<String, Any>
                    if (match?.get("user2Id") == unmatchedUserId) {
                        matchesRef.child(child.key ?: "").child("status").setValue("unmatched").await()
                    }
                }

                val matchSnapshot2 = matchesRef.orderByChild("user2Id").equalTo(currentUserId).get().await()
                matchSnapshot2.children.forEach { child ->
                    @Suppress("UNCHECKED_CAST")
                    val match = child.getValue(Map::class.java) as? Map<String, Any>
                    if (match?.get("user1Id") == unmatchedUserId) {
                        matchesRef.child(child.key ?: "").child("status").setValue("unmatched").await()
                    }
                }

                Result.success(Unit)
            } ?: Result.failure(Exception("User not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Report functionality
    suspend fun reportUser(reporterId: String, reportedId: String, reason: String): Result<Unit> {
        return try {
            val reportRef = reportsRef.push()
            val reportId = reportRef.key ?: ""
            val reportData = mapOf(
                "id" to reportId,
                "reporterId" to reporterId,
                "reportedId" to reportedId,
                "reason" to reason,
                "timestamp" to System.currentTimeMillis(),
                "status" to "pending"
            )
            reportRef.setValue(reportData).await()

            // Add to current user's reported list
            val currentUserSnapshot = usersRef.child(reporterId).get().await()
            val currentUser = currentUserSnapshot.getValue(User::class.java)

            currentUser?.let { user ->
                val updatedReportedUsers = user.reportedUsers.toMutableList()
                if (!updatedReportedUsers.contains(reportedId)) {
                    updatedReportedUsers.add(reportedId)
                    usersRef.child(reporterId).child("reportedUsers").setValue(updatedReportedUsers).await()
                }
                Result.success(Unit)
            } ?: Result.failure(Exception("User not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get user's swiped user IDs (for filtering purposes)
    suspend fun getSwipedUserIds(userId: String): Result<Set<String>> {
        return try {
            val snapshot = swipesRef.orderByChild("userId").equalTo(userId).get().await()
            val swipedIds = mutableSetOf<String>()

            snapshot.children.forEach { child ->
                @Suppress("UNCHECKED_CAST")
                val swipe = child.getValue(Map::class.java) as? Map<String, Any>
                val targetUserId = swipe?.get("targetUserId") as? String
                if (targetUserId != null) {
                    swipedIds.add(targetUserId)
                }
            }

            android.util.Log.d("UserRepository", "Found ${swipedIds.size} swiped users from database for user $userId")
            Result.success(swipedIds)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Failed to get swiped user IDs: ${e.message}")
            Result.failure(e)
        }
    }

    // Get user's swipe history
    suspend fun getSwipeHistory(userId: String): Result<List<Map<String, Any>>> {
        return try {
            val snapshot = swipesRef.orderByChild("userId").equalTo(userId).get().await()
            val swipes = mutableListOf<Map<String, Any>>()

            snapshot.children.forEach { child ->
                @Suppress("UNCHECKED_CAST")
                val swipe = child.getValue(Map::class.java) as? Map<String, Any>
                if (swipe != null) {
                    val swipeDirection = swipe["direction"] as? String ?: ""
                    val targetUserId = swipe["targetUserId"] as? String ?: ""
                    val timestamp = swipe["timestamp"] as? Long ?: 0L

                    if (swipeDirection == "right" && targetUserId.isNotEmpty()) {
                        val targetUser = getUser(targetUserId).getOrNull()
                        if (targetUser != null) {
                            swipes.add(
                                mapOf(
                                    "id" to swipe.get("id"),
                                    "userId" to swipe.get("userId"),
                                    "targetUserId" to targetUserId,
                                    "direction" to swipeDirection,
                                    "timestamp" to timestamp,
                                    "user" to targetUser
                                ) as Map<String, Any>
                            )
                        }
                    }
                }
            }

            Result.success(swipes.sortedByDescending { it["timestamp"] as Long })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get user's active matches
    suspend fun getActiveMatches(userId: String): Result<List<Map<String, Any>>> {
        return try {
            val snapshot1 = matchesRef.orderByChild("user1Id").equalTo(userId).get().await()
            val snapshot2 = matchesRef.orderByChild("user2Id").equalTo(userId).get().await()

            val matches = mutableListOf<Map<String, Any>>()

            snapshot1.children.forEach { child ->
                @Suppress("UNCHECKED_CAST")
                val rawMatch = child.getValue(Map::class.java)
                if (rawMatch != null) {
                    val match = rawMatch as Map<*, *>
                    if (match["status"] == "active") {
                        // Create a new map with the correct type
                        val typedMatch = mutableMapOf<String, Any>()
                        match.forEach { (key, value) ->
                            if (key is String) {
                                typedMatch[key] = value as Any
                            }
                        }
                        matches.add(typedMatch)
                    }
                }
            }

            snapshot2.children.forEach { child ->
                @Suppress("UNCHECKED_CAST")
                val rawMatch = child.getValue(Map::class.java)
                if (rawMatch != null) {
                    val match = rawMatch as Map<*, *>
                    if (match["status"] == "active") {
                        // Create a new map with the correct type
                        val typedMatch = mutableMapOf<String, Any>()
                        match.forEach { (key, value) ->
                            if (key is String) {
                                typedMatch[key] = value as Any
                            }
                        }
                        matches.add(typedMatch)
                    }
                }
            }

            Result.success(matches.sortedByDescending { it["timestamp"] as Long })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
