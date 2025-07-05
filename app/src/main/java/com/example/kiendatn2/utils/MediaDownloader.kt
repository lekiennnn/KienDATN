package com.example.kiendatn2.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


object MediaDownloader {

    suspend fun downloadImage(context: Context, url: String, fileName: String? = null): Long =
        withContext(Dispatchers.IO) {
            try {
                val downloadManager =
                    context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

                val uri = Uri.parse(url)
                val request = DownloadManager.Request(uri)

                val actualFileName = fileName ?: getFileNameFromUrl(url)

                request.setDescription("Downloading image")
                request.setTitle(actualFileName)

                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

                request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    actualFileName
                )

                val mimeType = getMimeType(url) ?: "image/*"
                request.setMimeType(mimeType)

                val downloadId = downloadManager.enqueue(request)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Downloading image...", Toast.LENGTH_SHORT).show()
                }

                return@withContext downloadId
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
                return@withContext -1L
            }
        }

    private fun getFileNameFromUrl(url: String): String {
        val lastPathSegment = url.substringAfterLast('/')
        return if (lastPathSegment.contains(".")) {
            lastPathSegment
        } else {
            "downloaded_image_${System.currentTimeMillis()}.jpg"
        }
    }

    private fun getMimeType(url: String): String? {
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        return if (extension != null) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        } else {
            null
        }
    }
}