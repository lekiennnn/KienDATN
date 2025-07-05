package com.example.kiendatn2

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.kiendatn2.service.CloudinaryManager
import com.example.kiendatn2.utils.LocaleManager
import com.example.kiendatn2.utils.SharedPreferenceManager
import com.google.firebase.FirebaseApp

class MyApplication : Application() {

    companion object {
        private const val TAG = "MyApplication"

        private const val CLOUDINARY_CLOUD_NAME = "dlkldrhng"
        private const val CLOUDINARY_UPLOAD_PRESET = "kiendatn2"
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleManager.applyLocaleForApplication(base))
    }

    override fun onCreate() {
        super.onCreate()

        SharedPreferenceManager.getInstance(this)

        initializeFirebase()

        initializeCloudinary()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = getString(R.string.default_notification_channel_id)
            val channelName = getString(R.string.default_notification_channel_name)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Handles all app notifications"
            }
            
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initializeFirebase() {
        try {
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "Firebase initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization failed", e)
        }
    }

    private fun initializeCloudinary() {
        try {
            val isInitialized = CloudinaryManager.initialize(
                cloudNameValue = CLOUDINARY_CLOUD_NAME,
                uploadPresetValue = CLOUDINARY_UPLOAD_PRESET
            )

            if (isInitialized) {
                Log.d(TAG, "Cloudinary initialized successfully")
            } else {
                Log.e(TAG, "Cloudinary initialization failed: Invalid configuration")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cloudinary initialization failed: ${e.message}", e)
        }
    }
}