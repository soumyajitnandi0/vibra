package com.example.classcrush.presentation.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.example.classcrush.data.model.GossipModel
import com.example.classcrush.data.model.ChatUtils
import com.example.classcrush.data.model.User
import com.example.classcrush.presentation.component.ProfileImage
import com.example.classcrush.presentation.component.ImageExpander
import com.example.classcrush.presentation.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    matchId: String,
    otherUser: User,
    onNavigateBack: () -> Unit,
    onNavigateToSafety: (User) -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    var messageText by remember { mutableStateOf("") }
    var showDropdownMenu by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Compute stable chatId from current user and otherUser
    val currentUserId = viewModel.getCurrentUser()?.uid ?: ""
    val chatId = remember(currentUserId, otherUser.id) {
        if (currentUserId.isNotEmpty() && otherUser.id.isNotEmpty()) {
            ChatUtils.chatIdOf(currentUserId, otherUser.id)
        } else ""
    }

    // Safety check for otherUser
    if (otherUser.id.isEmpty() || otherUser.name.isEmpty()) {
        LaunchedEffect(Unit) {
            // Navigate back if user data is invalid
            onNavigateBack()
        }
        return
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.sendImageMessage(chatId, it) }
    }

    LaunchedEffect(chatId) {
        if (chatId.isNotEmpty()) {
            viewModel.loadMessages(chatId)
            viewModel.markMessagesAsRead(chatId)
        }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileImage(
                        publicId = otherUser.profileImagePublicId.ifEmpty { "default" },
                        contentDescription = "Profile photo",
                        size = 40.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = otherUser.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = try {
                                if (otherUser.isOnline) "Online" else "Last seen ${formatLastSeen(otherUser.lastSeen)}"
                            } catch (e: Exception) {
                                "Last seen recently"
                            },
                            fontSize = 12.sp,
                            color = if (otherUser.isOnline)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                Box {
                    IconButton(onClick = { showDropdownMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }

                    DropdownMenu(
                        expanded = showDropdownMenu,
                        onDismissRequest = { showDropdownMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("View Profile") },
                            onClick = {
                                showDropdownMenu = false
                                // TODO: Navigate to profile view
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Safety & Privacy") },
                            onClick = {
                                showDropdownMenu = false
                                onNavigateToSafety(otherUser)
                            }
                        )
                    }
                }
            }
        )

        // Messages List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.messages) { message ->
                MessageItem(
                    message = message,
                    isFromCurrentUser = message.senderId == currentUserId,
                    otherUserImage = otherUser.profileImagePublicId.ifEmpty { "default" },
                    otherUser = otherUser
                )
            }

            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        // Message Input
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                IconButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    enabled = !uiState.isSending
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = "Send image",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    maxLines = 4,
                    enabled = !uiState.isSending
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(chatId, messageText.trim())
                            messageText = ""
                        }
                    },
                    enabled = messageText.isNotBlank() && !uiState.isSending
                ) {
                    if (uiState.isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send message",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Error handling
        uiState.error?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun MessageItem(
    message: GossipModel,
    isFromCurrentUser: Boolean,
    otherUserImage: String,
    otherUser: User? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isFromCurrentUser) {
            var showProfileDetails by remember { mutableStateOf(false) }
            
            ProfileImage(
                publicId = otherUserImage,
                contentDescription = "Sender photo",
                size = 32.dp,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clickable { 
                        if (otherUser != null) {
                            showProfileDetails = true
                        }
                    }
            )
            
            // Profile Details Dialog
            if (showProfileDetails && otherUser != null) {
                ProfileDetailDialog(
                    user = otherUser,
                    onDismiss = { showProfileDetails = false }
                )
            }
        }

        Column(
            horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isFromCurrentUser)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isFromCurrentUser) 16.dp else 4.dp,
                    bottomEnd = if (isFromCurrentUser) 4.dp else 16.dp
                )
            ) {
                when {
                    message.message != null -> {
                        Text(
                            text = message.message,
                            modifier = Modifier.padding(12.dp),
                            color = if (isFromCurrentUser)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp
                        )
                    }
                    message.imageUrl != null -> {
                        Column(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            ImageExpander(
                                imageUrl = message.imageUrl!!,
                                contentDescription = "Sent image",
                                modifier = Modifier.size(200.dp),
                                thumbnailModifier = Modifier
                                    .size(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }
            }

            Text(
                text = formatMessageTime(message.timestamp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        if (isFromCurrentUser) {
            Spacer(modifier = Modifier.width(40.dp))
        }
    }
}

private fun formatMessageTime(timestamp: Long): String {
    try {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 0 -> "now" // Handle negative timestamps
            diff < 60_000 -> "now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
            diff < 604800_000 -> {
                val sdf = SimpleDateFormat("EEE HH:mm", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
            else -> {
                val sdf = SimpleDateFormat("MMM dd HH:mm", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    } catch (e: Exception) {
        return "now"
    }
}

private fun formatLastSeen(timestamp: Long): String {
    try {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 0 -> "recently" // Handle negative timestamps
            diff < 60_000 -> "just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            diff < 604800_000 -> "${diff / 86400_000}d ago"
            else -> {
                val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    } catch (e: Exception) {
        return "recently"
    }
}

@Composable
fun ProfileDetailDialog(
    user: User,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Profile Details",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Profile Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (user.profileImagePublicId.isNotEmpty()) {
                        ImageExpander(
                            imageUrl = user.profileImageUrl.ifEmpty { 
                                "https://res.cloudinary.com/doihs4i87/image/upload/w_400,h_400,c_fill,g_face,q_auto:good,f_auto/${user.profileImagePublicId}" 
                            },
                            contentDescription = "Profile photo",
                            modifier = Modifier.fillMaxSize(),
                            thumbnailModifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Card(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Profile photo placeholder",
                                    modifier = Modifier.size(60.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // User Info
                Text(
                    text = "${user.name}, ${user.age}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "${user.department} • ${user.year}",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = user.college,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Bio
                if (user.bio.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = user.bio,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
