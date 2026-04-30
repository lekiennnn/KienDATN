package com.example.nam.ui.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nam.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    private val _uiState = MutableStateFlow(SearchUIState())
    val uiState: StateFlow<SearchUIState> = _uiState

    init {
        Log.d("SearchViewModel", "Initializing SearchViewModel")
        loadSearchHistory()
    }

    fun searchPosts(query: String) {
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(searchState = SearchState.Initial)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(searchState = SearchState.Loading)
            try {
                val posts = repository.searchPosts(query)
                _uiState.value = _uiState.value.copy(searchState = SearchState.Success(posts))
            } catch (e: Exception) {
                Log.e("PostSearchViewModel", "Error searching posts: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    searchState = SearchState.Error(
                        e.message ?: "Failed to search posts"
                    )
                )
            }
        }
    }

    fun saveSearchToHistory(query: String) {
        if (query.length < 2) return

        viewModelScope.launch {
            try {
                Log.d("SearchViewModel", "Saving search query to history: '$query'")
                repository.saveSearchQuery(query)
                Log.d("SearchViewModel", "Search query saved, now reloading history")
                loadSearchHistory() // Reload the history to show the new entry
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error saving search query: ${e.message}", e)
            }
        }
    }

    fun loadSearchHistory() {
        viewModelScope.launch {
            try {
                Log.d("SearchViewModel", "Loading search history...")
                val history = repository.getSearchHistory()
                Log.d("SearchViewModel", "Loaded ${history.size} history items")
                _uiState.value = _uiState.value.copy(searchHistory = history)
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error loading search history: ${e.message}", e)
            }
        }
    }

    fun deleteSearchHistoryItem(historyId: String) {
        viewModelScope.launch {
            try {
                repository.deleteSearchHistoryItem(historyId)
                loadSearchHistory() // Reload history after deletion
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error deleting search history item: ${e.message}")
            }
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            try {
                repository.clearSearchHistory()
                _uiState.value = _uiState.value.copy(searchHistory = emptyList())
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error clearing search history: ${e.message}")
            }
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            try {
                val currentState = uiState.value
                if (currentState.searchState is SearchState.Success) {
                    val updatedPosts =
                        (currentState.searchState as SearchState.Success).posts.map { post ->
                            if (post.id == postId) {
                                post.copy()
                            } else {
                                post
                            }
                        }
                    _uiState.value =
                        _uiState.value.copy(searchState = SearchState.Success(updatedPosts))
                }

                val isLiked = repository.toggleLike(postId)

                (uiState.value.searchState as? SearchState.Success)?.let { state ->
                    val updatedPosts = state.posts.map { post ->
                        if (post.id == postId) {
                            val newLikeCount =
                                if (isLiked) post.likeCount + 1 else post.likeCount - 1
                            post.copy(
                                isLikedByCurrentUser = isLiked,
                                likeCount = newLikeCount
                            )
                        } else {
                            post
                        }
                    }
                    _uiState.value =
                        _uiState.value.copy(searchState = SearchState.Success(updatedPosts))
                }
            } catch (e: Exception) {
                Log.e("PostSearchViewModel", "Error toggling like: ${e.message}")
            }
        }
    }
}