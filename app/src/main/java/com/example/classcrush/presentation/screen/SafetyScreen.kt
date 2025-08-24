package com.example.classcrush.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Warning
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
import com.example.classcrush.presentation.viewmodel.SafetyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyScreen(
    user: User,
    onNavigateBack: () -> Unit,
    viewModel: SafetyViewModel = hiltViewModel()
) {
    var showReportDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showUnmatchDialog by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Safety & Privacy",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // User Info
            ProfileImage(
                publicId = user.profileImagePublicId,
                contentDescription = "User photo",
                size = 80.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = user.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${user.department} • ${user.year}",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Safety Actions
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Safety Actions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Report User
                    OutlinedButton(
                        onClick = { showReportDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Report,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Report ${user.name}")
                    }

                    // Block User
                    OutlinedButton(
                        onClick = { showBlockDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Block,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Block ${user.name}")
                    }

                    // Unmatch
                    OutlinedButton(
                        onClick = { showUnmatchDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Unmatch")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Safety Tips
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🛡️ Safety Tips",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val safetyTips = listOf(
                        "Meet in public places for first dates",
                        "Tell a friend about your plans",
                        "Trust your instincts",
                        "Don't share personal information too quickly",
                        "Report any inappropriate behavior"
                    )

                    safetyTips.forEach { tip ->
                        Text(
                            text = "• $tip",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }

    // Report Dialog
    if (showReportDialog) {
        ReportUserDialog(
            userName = user.name,
            onDismiss = { showReportDialog = false },
            onReport = { reason ->
                viewModel.reportUser(user.id, reason)
                showReportDialog = false
            }
        )
    }

    // Block Dialog
    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Block ${user.name}?") },
            text = {
                Text("They won't be able to see your profile or contact you. This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.blockUser(user.id)
                        showBlockDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Block")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Unmatch Dialog
    if (showUnmatchDialog) {
        AlertDialog(
            onDismissRequest = { showUnmatchDialog = false },
            title = { Text("Unmatch with ${user.name}?") },
            text = {
                Text("You will no longer see each other's profiles and your conversation will be deleted.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.unmatchUser(user.id)
                        showUnmatchDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Unmatch")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnmatchDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Show loading or error states
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar or handle error
        }
    }
}

@Composable
fun ReportUserDialog(
    userName: String,
    onDismiss: () -> Unit,
    onReport: (String) -> Unit
) {
    var selectedReason by remember { mutableStateOf("") }

    val reportReasons = listOf(
        "Inappropriate photos",
        "Harassment or bullying",
        "Spam or fake profile",
        "Inappropriate messages",
        "Underage user",
        "Other"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report $userName") },
        text = {
            Column {
                Text(
                    text = "Why are you reporting this user?",
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                reportReasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Text(
                            text = reason,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onReport(selectedReason) },
                enabled = selectedReason.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
