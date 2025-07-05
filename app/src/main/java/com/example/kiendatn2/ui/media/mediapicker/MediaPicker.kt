package com.example.kiendatn2.ui.media.mediapicker

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.example.kiendatn2.R
import com.example.kiendatn2.ui.media.mediapicker.viewmodel.MediaPickerUiState
import com.example.kiendatn2.ui.media.mediapicker.viewmodel.MediaPickerViewModel
import com.example.kiendatn2.ui.media.mediapicker.viewmodel.MediaPickerViewModelFactory
import com.example.kiendatn2.ui.theme.LocalCustomColors

@Composable
fun MediaPicker(
    onMediaSelected: (Uri, Boolean) -> Unit,
    onDismiss: () -> Unit,
    hasPermission: Boolean,
    requestPermission: () -> Unit,
    includeVideos: Boolean = false
) {
    val context = LocalContext.current
    val viewModel: MediaPickerViewModel = viewModel(
        factory = MediaPickerViewModelFactory(context, hasPermission, includeVideos)
    )
    val uiState by viewModel.uiState.collectAsState()

    MediaPickerContent(
        uiState = uiState,
        onMediaSelected = onMediaSelected,
        onDismiss = onDismiss,
        requestPermission = requestPermission,
        includeVideos = includeVideos
    )
}

@Composable
private fun MediaPickerContent(
    uiState: MediaPickerUiState,
    onMediaSelected: (Uri, Boolean) -> Unit,
    onDismiss: () -> Unit,
    requestPermission: () -> Unit,
    includeVideos: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LocalCustomColors.current.navBarsBackground)
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(if (includeVideos) R.string.select_media else R.string.select_image),
            style = MaterialTheme.typography.headlineSmall,
            color = LocalCustomColors.current.textPrimary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when (uiState) {
            is MediaPickerUiState.Loading -> MediaLoadingView()
            is MediaPickerUiState.Success -> MediaGridView(
                mediaItems = uiState.mediaItems,
                onMediaSelected = { uri, isVideo ->
                    onMediaSelected(uri, isVideo)
                    onDismiss()
                }
            )

            is MediaPickerUiState.Error -> PermissionErrorView(requestPermission)
        }
    }
}

@Composable
private fun MediaLoadingView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun MediaGridView(
    mediaItems: List<MediaItem>,
    onMediaSelected: (Uri, Boolean) -> Unit
) {
    if (mediaItems.isEmpty()) {
        EmptyMediaView()
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(400.dp)
        ) {
            items(mediaItems) { mediaItem ->
                MediaItemView(mediaItem, onMediaSelected)
            }
        }
    }
}

@Composable
private fun EmptyMediaView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.no_media_found),
            color = LocalCustomColors.current.textPrimary
        )
    }
}

@Composable
private fun MediaItemView(
    mediaItem: MediaItem,
    onMediaSelected: (Uri, Boolean) -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .clickable {
                onMediaSelected(mediaItem.uri, mediaItem.isVideo)
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(mediaItem.uri)
                .apply {
                    if (mediaItem.isVideo) {
                        videoFrameMillis(1000)
                    }
                }
                .crossfade(true)
                .build(),
            contentDescription = "Media thumbnail: ${mediaItem.displayName}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (mediaItem.isVideo) {
            VideoOverlay()
        }
    }
}

@Composable
private fun VideoOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Video",
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun PermissionErrorView(requestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.permission_required),
                color = LocalCustomColors.current.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = requestPermission) {
                Text(text = stringResource(R.string.grant_permission))
            }
        }
    }
}