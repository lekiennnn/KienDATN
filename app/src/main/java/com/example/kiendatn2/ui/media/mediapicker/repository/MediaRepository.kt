package com.example.kiendatn2.ui.media.mediapicker.repository

import com.example.kiendatn2.ui.media.mediapicker.MediaItem

interface MediaRepository {
    suspend fun getMedia(includeVideos: Boolean): List<MediaItem>
}