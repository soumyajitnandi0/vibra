# Discover Page Swipe Logic (Tinder-style)

## 🎯 Overview

The Discover page implements a Tinder-style swipe interface where users can swipe right to "Like" or left to "Pass" on potential matches. The implementation includes smooth animations, haptic feedback, and intelligent match detection.

## 🏗️ Core Architecture

### 1. **SwipeableCard Component** (`SwipeableCard.kt`)
The heart of the swipe functionality with enhanced features:

#### Key Features:
- **Velocity-based swiping**: Cards can be swiped with velocity detection
- **Smooth animations**: Spring-based animations for natural feel
- **Visual feedback**: Progress-based indicators and rotation
- **Haptic feedback**: Tactile response on swipe actions
- **Multi-touch support**: Only top card is interactive

#### Swipe Thresholds:
```kotlin
val swipeThreshold = screenWidth * 0.25f // 25% of screen width
val maxRotation = 15f // Maximum rotation angle
val velocityThreshold = 1000f // pixels per second
```

#### Animation Configuration:
```kotlin
animationSpec = spring(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow
)
```

### 2. **DiscoverViewModel** (`DiscoverViewModel.kt`)
Manages the business logic and state:

#### Key Features:
- **Daily swipe limits**: 50 swipes per day for gamification
- **Match percentage calculation**: Based on common interests
- **Swipe history**: Enables undo functionality
- **Real-time state management**: Reactive UI updates

#### Match Percentage Algorithm:
```kotlin
private fun calculateMatchPercentage(user1: User, user2: User): Int {
    var matchPoints = 0
    
    // Same department (30 points)
    if (user1.department == user2.department) matchPoints += 30
    
    // Same year (20 points)
    if (user1.year == user2.year) matchPoints += 20
    
    // Same college (15 points)
    if (user1.college == user2.college) matchPoints += 15
    
    // Age compatibility (10 points)
    val ageDifference = abs(user1.age - user2.age)
    if (ageDifference <= 2) matchPoints += 10
    else if (ageDifference <= 5) matchPoints += 5
    
    // Bio similarity (25 points)
    val bio1 = user1.bio.lowercase()
    val bio2 = user2.bio.lowercase()
    val commonWords = bio1.split(" ").intersect(bio2.split(" ").toSet())
    matchPoints += minOf(25, commonWords.size * 5)
    
    return minOf(100, matchPoints)
}
```

### 3. **DiscoverScreen** (`DiscoverScreen.kt`)
The main UI component with enhanced features:

#### Key Features:
- **Daily swipe counter**: Visual indicator of remaining swipes
- **Card stack**: Shows up to 3 cards with depth effect
- **Action buttons**: Manual swipe controls
- **Match dialog**: Enhanced with match percentage
- **Error handling**: Comprehensive error states

## 🔄 How It Works (Step-by-Step)

### 1. **Load Profiles**
```kotlin
fun loadPotentialMatches() {
    // Check daily swipe limit
    if (hasReachedDailyLimit()) {
        showError("Daily swipe limit reached")
        return
    }
    
    // Load filtered profiles from Firebase
    userRepository.getPotentialMatches(user)
        .onSuccess { users ->
            _uiState.value = _uiState.value.copy(
                potentialMatches = users,
                currentIndex = 0
            )
        }
}
```

### 2. **Display Card Stack**
```kotlin
@Composable
fun CardStack(
    users: List<User>,
    currentIndex: Int,
    onSwipeLeft: (User) -> Unit,
    onSwipeRight: (User) -> Unit
) {
    // Show up to 3 cards in stack
    for (i in 0 until minOf(3, users.size - currentIndex)) {
        val userIndex = currentIndex + i
        val user = users[userIndex]
        val isTopCard = i == 0
        
        SwipeableCard(
            user = user,
            onSwipeLeft = { if (isTopCard) onSwipeLeft(user) },
            onSwipeRight = { if (isTopCard) onSwipeRight(user) },
            isTopCard = isTopCard
        )
    }
}
```

### 3. **Handle Swipe Gestures**
```kotlin
// In SwipeableCard.kt
detectDragGestures(
    onDragStart = {
        isDragging = true
        dragStartTime = System.currentTimeMillis()
    },
    onDragEnd = {
        isDragging = false
        val dragDuration = System.currentTimeMillis() - dragStartTime
        val velocity = abs(offsetX) / (dragDuration / 1000f)
        
        when {
            abs(offsetX) > swipeThreshold || velocity > velocityThreshold -> {
                // Animate card off screen
                isAnimating = true
                val targetOffset = if (offsetX > 0) screenWidth * 1.5f else -screenWidth * 1.5f
                offsetX = targetOffset
            }
            else -> {
                // Snap back to center
                animate(offsetX, 0f, spring()) { value, _ ->
                    offsetX = value
                }
            }
        }
    },
    onDrag = { _, dragAmount ->
        if (isTopCard && !isAnimating) {
            offsetX += dragAmount.x
            offsetY += dragAmount.y * 0.2f // Reduce vertical movement
        }
    }
)
```

### 4. **Process Swipe Decision**
```kotlin
fun likeUser(likedUser: User) {
    // Check daily limit
    if (hasReachedDailyLimit()) {
        showError("Daily swipe limit reached")
        return
    }
    
    // Increment daily count
    dailySwipeCount++
    
    // Add to swipe history
    swipeHistory.add(SwipeAction(likedUser, SwipeType.LIKE, System.currentTimeMillis()))
    
    // Calculate match percentage
    val matchPercentage = calculateMatchPercentage(currentUser, likedUser)
    
    // Save to Firebase
    userRepository.likeUser(currentUser.id, likedUser.id)
        .onSuccess { isMatch ->
            if (isMatch) {
                // Create chat match
                chatRepository.createMatch(currentUser.id, likedUser.id)
                showMatchDialog(likedUser, matchPercentage)
            }
            moveToNextCard()
        }
}
```

### 5. **Mutual Match Detection**
```kotlin
// In UserRepository.kt
suspend fun likeUser(userId: String, likedUserId: String): Result<Boolean> {
    return try {
        // Add to liked users
        val user = getUser(userId).getOrNull()
        val likedUser = getUser(likedUserId).getOrNull()
        
        if (user != null && likedUser != null) {
            val updatedLikedUsers = user.likedUsers.toMutableList()
            if (!updatedLikedUsers.contains(likedUserId)) {
                updatedLikedUsers.add(likedUserId)
                updateUserField(userId, "likedUsers", updatedLikedUsers)
            }
            
            // Check for mutual match
            val isMatch = likedUser.likedUsers.contains(userId)
            Result.success(isMatch)
        } else {
            Result.failure(Exception("User not found"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### 6. **Handle Empty State**
```kotlin
private fun moveToNextCard() {
    val nextIndex = currentIndex + 1
    
    if (nextIndex >= potentialMatches.size) {
        // Load more matches or show empty state
        loadPotentialMatches()
    } else {
        _uiState.value = _uiState.value.copy(currentIndex = nextIndex)
    }
}
```

## 🎨 Visual Enhancements

### 1. **Swipe Indicators**
- **Like indicator**: Green "LIKE" badge with progress-based opacity
- **Pass indicator**: Red "PASS" badge with progress-based opacity
- **Rotation**: Cards rotate based on swipe direction

### 2. **Card Stack Effect**
- **Depth**: Background cards are scaled down (0.92f) and faded (0.7f alpha)
- **Offset**: Cards are slightly offset for 3D effect
- **Elevation**: Different elevation for visual hierarchy

### 3. **Daily Swipe Counter**
```kotlin
@Composable
fun DailySwipeCounter(
    currentCount: Int,
    maxCount: Int
) {
    val progress = currentCount.toFloat() / maxCount.toFloat()
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Row {
            Icon(Icons.Default.Favorite, tint = if (progress > 0.8f) Color.Red else Primary)
            Text("$currentCount/$maxCount")
        }
    }
}
```

### 4. **Match Dialog Enhancement**
```kotlin
@Composable
fun MatchDialog(
    matchedUser: User,
    matchPercentage: Int?,
    onDismiss: () -> Unit,
    onSendMessage: () -> Unit
) {
    AlertDialog(
        title = { Text("It's a Match! 🎉") },
        text = {
            Column {
                // User image
                ProfileImage(matchedUser.profileImagePublicId)
                
                // Match percentage
                matchPercentage?.let { percentage ->
                    Card {
                        Row {
                            Text("💫")
                            Text("$percentage% Match")
                        }
                    }
                }
            }
        }
    )
}
```

## 🎮 Gamification Features

### 1. **Daily Swipe Limits**
- **Limit**: 50 swipes per day
- **Reset**: Automatically resets at midnight
- **Visual feedback**: Counter shows remaining swipes
- **Color coding**: Red when approaching limit

### 2. **Match Percentage**
- **Calculation**: Based on common interests and compatibility
- **Display**: Shown in match dialog
- **Scoring**: Department (30), Year (20), College (15), Age (10), Bio (25)

### 3. **Undo Functionality**
- **History**: Tracks last 10 swipes
- **Reversal**: Can undo likes and passes
- **Firebase sync**: Updates database when undone
- **Visual feedback**: Cards return to stack

## 🔧 Technical Implementation

### 1. **State Management**
```kotlin
data class DiscoverUiState(
    val isLoading: Boolean = false,
    val potentialMatches: List<User> = emptyList(),
    val currentIndex: Int = 0,
    val newMatch: User? = null,
    val showMatchDialog: Boolean = false,
    val matchPercentage: Int? = null,
    val error: String? = null
)
```

### 2. **Swipe History**
```kotlin
data class SwipeAction(
    val user: User,
    val action: SwipeType,
    val timestamp: Long
)

enum class SwipeType {
    LIKE, PASS
}
```

### 3. **Performance Optimizations**
- **Lazy loading**: Only load 3 cards at a time
- **Memory management**: Clear old cards from memory
- **Smooth animations**: 60fps spring animations
- **Efficient rendering**: Use `graphicsLayer` for transforms

## 🚀 Future Enhancements

### 1. **Advanced Features**
- [ ] **Super Like**: Special swipe up gesture
- [ ] **Rewind**: Undo multiple swipes
- [ ] **Boost**: Temporary visibility boost
- [ ] **Filters**: Advanced filtering options

### 2. **Analytics**
- [ ] **Swipe patterns**: Track user behavior
- [ ] **Match success rate**: Measure effectiveness
- [ ] **User engagement**: Monitor daily usage

### 3. **Social Features**
- [ ] **Group matching**: Match with study groups
- [ ] **Event matching**: Match for campus events
- [ ] **Study buddy finder**: Academic matching

## 📱 User Experience

### 1. **Intuitive Gestures**
- **Swipe right**: Like (green indicator)
- **Swipe left**: Pass (red indicator)
- **Tap**: View profile details
- **Long press**: Super like (future)

### 2. **Visual Feedback**
- **Haptic feedback**: Tactile response on actions
- **Smooth animations**: Natural card movement
- **Progress indicators**: Visual swipe progress
- **Color coding**: Intuitive color scheme

### 3. **Accessibility**
- **Screen reader support**: Proper content descriptions
- **High contrast**: Clear visual indicators
- **Large touch targets**: Easy button interaction
- **Voice commands**: Future enhancement

## 🔒 Security & Privacy

### 1. **Data Protection**
- **Secure storage**: Firebase security rules
- **Privacy controls**: User consent management
- **Data retention**: Automatic cleanup policies
- **Encryption**: End-to-end encryption for chats

### 2. **User Safety**
- **Report system**: Easy reporting mechanism
- **Block functionality**: User blocking capability
- **Content moderation**: Automated content filtering
- **Emergency contacts**: Safety features

This implementation provides a robust, scalable, and user-friendly Tinder-style swipe interface that can be easily extended with additional features while maintaining excellent performance and user experience. 