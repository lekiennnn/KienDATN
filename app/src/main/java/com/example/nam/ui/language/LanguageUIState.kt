package com.example.nam.ui.language

data class LanguageUIState(
    val languages: List<String> = emptyList(),
    val selectedLanguage: String = ""
)