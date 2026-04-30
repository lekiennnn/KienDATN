package com.example.nam.ui.language

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.nam.utils.SharedPreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LanguageViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(LanguageUIState())
    val uiState: StateFlow<LanguageUIState> = _uiState

    private val sharedPreferenceManager = SharedPreferenceManager.getInstance(application)

    init {
        fetchLanguages()
    }

    fun fetchLanguages() {
        val languages = listOf("English", "Vietnamese", "French", "German", "Spanish")
        val savedLanguage = sharedPreferenceManager.getLanguage()

        _uiState.value = _uiState.value.copy(
            languages = languages,
            selectedLanguage = if (languages.contains(savedLanguage)) savedLanguage else "English"
        )
    }

    fun selectLanguage(language: String) {
        _uiState.value = _uiState.value.copy(selectedLanguage = language)
    }

    fun confirmLanguage(): String {
        val selectedLanguage = _uiState.value.selectedLanguage
        // Save the selected language preference
        sharedPreferenceManager.setLanguage(selectedLanguage)
        return selectedLanguage
    }
}