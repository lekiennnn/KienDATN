package com.example.kiendatn2.ui.search

import com.example.kiendatn2.data.Post
import com.example.kiendatn2.data.SearchHistory

data class SearchUIState(
    val searchState: SearchState = SearchState.Initial,
    val searchHistory: List<SearchHistory> = emptyList()
)

sealed class SearchState {
    object Initial : SearchState()
    object Loading : SearchState()
    data class Success(val posts: List<Post>) : SearchState()
    data class Error(val message: String) : SearchState()
}