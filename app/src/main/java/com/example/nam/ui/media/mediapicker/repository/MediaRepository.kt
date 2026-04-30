package com.example.nam.ui.media.mediapicker.repository

import com.example.nam.ui.media.mediapicker.MediaItem

interface MediaRepository {
    suspend fun getMedia(includeVideos: Boolean): List<MediaItem>
}