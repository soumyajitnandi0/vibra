package com.example.classcrush.presentation.screen

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
// import androidx.compose.material.icons.filled.Undo // Undo removed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.classcrush.data.model.User
import com.example.classcrush.presentation.component.ProfileImage
import com.example.classcrush.presentation.component.SwipeableCard
import com.example.classcrush.presentation.viewmodel.DiscoverViewModel

@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current

    // Load matches when the screen is first composed
    LaunchedEffect(Unit) {
        android.util.Log.d("DiscoverScreen", "Screen launched, triggering loadPotentialMatches")
        viewModel.loadPotentialMatches()
    }

    // Debug the current state
    LaunchedEffect(uiState) {
        android.util.Log.d("DiscoverScreen", "UI State changed:")
        android.util.Log.d("DiscoverScreen", "- isLoading: ${uiState.isLoading}")
        android.util.Log.d("DiscoverScreen", "- error: ${uiState.error}")
        android.util.Log.d("DiscoverScreen", "- potentialMatches size: ${uiState.potentialMatches.size}")
        android.util.Log.d("DiscoverScreen", "- currentIndex: ${uiState.currentIndex}")

        if (uiState.potentialMatches.isNotEmpty()) {
            android.util.Log.d("DiscoverScreen", "- matches: ${uiState.potentialMatches.map { "${it.name} (${it.college})" }}")
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            uiState.isLoading -> {
                android.util.Log.d("DiscoverScreen", "Showing loading state")
                LoadingState()
            }

            uiState.error != null -> {
                android.util.Log.d("DiscoverScreen", "Showing error state: ${uiState.error}")
                ErrorState(
                    error = uiState.error!!,
                    onRetry = {
                        android.util.Log.d("DiscoverScreen", "Retry button clicked")
                        viewModel.clearError()
                        viewModel.refreshMatches()
                    },
                    onShowDebugInfo = {
                        android.util.Log.d("DiscoverScreen", "Debug info requested:")
                        android.util.Log.d("DiscoverScreen", viewModel.getCurrentUserInfo())
                    }
                )
            }

            uiState.potentialMatches.isEmpty() -> {
                android.util.Log.d("DiscoverScreen", "Showing empty state")
                EmptyState(
                    onRefresh = {
                        android.util.Log.d("DiscoverScreen", "Refresh button clicked in empty state")
                        viewModel.refreshMatches()
                    }
                )
            }

            else -> {
                android.util.Log.d("DiscoverScreen", "Showing card stack with ${uiState.potentialMatches.size} matches, current index: ${uiState.currentIndex}")

                // Ensure currentIndex is valid
                if (uiState.currentIndex < uiState.potentialMatches.size) {
                    // Card Stack
                    CardStack(
                        users = uiState.potentialMatches,
                        currentIndex = uiState.currentIndex,
                        onSwipeLeft = { user ->
                            android.util.Log.d("DiscoverScreen", "Swiped left on: ${user.name}")
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.dislikeUser(user)
                        },
                        onSwipeRight = { user ->
                            android.util.Log.d("DiscoverScreen", "Swiped right on: ${user.name}")
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.likeUser(user)
                        }
                    )

                    // Daily Swipe Counter
                    DailySwipeCounter(
                        currentCount = viewModel.getDailySwipeCount(),
                        maxCount = 50,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                    )

                    // Action Buttons
                    ActionButtons(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(32.dp),
                        onPassClick = {
                            uiState.potentialMatches.getOrNull(uiState.currentIndex)?.let { user ->
                                android.util.Log.d("DiscoverScreen", "Pass button clicked for: ${user.name}")
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.dislikeUser(user)
                            }
                        },
                        onSuperLikeClick = {
                            uiState.potentialMatches.getOrNull(uiState.currentIndex)?.let { user ->
                                android.util.Log.d("DiscoverScreen", "Super like button clicked for: ${user.name}")
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.likeUser(user)
                            }
                        },
                        onLikeClick = {
                            uiState.potentialMatches.getOrNull(uiState.currentIndex)?.let { user ->
                                android.util.Log.d("DiscoverScreen", "Like button clicked for: ${user.name}")
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.likeUser(user)
                            }
                        },
                        enabled = !uiState.isLoading && !viewModel.hasReachedDailyLimit()
                    )
                } else {
                    android.util.Log.w("DiscoverScreen", "Invalid currentIndex: ${uiState.currentIndex} >= ${uiState.potentialMatches.size}")
                    EmptyState(onRefresh = { viewModel.refreshMatches() })
                }
            }
        }

        // Match Dialog
        if (uiState.showMatchDialog && uiState.newMatch != null) {
            android.util.Log.d("DiscoverScreen", "Showing match dialog for: ${uiState.newMatch!!.name}")
            MatchDialog(
                matchedUser = uiState.newMatch!!,
                matchPercentage = uiState.matchPercentage,
                onDismiss = {
                    android.util.Log.d("DiscoverScreen", "Match dialog dismissed")
                    viewModel.dismissMatchDialog()
                },
                onSendMessage = {
                    android.util.Log.d("DiscoverScreen", "Send message clicked")
                    viewModel.dismissMatchDialog()
                    // TODO: Navigate to chat
                }
            )
        }
    }
}

@Composable
fun CardStack(
    users: List<User>,
    currentIndex: Int,
    onSwipeLeft: (User) -> Unit,
    onSwipeRight: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Show up to 3 cards in stack
        for (i in 0 until minOf(3, users.size - currentIndex)) {
            val userIndex = currentIndex + i
            if (userIndex < users.size) {
                val user = users[userIndex]
                val isTopCard = i == 0

                android.util.Log.d("CardStack", "Rendering card $i for user: ${user.name} (index: $userIndex, isTopCard: $isTopCard)")

                SwipeableCard(
                    user = user,
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = (i * 4).dp), // Slight offset for stack effect
                    onSwipeLeft = { if (isTopCard) onSwipeLeft(user) },
                    onSwipeRight = { if (isTopCard) onSwipeRight(user) },
                    isTopCard = isTopCard
                )
            }
        }
    }
}

@Composable
fun ActionButtons(
    modifier: Modifier = Modifier,
    onPassClick: () -> Unit,
    onSuperLikeClick: () -> Unit,
    onLikeClick: () -> Unit,
    enabled: Boolean = true
) {
    var passPressed by remember { mutableStateOf(false) }
    var superLikePressed by remember { mutableStateOf(false) }
    var likePressed by remember { mutableStateOf(false) }
    // Undo removed

    val passScale by animateFloatAsState(
        targetValue = if (passPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "passScale"
    )

    val superLikeScale by animateFloatAsState(
        targetValue = if (superLikePressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "superLikeScale"
    )

    val likeScale by animateFloatAsState(
        targetValue = if (likePressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "likeScale"
    )

    // Undo removed

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Undo button removed

        // Pass Button
        FloatingActionButton(
            onClick = {
                if (enabled) {
                    passPressed = true
                    onPassClick()
                    passPressed = false
                }
            },
            modifier = Modifier
                .size(56.dp)
                .scale(passScale),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = Color.Red,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Pass",
                modifier = Modifier.size(24.dp)
            )
        }

        // Super Like Button
        FloatingActionButton(
            onClick = {
                if (enabled) {
                    superLikePressed = true
                    onSuperLikeClick()
                    superLikePressed = false
                }
            },
            modifier = Modifier
                .size(48.dp)
                .scale(superLikeScale),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = "Super Like",
                modifier = Modifier.size(20.dp)
            )
        }

        // Like Button
        FloatingActionButton(
            onClick = {
                if (enabled) {
                    likePressed = true
                    onLikeClick()
                    likePressed = false
                }
            },
            modifier = Modifier
                .size(56.dp)
                .scale(likeScale),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = Color.Green,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        ) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = "Like",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Finding your matches...",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyState(
    onRefresh: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "🎓",
                fontSize = 64.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "No more profiles",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Check back later for new students!",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Button(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Refresh")
            }
        }
    }
}

@Composable
fun DailySwipeCounter(
    currentCount: Int,
    maxCount: Int,
    modifier: Modifier = Modifier
) {
    val progress = currentCount.toFloat() / maxCount.toFloat()
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (progress > 0.8f) Color.Red else MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "$currentCount/$maxCount",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (progress > 0.8f) Color.Red else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun MatchDialog(
    matchedUser: User,
    matchPercentage: Int?,
    onDismiss: () -> Unit,
    onSendMessage: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "It's a Match! 🎉",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Matched user image
                Card(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    ProfileImage(
                        publicId = matchedUser.profileImagePublicId,
                        contentDescription = "Matched user photo",
                        size = 100.dp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "You and ${matchedUser.name} liked each other!",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Show match percentage if available
                matchPercentage?.let { percentage ->
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "💫",
                                fontSize = 16.sp
                            )
                            Text(
                                text = "$percentage% Match",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSendMessage,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send Message")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Swiping")
            }
        }
    )
}

@Composable
fun ErrorState(
    error: String,
    onRetry: () -> Unit,
    onShowDebugInfo: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "⚠️",
                fontSize = 64.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Oops! Something went wrong",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = error,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Try Again")
            }

            // Debug button (remove in production)
            TextButton(
                onClick = onShowDebugInfo,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Show Debug Info")
            }
        }
    }
}