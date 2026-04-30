package com.example.nam.ui.search

import com.example.nam.data.Post
import com.example.nam.data.SearchHistory

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