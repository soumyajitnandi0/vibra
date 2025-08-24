package com.example.classcrush.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

@Composable
fun ImageExpander(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    thumbnailModifier: Modifier = Modifier
) {
    var showExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.clickable { showExpanded = true }
    ) {
        // Thumbnail image
        AsyncImage(
            imageUrl = imageUrl,
            contentDescription = contentDescription,
            modifier = thumbnailModifier,
            contentScale = ContentScale.Crop
        )

        // Expanded dialog
        if (showExpanded) {
            Dialog(
                onDismissRequest = { showExpanded = false },
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                    usePlatformDefaultWidth = false
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.9f))
                        .clickable { showExpanded = false }
                ) {
                    // Close button
                    IconButton(
                        onClick = { showExpanded = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Full-size image
                    AsyncImage(
                        imageUrl = imageUrl,
                        contentDescription = contentDescription,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
private fun AsyncImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .build()
    )

    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}


