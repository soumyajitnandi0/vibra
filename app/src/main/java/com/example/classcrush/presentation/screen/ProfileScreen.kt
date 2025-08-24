package com.example.classcrush.presentation.screen

import androidx.compose.foundation.Image
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.example.classcrush.presentation.component.ProfileImage
import com.example.classcrush.presentation.component.ImageExpander
import com.example.classcrush.presentation.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToLogin: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.updateProfileImage(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    // Handle logout navigation
    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            onNavigateToLogin()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Profile",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                IconButton(onClick = { /* TODO: Navigate to settings */ }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
                IconButton(onClick = { showLogoutDialog = true }) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                }
            }
        )

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.user != null -> {
                val user = uiState.user!!

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Image
                    Box {
                        if (user.profileImagePublicId.isNotEmpty()) {
                            ImageExpander(
                                imageUrl = user.profileImageUrl.ifEmpty { 
                                    "https://res.cloudinary.com/doihs4i87/image/upload/w_400,h_400,c_fill,g_face,q_auto:good,f_auto/${user.profileImagePublicId}" 
                                },
                                contentDescription = "Profile photo",
                                modifier = Modifier.size(150.dp),
                                thumbnailModifier = Modifier
                                    .size(150.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            ProfileImage(
                                publicId = user.profileImagePublicId,
                                contentDescription = "Profile photo",
                                size = 150.dp
                            )
                        }

                        FloatingActionButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(40.dp),
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit photo",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // User Info
                    Text(
                        text = "${user.name}, ${user.age}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${user.department} • ${user.year}",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Text(
                        text = user.college,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    var showEditBio by remember { mutableStateOf(false) }

                    // Bio
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "About Me",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                IconButton(onClick = { showEditBio = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit bio")
                                }
                            }

                            Text(
                                text = user.bio.ifEmpty { "Tell people about yourself..." },
                                fontSize = 14.sp,
                                color = if (user.bio.isEmpty())
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    if (showEditBio) {
                        EditBioDialog(
                            initialBio = user.bio,
                            isSaving = uiState.isUpdating,
                            onDismiss = { showEditBio = false },
                            onSave = { newBio ->
                                viewModel.updateBio(newBio)
                                showEditBio = false
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stats Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(
                                label = "Matches",
                                value = user.matches.size.toString()
                            )

                            StatItem(
                                label = "Likes Given",
                                value = user.likedUsers.size.toString()
                            )

                            StatItem(
                                label = "Member Since",
                                value = formatMemberSince(user.createdAt)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Edit Profile Button
                    var showEditProfile by remember { mutableStateOf(false) }
                    Button(
                        onClick = { showEditProfile = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Edit Profile")
                    }

                    if (uiState.isUpdatingImage) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Updating photo...")
                        }
                    }
                    if (showEditProfile) {
                        EditProfileDialog(
                            user = user,
                            isSaving = uiState.isUpdating,
                            onDismiss = { showEditProfile = false },
                            onSave = { updated ->
                                viewModel.saveProfile(updated)
                                showEditProfile = false
                            }
                        )
                    }

                }
            }
        }

        // Logout Confirmation Dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Logout") },
                text = { Text("Are you sure you want to logout?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.logout()
                            showLogoutDialog = false
                        }
                    ) {
                        Text("Logout")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        uiState.error?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatMemberSince(timestamp: Long): String {
    val calendar = java.util.Calendar.getInstance()
    calendar.timeInMillis = timestamp
    val year = calendar.get(java.util.Calendar.YEAR)
    return year.toString()
}

@Composable
fun EditBioDialog(
    initialBio: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var bio by remember { mutableStateOf(initialBio) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Bio") },
        text = {
            Column {
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    placeholder = { Text("Tell people about yourself...") },
                    minLines = 4,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(bio) }, enabled = !isSaving) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isSaving) "Saving..." else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditProfileDialog(
    user: com.example.classcrush.data.model.User,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (com.example.classcrush.data.model.User) -> Unit
) {
    var name by remember { mutableStateOf(user.name) }
    var ageText by remember { mutableStateOf(user.age.toString()) }
    var college by remember { mutableStateOf(user.college) }
    var department by remember { mutableStateOf(user.department) }
    var year by remember { mutableStateOf(user.year) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ageText,
                    onValueChange = { value -> if (value.all { it.isDigit() }) ageText = value },
                    label = { Text("Age") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = college,
                    onValueChange = { college = it },
                    label = { Text("College") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Department") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text("Year") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val safeAge = ageText.toIntOrNull() ?: user.age
                    onSave(
                        user.copy(
                            name = name.trim(),
                            age = safeAge,
                            college = college.trim(),
                            department = department.trim(),
                            year = year.trim()
                        )
                    )
                },
                enabled = !isSaving && name.isNotBlank() && ageText.isNotBlank()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isSaving) "Saving..." else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancel") }
        }
    )
}
