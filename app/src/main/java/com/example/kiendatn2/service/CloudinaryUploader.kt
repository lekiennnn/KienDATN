package com.example.kiendatn2.service

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

sealed class MediaUploadState {
    object Idle : MediaUploadState()
    object Uploading : MediaUploadState()
    data class Success(val mediaUrl: String) : MediaUploadState()
    data class Error(val message: String) : MediaUploadState()
}

sealed class ImageUploadState {
    object Idle : ImageUploadState()
    object Uploading : ImageUploadState()
    data class Success(val imageUrl: String) : ImageUploadState()
    data class Error(val message: String) : ImageUploadState()
}

class CloudinaryUploader {

    private val cloudinaryManager = CloudinaryManager()

    private val _uploadState = MutableStateFlow<MediaUploadState>(MediaUploadState.Idle)
    val uploadState: StateFlow<MediaUploadState> = _uploadState

    private val _imageUploadState = MutableStateFlow<ImageUploadState>(ImageUploadState.Idle)
    val imageUploadState: StateFlow<ImageUploadState> = _imageUploadState

    suspend fun uploadImage(context: Context, imageUri: Uri?): MediaUploadState {
        if (imageUri == null) {
            _uploadState.value = MediaUploadState.Idle
            return MediaUploadState.Idle
        }

        _uploadState.value = MediaUploadState.Uploading

        try {
            val imageUrl = withContext(Dispatchers.IO) {
                cloudinaryManager.uploadFromUri(context, imageUri, isVideo = false)
            }
            val successState = MediaUploadState.Success(imageUrl)
            _uploadState.value = successState
            return successState
        } catch (e: Exception) {
            Log.e("CloudinaryUploader", "Image upload failed: ${e.message}", e)
            val errorState = MediaUploadState.Error(e.message ?: "Unknown error")
            _uploadState.value = errorState
            return errorState
        }
    }

    suspend fun uploadVideo(context: Context, videoUri: Uri?): MediaUploadState {
        if (videoUri == null) {
            _uploadState.value = MediaUploadState.Idle
            return MediaUploadState.Idle
        }

        _uploadState.value = MediaUploadState.Uploading

        try {
            val videoUrl = withContext(Dispatchers.IO) {
                cloudinaryManager.uploadFromUri(context, videoUri, isVideo = true)
            }
            val successState = MediaUploadState.Success(videoUrl)
            _uploadState.value = successState
            return successState
        } catch (e: Exception) {
            Log.e("CloudinaryUploader", "Video upload failed: ${e.message}", e)
            val errorState = MediaUploadState.Error(e.message ?: "Unknown error")
            _uploadState.value = errorState
            return errorState
        }
    }

    fun resetState() {
        _uploadState.value = MediaUploadState.Idle
    }

    suspend fun uploadMedia(
        context: Context,
        mediaUri: Uri?,
        isVideo: Boolean = false
    ): MediaUploadState {
        return if (isVideo) {
            uploadVideo(context, mediaUri)
        } else {
            uploadImage(context, mediaUri)
        }
    }

    suspend fun uploadProfileImage(context: Context, imageUri: Uri?): ImageUploadState {
        if (imageUri == null) {
            _imageUploadState.value = ImageUploadState.Idle
            return ImageUploadState.Idle
        }

        _imageUploadState.value = ImageUploadState.Uploading

        try {
            val imageUrl = withContext(Dispatchers.IO) {
                cloudinaryManager.uploadFromUri(context, imageUri, isVideo = false)
            }
            val successState = ImageUploadState.Success(imageUrl)
            _imageUploadState.value = successState
            return successState
        } catch (e: Exception) {
            Log.e("CloudinaryUploader", "Profile image upload failed: ${e.message}", e)
            val errorState = ImageUploadState.Error(e.message ?: "Unknown error")
            _imageUploadState.value = errorState
            return errorState
        }
    }

    fun resetAllStates() {
        _uploadState.value = MediaUploadState.Idle
        _imageUploadState.value = ImageUploadState.Idle
    }

    fun resetImageState() {
        _imageUploadState.value = ImageUploadState.Idle
    }
}