package com.example.kiendatn2.ui.media.mediapicker

import android.net.Uri

data class MediaItem(
    val uri: Uri,
    val displayName: String,
    val isVideo: Boolean
)