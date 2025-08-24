package com.example.classcrush.presentation.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.classcrush.data.model.User
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

@Composable
fun SwipeableCard(
    user: User,
    modifier: Modifier = Modifier,
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
    onCardClick: () -> Unit = {},
    isTopCard: Boolean = true
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val swipeThreshold = screenWidth * 0.20f // Easier swipe threshold
    val velocitySwipeThreshold = 1200f // px/s for fling-to-swipe
    val maxRotation = 12f // Slightly reduced rotation for stability

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isAnimating by remember { mutableStateOf(false) }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var lastDragTime by remember { mutableLongStateOf(0L) }
    var velocityTracker by remember { mutableStateOf<VelocityTracker?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Enhanced animation values with better spring configuration
    val animatedOffsetX by animateFloatAsState(
        targetValue = if (isAnimating) offsetX else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        finishedListener = {
            if (isAnimating) {
                isAnimating = false
                if (abs(offsetX) > swipeThreshold) {
                    if (offsetX > 0) onSwipeRight() else onSwipeLeft()
                }
                offsetX = 0f
                offsetY = 0f
            }
        },
        label = "offsetX"
    )

    val animatedOffsetY by animateFloatAsState(
        targetValue = if (isAnimating) offsetY else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "offsetY"
    )

    // Calculate rotation based on horizontal offset with smoother curve
    val rotation = (offsetX / screenWidth) * maxRotation * sign(offsetX)

    // Calculate scale for background cards with better visual hierarchy
    val scale = if (isTopCard) 1f else 0.92f
    val alpha = if (isTopCard) 1f else 0.7f

    // Enhanced swipe direction indicators with better thresholds
    val showLikeIndicator = offsetX > 30f
    val showPassIndicator = offsetX < -30f

    // Calculate swipe progress for visual feedback
    val swipeProgress = abs(offsetX) / swipeThreshold
    val likeProgress = (offsetX / swipeThreshold).coerceIn(0f, 1f)
    val passProgress = (-offsetX / swipeThreshold).coerceIn(0f, 1f)

    Card(
        modifier = modifier
            .fillMaxSize()
            .scale(scale)
            .graphicsLayer(
                translationX = if (isDragging) offsetX else animatedOffsetX,
                translationY = if (isDragging) offsetY else animatedOffsetY,
                rotationZ = if (isDragging) rotation else (animatedOffsetX / screenWidth) * maxRotation * sign(animatedOffsetX),
                alpha = alpha
            )
            .zIndex(if (isTopCard) 1f else 0f)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        dragStartTime = System.currentTimeMillis()
                        lastDragTime = dragStartTime
                        // Prepare velocity tracking
                        velocityTracker = VelocityTracker()
                    },
                    onDragEnd = {
                        isDragging = false
                        // Calculate horizontal velocity (px/s)
                        val velocityX = try {
                            velocityTracker?.calculateVelocity()?.x ?: 0f
                        } catch (e: Exception) { 0f }

                        when {
                            abs(offsetX) > swipeThreshold || abs(velocityX) > velocitySwipeThreshold -> {
                                // Animate card off screen with velocity-based animation
                                isAnimating = true
                                val targetOffset = if (offsetX > 0) screenWidth * 1.5f else -screenWidth * 1.5f
                                offsetX = targetOffset
                                offsetY = offsetY * 0.3f // Reduce vertical movement
                            }
                            else -> {
                                // Snap back to center with enhanced spring animation
                                coroutineScope.launch {
                                    animate(
                                        initialValue = offsetX,
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    ) { value, _ ->
                                        offsetX = value
                                    }
                                    animate(
                                        initialValue = offsetY,
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    ) { value, _ ->
                                        offsetY = value
                                    }
                                }
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (isTopCard && !isAnimating) {
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y * 0.2f // Further reduce vertical movement
                            lastDragTime = System.currentTimeMillis()
                            // Track pointer for velocity
                            if (velocityTracker == null) velocityTracker = VelocityTracker()
                            velocityTracker?.addPosition(change.uptimeMillis, change.position)
                        }
                    }
                )
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isTopCard) 8.dp else 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Profile Image Background
            CardImage(
                publicId = user.profileImagePublicId,
                contentDescription = "Profile photo of ${user.name}",
                modifier = Modifier.fillMaxSize()
            )

            // Enhanced Gradient Overlay with better contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.6f),
                                Color.Black.copy(alpha = 0.9f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )

            // Enhanced Swipe Indicators with progress-based opacity
            if (showLikeIndicator) {
                SwipeIndicator(
                    text = "LIKE",
                    color = Color.Green,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                        .rotate(-rotation)
                        .graphicsLayer(alpha = likeProgress),
                    progress = likeProgress
                )
            }

            if (showPassIndicator) {
                SwipeIndicator(
                    text = "PASS",
                    color = Color.Red,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(24.dp)
                        .rotate(-rotation)
                        .graphicsLayer(alpha = passProgress),
                    progress = passProgress
                )
            }

            // User Information with enhanced layout
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                // Name and age with better typography
                Text(
                    text = "${user.name}, ${user.age}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Department and year with improved spacing
                Text(
                    text = "${user.department} • ${user.year}",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 4.dp)
                )

                // College with subtle styling
                Text(
                    text = user.college,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 2.dp)
                )

                // Bio with better text handling
                if (user.bio.isNotEmpty()) {
                    Text(
                        text = user.bio,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(top = 12.dp),
                        maxLines = 3
                    )
                }

                // Add interests if available
                if (user.additionalImages.isNotEmpty()) {
                    Text(
                        text = "📸 ${user.additionalImages.size} more photos",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Enhanced location indicator
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "📍 On Campus",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Add online status indicator
            if (user.isOnline) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Green.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "🟢 Online",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SwipeIndicator(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    progress: Float = 1f
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.9f * progress)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
