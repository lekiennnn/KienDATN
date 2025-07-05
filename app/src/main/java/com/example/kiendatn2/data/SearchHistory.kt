package com.example.kiendatn2.data

import com.google.firebase.Timestamp

data class SearchHistory(
    val id: String = "",
    val userId: String = "",
    val query: String = "",
    val timestamp: Timestamp = Timestamp.now()
)