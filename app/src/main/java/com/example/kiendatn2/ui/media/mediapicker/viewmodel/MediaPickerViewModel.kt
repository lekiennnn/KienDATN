package com.example.kiendatn2.ui.media.mediapicker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kiendatn2.ui.media.mediapicker.MediaItem
import com.example.kiendatn2.ui.media.mediapicker.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MediaPickerViewModel(
    private val mediaRepository: MediaRepository,
    private val hasPermission: Boolean,
    private val includeVideos: Boolean
) : ViewModel() {

    private val _uiState = MutableStateFlow<MediaPickerUiState>(MediaPickerUiState.Loading)
    val uiState: StateFlow<MediaPickerUiState> = _uiState

    init {
        loadMedia()
    }

    fun loadMedia() {
        if (!hasPermission) {
            _uiState.value = MediaPickerUiState.Error
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mediaItems = mediaRepository.getMedia(includeVideos)
                _uiState.value = MediaPickerUiState.Success(mediaItems)
            } catch (e: Exception) {
                android.util.Log.e("MediaPickerViewModel", "Error loading media: ${e.message}", e)
                _uiState.value = MediaPickerUiState.Error
            }
        }
    }
}

sealed class MediaPickerUiState {
    object Loading : MediaPickerUiState()
    data class Success(val mediaItems: List<MediaItem>) : MediaPickerUiState()
    object Error : MediaPickerUiState()
}