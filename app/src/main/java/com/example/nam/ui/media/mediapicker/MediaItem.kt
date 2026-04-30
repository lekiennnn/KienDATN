package com.example.nam.ui.media.mediapicker

import android.net.Uri

data class MediaItem(
    val uri: Uri,
    val displayName: String,
    val isVideo: Boolean
)