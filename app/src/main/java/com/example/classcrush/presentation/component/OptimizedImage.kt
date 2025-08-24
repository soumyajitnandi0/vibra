package com.example.classcrush.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import javax.inject.Inject

@Composable
fun OptimizedCloudinaryImage(
    publicId: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    width: Int = 400,
    height: Int = 400,
    shape: Shape? = null,
    contentScale: ContentScale = ContentScale.Crop,
    showLoading: Boolean = true,
    fallbackUrl: String = "https://via.placeholder.com/${width}x${height}/E91E63/FFFFFF?text=Profile"
) {
    val context = LocalContext.current

    // Generate optimized URL based on publicId using your actual Cloudinary cloud
    val imageUrl = remember(publicId, width, height) {
        if (publicId.isNotEmpty() && publicId != "placeholder") {
            // Use your actual Cloudinary cloud name
            "https://res.cloudinary.com/doihs4i87/image/upload/w_$width,h_$height,c_fill,g_face,q_auto:good,f_auto/$publicId"
        } else {
            fallbackUrl
        }
    }

    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .build()
    )

    Box(modifier = modifier) {
        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = if (shape != null) {
                Modifier.fillMaxSize().clip(shape)
            } else {
                Modifier.fillMaxSize()
            },
            contentScale = contentScale
        )

        // Show loading indicator
        if (showLoading && painter.state is AsyncImagePainter.State.Loading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Show error state
        if (painter.state is AsyncImagePainter.State.Error) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Profile placeholder",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ProfileImage(
    publicId: String,
    contentDescription: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    showLoading: Boolean = true
) {
    OptimizedCloudinaryImage(
        publicId = publicId,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        width = size.value.toInt(),
        height = size.value.toInt(),
        shape = CircleShape,
        showLoading = showLoading
    )
}

@Composable
fun CardImage(
    publicId: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    showLoading: Boolean = true
) {
    OptimizedCloudinaryImage(
        publicId = publicId,
        contentDescription = contentDescription,
        modifier = modifier,
        width = 800,
        height = 800,
        showLoading = showLoading
    )
}
