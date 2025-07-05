package com.example.kiendatn2.service

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody

class CloudinaryManager {

    companion object {
        private const val CLOUDINARY_IMAGE_URL = "https://api.cloudinary.com/v1_1/%s/image/upload"
        private const val CLOUDINARY_VIDEO_URL = "https://api.cloudinary.com/v1_1/%s/video/upload"

        private var cloudName: String = ""
        private var uploadPreset: String = ""
        private var isInitialized = false

        fun isInitialized(): Boolean {
            return isInitialized && cloudName.isNotBlank() && uploadPreset.isNotBlank()
        }

        fun initialize(cloudNameValue: String, uploadPresetValue: String): Boolean {
            if (cloudNameValue.isBlank() || uploadPresetValue.isBlank()) {
                Log.e("CloudinaryManager", "CloudinaryManager initialization failed: empty credentials")
                isInitialized = false
                return false
            }

            cloudName = cloudNameValue
            uploadPreset = uploadPresetValue
            isInitialized = true
            return true
        }
    }

    private val client = OkHttpClient()

    suspend fun uploadFile(file: File, isVideo: Boolean = false): String =
        withContext(Dispatchers.IO) {
            if (!isInitialized()) {
                throw IOException("CloudinaryManager not initialized properly. Make sure to call initialize() with valid credentials.")
            }

            try {
                val mediaType = if (isVideo) "video/*" else "image/*"
                val uploadUrl = if (isVideo) {
                    String.format(CLOUDINARY_VIDEO_URL, cloudName)
                } else {
                    String.format(CLOUDINARY_IMAGE_URL, cloudName)
                }

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file",
                        file.name,
                        file.asRequestBody(mediaType.toMediaType())
                    )
                    .addFormDataPart("upload_preset", uploadPreset)
                    .addFormDataPart("folder", "app_uploads")
                    .build()

                val request = Request.Builder()
                    .url(uploadUrl)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("Unexpected response code: ${response.code}")
                }

                val responseBody =
                    response.body?.string() ?: throw IOException("Empty response body")
                val json = JSONObject(responseBody)
                val secureUrl = json.getString("secure_url")

                return@withContext secureUrl

            } catch (e: Exception) {
                Log.e("CloudinaryManager", "Upload failed: ${e.message}", e)
                throw IOException("Media upload failed: ${e.message}", e)
            }
        }

    suspend fun uploadFromUri(context: Context, uri: Uri, isVideo: Boolean = false): String =
        withContext(Dispatchers.IO) {
            if (!isInitialized()) {
                throw IOException("CloudinaryManager not initialized properly. Make sure to call initialize() with valid credentials.")
            }

            var tempFile: File? = null
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IOException("Failed to open input stream for URI: $uri")

                val extension = if (isVideo) ".mp4" else ".jpg"
                tempFile = File.createTempFile("upload_", extension, context.cacheDir)

                inputStream.use { stream ->
                    FileOutputStream(tempFile).use { output ->
                        stream.copyTo(output)
                    }
                }

                return@withContext uploadFile(tempFile, isVideo)
            } catch (e: Exception) {
                Log.e("CloudinaryManager", "Upload from URI failed: ${e.message}", e)
                throw IOException("Media upload failed: ${e.message}", e)
            } finally {
                try {
                    tempFile?.let {
                        if (it.exists()) {
                            it.delete()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CloudinaryManager", "Error deleting temporary file", e)
                }
            }
        }
}