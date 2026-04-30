package com.example.nam.ui.media.mediapicker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.nam.ui.media.mediapicker.repository.MediaStoreRepository


class MediaPickerViewModelFactory(
    private val context: Context,
    private val hasPermission: Boolean,
    private val includeVideos: Boolean
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediaPickerViewModel::class.java)) {
            return MediaPickerViewModel(
                MediaStoreRepository(context),
                hasPermission,
                includeVideos
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}