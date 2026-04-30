package com.example.nam.utils

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import java.util.Locale

class LocaleManager {
    companion object {
        fun setLocale(context: Context, language: String): Context {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                updateResources(context, language)
            } else {
                updateResourcesLegacy(context, language)
            }
        }

        @TargetApi(Build.VERSION_CODES.N)
        private fun updateResources(context: Context, language: String): Context {
            val locale = getLocaleFromLanguage(language)

            val configuration = context.resources.configuration
            configuration.setLocale(locale)

            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            configuration.setLocales(localeList)

            return context.createConfigurationContext(configuration)
        }

        @Suppress("DEPRECATION")
        private fun updateResourcesLegacy(context: Context, language: String): Context {
            val locale = getLocaleFromLanguage(language)

            val resources = context.resources
            val configuration = resources.configuration
            configuration.locale = locale

            Locale.setDefault(locale)

            resources.updateConfiguration(configuration, resources.displayMetrics)

            return context
        }

        private fun getLocaleFromLanguage(language: String): Locale {
            return when (language) {
                "English" -> Locale("en")
                "Vietnamese" -> Locale("vi")
                "French" -> Locale("fr")
                "German" -> Locale("de")
                "Spanish" -> Locale("es")
                else -> Locale("en") // Default to English
            }
        }

        fun applyLocale(context: Context): Context {
            val sharedPreferenceManager = SharedPreferenceManager.getInstance(context)
            val language = sharedPreferenceManager.getLanguage()
            return setLocale(context, language)
        }

        fun applyLocaleForApplication(context: Context): Context {
            val prefs = context.getSharedPreferences(
                SharedPreferenceManager.PREF_NAME,
                Context.MODE_PRIVATE
            )
            val language = prefs.getString(
                SharedPreferenceManager.KEY_LANGUAGE,
                SharedPreferenceManager.DEFAULT_LANGUAGE
            ) ?: SharedPreferenceManager.DEFAULT_LANGUAGE

            return setLocale(context, language)
        }
    }
}