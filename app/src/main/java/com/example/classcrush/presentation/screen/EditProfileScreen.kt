package com.example.classcrush.presentation.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.classcrush.data.model.CloudinaryImage
import com.example.classcrush.presentation.component.OptimizedCloudinaryImage
import com.example.classcrush.presentation.component.ProfileImage
import com.example.classcrush.presentation.viewmodel.ImageUploadViewModel
import com.example.classcrush.presentation.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    profileViewModel: ProfileViewModel = hiltViewModel(),
    imageUploadViewModel: ImageUploadViewModel = hiltViewModel()
) {
    val profileUiState by profileViewModel.uiState.collectAsState()
    val imageUploadUiState by imageUploadViewModel.uiState.collectAsState()

    val profileImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { imageUploadViewModel.uploadProfileImage(it) }
    }

    val additionalImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { imageUploadViewModel.uploadAdditionalImage(it) }
    }

    LaunchedEffect(Unit) {
        profileViewModel.loadUserProfile()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Edit Profile",
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

        profileUiState.user?.let { user ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Main Profile Image Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Profile Photo",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Box {
                            ProfileImage(
                                publicId = user.profileImagePublicId,
                                contentDescription = "Profile photo",
                                size = 120.dp,
                                showLoading = imageUploadUiState.isUploading
                            )

                            if (imageUploadUiState.isUploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { profileImageLauncher.launch("image/*") },
                            enabled = !imageUploadUiState.isUploading
                        ) {
                            Text("Change Photo")
                        }

                        if (user.profileImagePublicId.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    imageUploadViewModel.deleteImage(user.profileImagePublicId)
                                },
                                enabled = !imageUploadUiState.isDeleting
                            ) {
                                Text("Remove Photo", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Additional Images Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                                text = "Additional Photos",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "${user.additionalImages.size}/5",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Add new image button
                            if (user.additionalImages.size < 5) {
                                item {
                                    Card(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        onClick = { additionalImageLauncher.launch("image/*") },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Add photo",
                                                modifier = Modifier.size(32.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Existing images
                            items(user.additionalImages) { image ->
                                AdditionalImageItem(
                                    image = image,
                                    onDelete = {
                                        imageUploadViewModel.deleteImage(image.publicId)
                                    },
                                    isDeleting = imageUploadUiState.isDeleting
                                )
                            }
                        }

                        Text(
                            text = "Add up to 5 additional photos to showcase your personality",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Image Quality Tips
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
                            text = "📸 Photo Tips",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val tips = listOf(
                            "Use high-quality, well-lit photos",
                            "Show your face clearly in your main photo",
                            "Include photos that show your interests",
                            "Avoid group photos as your main image",
                            "Keep photos recent and authentic"
                        )

                        tips.forEach { tip ->
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

        // Error handling
        imageUploadUiState.error?.let { error ->
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
fun AdditionalImageItem(
    image: CloudinaryImage,
    onDelete: () -> Unit,
    isDeleting: Boolean
) {
    Box {
        OptimizedCloudinaryImage(
            publicId = image.publicId,
            contentDescription = "Additional photo",
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(8.dp)),
            width = 200,
            height = 200
        )

        IconButton(
            onClick = onDelete,
            enabled = !isDeleting,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete photo",
                tint = MaterialTheme.colorScheme.error
            )
        }

        if (isDeleting) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
