package com.example.classcrush.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.classcrush.data.model.User
import com.example.classcrush.presentation.component.ProfileImage
import com.example.classcrush.presentation.viewmodel.MatchesViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesScreen(
    onNavigateToChat: (String, User) -> Unit,
    viewModel: MatchesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadMatches()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Matches") })

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.error!!, color = MaterialTheme.colorScheme.error)
                }
            }
            uiState.matches.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No matches yet. Keep swiping!")
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(uiState.matches) { matchItem ->
                        if (matchItem.otherUser != null && matchItem.match != null) {
                            MatchRow(
                                matchItem = matchItem,
                                onNavigateToChat = { onNavigateToChat(matchItem.match.id, matchItem.otherUser) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatchRow(
    matchItem: com.example.classcrush.presentation.viewmodel.MatchItem,
    onNavigateToChat: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clickable { onNavigateToChat() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileImage(
                publicId = matchItem.otherUser.profileImagePublicId,
                size = 56.dp,
                // Add this line to fix the error
                contentDescription = "Profile picture of ${matchItem.otherUser.name}"
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = matchItem.otherUser.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = matchItem.match.lastMessage.ifEmpty { "Say hi!" },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatLastMessageTime(matchItem.match.lastMessageTime),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatLastMessageTime(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86400_000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}