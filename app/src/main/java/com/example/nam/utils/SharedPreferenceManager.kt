package com.example.nam.utils

import android.content.Context
import android.content.SharedPreferences

class SharedPreferenceManager(context: Context) {

    companion object {
        const val PREF_NAME = "com.example.nam.preferences"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        const val KEY_LANGUAGE = "selected_language"
        const val DEFAULT_LANGUAGE = "English"

        @Volatile
        private var INSTANCE: SharedPreferenceManager? = null

        fun getInstance(context: Context): SharedPreferenceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SharedPreferenceManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun setLoggedIn(isLoggedIn: Boolean) {
        preferences.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }

    fun isLoggedIn(): Boolean {
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun setUserId(userId: String?) {
        preferences.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): String? {
        return preferences.getString(KEY_USER_ID, null)
    }

    fun setLanguage(language: String) {
        preferences.edit().putString(KEY_LANGUAGE, language).apply()
    }

    fun getLanguage(): String {
        return preferences.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    fun clearUserSession() {
        preferences.edit()
            .remove(KEY_IS_LOGGED_IN)
            .remove(KEY_USER_ID)
            .apply()
    }
}