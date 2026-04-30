package com.example.nam.ui.media.mediapicker.repository

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.nam.ui.media.mediapicker.MediaItem

class MediaStoreRepository(private val context: Context) : MediaRepository {
    override suspend fun getMedia(includeVideos: Boolean): List<MediaItem> {
        val mediaItems = mutableListOf<MediaItem>()

        try {
            if (includeVideos) {
                queryAllMedia(mediaItems)
            } else {
                queryImagesOnly(mediaItems)
            }
        } catch (e: Exception) {
            android.util.Log.e("MediaRepository", "Error querying media: ${e.message}", e)
        }

        return mediaItems
    }

    private fun queryAllMedia(mediaItems: MutableList<MediaItem>) {
        queryMediaFiles("external", mediaItems)
        queryMediaFiles("internal", mediaItems)
    }

    private fun queryMediaFiles(volumeName: String, mediaItems: MutableList<MediaItem>) {
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.MEDIA_TYPE
        )
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)"
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        context.contentResolver.query(
            MediaStore.Files.getContentUri(volumeName),
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val mimeTypeColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val mediaTypeColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

            while (cursor.moveToNext()) {
                try {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Unknown"
                    val mimeType = cursor.getString(mimeTypeColumn) ?: "Unknown"
                    val mediaType = cursor.getInt(mediaTypeColumn)
                    val isVideo = mimeType.startsWith("video/") ||
                            name.lowercase().run {
                                endsWith(".mp4") || endsWith(".avi") || endsWith(".mkv") ||
                                        endsWith(".mov") || endsWith(".3gp") || endsWith(".webm")
                            }
                    val uri = when (mediaType) {
                        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE ->
                            Uri.withAppendedPath(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id.toString()
                            )

                        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO ->
                            Uri.withAppendedPath(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                id.toString()
                            )

                        else -> continue
                    }
                    mediaItems.add(MediaItem(uri, name, isVideo))
                } catch (e: Exception) {
                    android.util.Log.e(
                        "MediaRepository",
                        "Error processing media item: ${e.message}",
                        e
                    )
                }
            }
        }
    }

    private fun queryImagesOnly(mediaItems: MutableList<MediaItem>) {
        queryImages("external", mediaItems)
        queryImages("internal", mediaItems)
    }

    private fun queryImages(volumeName: String, mediaItems: MutableList<MediaItem>) {
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME
        )
        val imageSelection = "${MediaStore.Images.Media.MIME_TYPE} IN (?, ?)"
        val imageSelectionArgs = arrayOf("image/jpeg", "image/png")
        val imageSortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        val contentUri = if (volumeName == "external")
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        else
            MediaStore.Images.Media.INTERNAL_CONTENT_URI

        context.contentResolver.query(
            contentUri,
            imageProjection,
            imageSelection,
            imageSelectionArgs,
            imageSortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "Unknown"
                val uri = Uri.withAppendedPath(contentUri, id.toString())
                mediaItems.add(MediaItem(uri, name, false))
            }
        }
    }
}