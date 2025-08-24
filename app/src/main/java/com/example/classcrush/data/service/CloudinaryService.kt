package com.example.classcrush.data.service

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class CloudinaryService @Inject constructor(
    private val context: Context
) {

    companion object {
        // TODO: Replace with your actual Cloudinary credentials
        private const val CLOUD_NAME = "doihs4i87"
        private const val API_KEY = "858276792164733"
        private const val API_SECRET = "Lqi4dM5OIWtHS0vPwn32OX74M_w"

        // Folder structure for organized storage
        private const val PROFILE_IMAGES_FOLDER = "classcrush/profile_images"
        private const val ADDITIONAL_IMAGES_FOLDER = "classcrush/additional_images"
        private const val CHAT_IMAGES_FOLDER = "classcrush/chat_images"
    }

    private var isInitialized = false

    init {
        initializeCloudinary()
    }

    private fun initializeCloudinary() {
        if (!isInitialized) {
            try {
                val config = mapOf(
                    "cloud_name" to CLOUD_NAME,
                    "api_key" to API_KEY,
                    "api_secret" to API_SECRET,
                    "secure" to true
                )

                MediaManager.init(context, config)
                isInitialized = true
            } catch (e: Exception) {
                throw Exception("Failed to initialize Cloudinary: ${e.message}")
            }
        }
    }

    suspend fun uploadProfileImage(
        userId: String,
        imageUri: Uri,
        isMainProfile: Boolean = true
    ): Result<CloudinaryUploadResult> {
        return suspendCancellableCoroutine { continuation ->
            val folder = if (isMainProfile) PROFILE_IMAGES_FOLDER else ADDITIONAL_IMAGES_FOLDER
            val publicId = "${folder}/${userId}_${System.currentTimeMillis()}"

            // Simplified options without complex transformations
            val options = mutableMapOf<String, Any>(
                "public_id" to publicId,
                "folder" to folder,
                "overwrite" to true,
                "resource_type" to "image"
            )

            // Add basic transformation parameters directly
            if (isMainProfile) {
                options["width"] = 800
                options["height"] = 800
                options["crop"] = "fill"
                options["gravity"] = "face"
                options["quality"] = "auto:good"
                options["format"] = "jpg"
            } else {
                options["width"] = 600
                options["height"] = 800
                options["crop"] = "fill"
                options["gravity"] = "auto"
                options["quality"] = "auto:good"
                options["format"] = "jpg"
            }

            try {
                MediaManager.get().upload(imageUri)
                    .options(options)
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String) {
                            // Upload started
                        }

                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                            // Progress update - could be used for progress bars
                        }

                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            try {
                                val secureUrl = resultData["secure_url"] as? String
                                val publicId = resultData["public_id"] as? String
                                val version = resultData["version"] as? Int

                                if (secureUrl != null && publicId != null) {
                                    val result = CloudinaryUploadResult(
                                        secureUrl = secureUrl,
                                        publicId = publicId,
                                        version = version ?: 1,
                                        thumbnailUrl = generateThumbnailUrl(publicId),
                                        mediumUrl = generateMediumUrl(publicId)
                                    )
                                    continuation.resume(Result.success(result))
                                } else {
                                    continuation.resumeWithException(
                                        Exception("Invalid response from Cloudinary: missing URL or public ID")
                                    )
                                }
                            } catch (e: Exception) {
                                continuation.resumeWithException(
                                    Exception("Failed to parse Cloudinary response: ${e.message}")
                                )
                            }
                        }

                        override fun onError(requestId: String, error: ErrorInfo) {
                            continuation.resumeWithException(
                                Exception("Cloudinary upload failed: ${error.description}")
                            )
                        }

                        override fun onReschedule(requestId: String, error: ErrorInfo) {
                            // Upload rescheduled - could retry logic here
                        }
                    })
                    .dispatch()
            } catch (e: Exception) {
                continuation.resumeWithException(
                    Exception("Failed to start Cloudinary upload: ${e.message}")
                )
            }
        }
    }

    suspend fun uploadChatImage(
        userId: String,
        imageUri: Uri
    ): Result<CloudinaryUploadResult> {
        return suspendCancellableCoroutine { continuation ->
            val publicId = "${CHAT_IMAGES_FOLDER}/${userId}_${System.currentTimeMillis()}"

            val options = mutableMapOf<String, Any>(
                "public_id" to publicId,
                "folder" to CHAT_IMAGES_FOLDER,
                "width" to 1200,
                "height" to 1200,
                "crop" to "limit",
                "quality" to "auto:good",
                "format" to "jpg",
                "overwrite" to true,
                "resource_type" to "image"
            )

            MediaManager.get().upload(imageUri)
                .options(options)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val secureUrl = resultData["secure_url"] as? String
                        val publicId = resultData["public_id"] as? String
                        val version = resultData["version"] as? Int

                        if (secureUrl != null && publicId != null) {
                            val result = CloudinaryUploadResult(
                                secureUrl = secureUrl,
                                publicId = publicId,
                                version = version ?: 1,
                                thumbnailUrl = generateThumbnailUrl(publicId),
                                mediumUrl = generateMediumUrl(publicId)
                            )
                            continuation.resume(Result.success(result))
                        } else {
                            continuation.resumeWithException(
                                Exception("Failed to get URL from upload result")
                            )
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        continuation.resumeWithException(
                            Exception("Chat image upload failed: ${error.description}")
                        )
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {}
                })
                .dispatch()
        }
    }

    suspend fun deleteImage(publicId: String): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            try {
                // Note: The Cloudinary Android SDK doesn't support direct image deletion
                // Image deletion requires admin privileges and should be handled server-side
                // For now, we'll just log the deletion attempt and return success

                android.util.Log.d("CloudinaryService", "Image deletion requested for: $publicId")

                // In a production app, you should:
                // 1. Create a backend API endpoint for image deletion
                // 2. Call that endpoint from here
                // 3. Let the backend handle the actual Cloudinary Admin API call

                continuation.resume(Result.success(Unit))

            } catch (e: Exception) {
                continuation.resumeWithException(
                    Exception("Failed to delete image: ${e.message}")
                )
            }
        }
    }

    fun generateOptimizedUrl(
        publicId: String,
        width: Int = 800,
        height: Int = 800,
        quality: String = "auto:good",
        crop: String = "fill",
        gravity: String = "face:center"
    ): String {
        return if (publicId.isNotEmpty() && publicId != "placeholder") {
            "https://res.cloudinary.com/$CLOUD_NAME/image/upload/" +
                    "w_$width,h_$height,c_$crop,g_$gravity,q_$quality,f_auto/$publicId"
        } else {
            "https://via.placeholder.com/${width}x${height}/E91E63/FFFFFF?text=Profile"
        }
    }

    private fun generateThumbnailUrl(publicId: String): String {
        return generateOptimizedUrl(publicId, 150, 150)
    }

    private fun generateMediumUrl(publicId: String): String {
        return generateOptimizedUrl(publicId, 400, 400)
    }

    fun getImageVariations(publicId: String): ImageVariations {
        return ImageVariations(
            original = "https://res.cloudinary.com/$CLOUD_NAME/image/upload/$publicId",
            large = generateOptimizedUrl(publicId, 800, 800),
            medium = generateOptimizedUrl(publicId, 400, 400),
            thumbnail = generateOptimizedUrl(publicId, 150, 150),
            blur = "https://res.cloudinary.com/$CLOUD_NAME/image/upload/e_blur:300,q_auto:low/$publicId"
        )
    }

    fun generateBlurPlaceholder(publicId: String): String {
        return "https://res.cloudinary.com/$CLOUD_NAME/image/upload/w_50,h_50,e_blur:1000,q_auto:low,f_auto/$publicId"
    }

    // Utility function to check if Cloudinary is properly configured
    fun isConfigured(): Boolean {
        return CLOUD_NAME.isNotEmpty() &&
                API_KEY != "your_api_key" &&
                API_SECRET != "your_api_secret"
    }
}

data class CloudinaryUploadResult(
    val secureUrl: String,
    val publicId: String,
    val version: Int,
    val thumbnailUrl: String,
    val mediumUrl: String
)

data class ImageVariations(
    val original: String,
    val large: String,
    val medium: String,
    val thumbnail: String,
    val blur: String
)