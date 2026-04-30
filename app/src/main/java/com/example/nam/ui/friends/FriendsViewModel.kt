package com.example.nam.ui.friends

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nam.data.Friendship
import com.example.nam.data.User
import com.example.nam.repository.FriendsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FriendsViewModel : ViewModel() {
    private val repository = FriendsRepository()

    private val _uiState = MutableStateFlow(FriendsUIState())
    val uiState: StateFlow<FriendsUIState> = _uiState
    
    init {
        loadFriends()
        loadPendingRequests()
        loadSentRequests()
    }

    fun isAcceptingRequest(friendshipId: String): Boolean =
        uiState.value.acceptingIds.contains(friendshipId)

    fun isDecliningRequest(friendshipId: String): Boolean =
        uiState.value.decliningIds.contains(friendshipId)

    fun isCancelingRequest(friendshipId: String): Boolean =
        uiState.value.cancelingIds.contains(friendshipId)

    fun loadFriends() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(friendsState = FriendsState.Loading)
            try {
                repository.getFriends()
                    .catch { e ->
                        _uiState.value = _uiState.value.copy(
                            friendsState = FriendsState.Error(
                            e.message ?: "Failed to load friends"
                        )
                        )
                    }
                    .collectLatest { friendships ->
                        _uiState.value =
                            _uiState.value.copy(friendsState = FriendsState.Success(friendships))
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    friendsState = FriendsState.Error(
                    e.message ?: "Failed to load friends"
                )
                )
            }
        }
    }
    
    fun loadPendingRequests() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(pendingRequestsState = FriendsState.Loading)
            Log.d("FriendsViewModel", "Loading pending friend requests...")
            try {
                repository.getPendingFriendRequests()
                    .catch { e ->
                        Log.e(
                            "FriendsViewModel",
                            "Error in flow while loading pending requests: ${e.message}",
                            e
                        )
                        _uiState.value = _uiState.value.copy(
                            pendingRequestsState = FriendsState.Error(
                            e.message ?: "Failed to load friend requests"
                        )
                        )
                    }
                    .collectLatest { friendships ->
                        Log.d("FriendsViewModel", "Received ${friendships.size} pending requests")
                        _uiState.value = _uiState.value.copy(
                            pendingRequestsState = FriendsState.Success(friendships)
                        )
                    }
            } catch (e: Exception) {
                Log.e(
                    "FriendsViewModel",
                    "Exception while loading pending requests: ${e.message}",
                    e
                )
                _uiState.value = _uiState.value.copy(
                    pendingRequestsState = FriendsState.Error(
                    e.message ?: "Failed to load friend requests"
                )
                )
            }
        }
    }
    
    fun loadSentRequests() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(sentRequestsState = FriendsState.Loading)
            Log.d("FriendsViewModel", "Loading sent friend requests...")
            try {
                repository.getSentFriendRequests()
                    .catch { e ->
                        Log.e(
                            "FriendsViewModel",
                            "Error in flow while loading sent requests: ${e.message}",
                            e
                        )
                        _uiState.value = _uiState.value.copy(
                            sentRequestsState = FriendsState.Error(
                            e.message ?: "Failed to load sent requests"
                        )
                        )
                    }
                    .collectLatest { friendships ->
                        Log.d("FriendsViewModel", "Received ${friendships.size} sent requests")
                        _uiState.value =
                            _uiState.value.copy(sentRequestsState = FriendsState.Success(friendships))
                    }
            } catch (e: Exception) {
                Log.e("FriendsViewModel", "Exception while loading sent requests: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    sentRequestsState = FriendsState.Error(
                    e.message ?: "Failed to load sent requests"
                )
                )
            }
        }
    }
    
    fun searchUsers(query: String) {
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(searchState = SearchState.Initial)
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(searchState = SearchState.Loading)
            try {
                val users = repository.searchUsers(query)
                _uiState.value = _uiState.value.copy(searchState = SearchState.Success(users))
            } catch (e: Exception) {
                Log.e("FriendsViewModel", "Error searching users: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    searchState = SearchState.Error(
                        e.message ?: "Failed to search users"
                    )
                )
            }
        }
    }
    
    fun sendFriendRequest(userId: String) {
        viewModelScope.launch {
            try {
                val currentState = uiState.value
                if (currentState.searchState is SearchState.Success) {
                    val updatedUsers =
                        (currentState.searchState as SearchState.Success).users.map { user ->
                        if (user.id == userId) {
                            user.copy(isLoading = true)
                        } else {
                            user
                        }
                    }
                    _uiState.value =
                        _uiState.value.copy(searchState = SearchState.Success(updatedUsers))
                }

                val friendship = repository.sendFriendRequest(userId)

                (uiState.value.searchState as? SearchState.Success)?.let { state ->
                    val updatedUsers = state.users.map { user ->
                        if (user.id == userId) {
                            user.copy(isFollowedByCurrentUser = true, isLoading = false)
                        } else {
                            user
                        }
                    }
                    _uiState.value =
                        _uiState.value.copy(searchState = SearchState.Success(updatedUsers))
                }
                
                loadSentRequests()

                Log.d("FriendsViewModel", "Friend request sent successfully to userId: $userId")
            } catch (e: Exception) {
                Log.e("FriendsViewModel", "Error sending friend request: ${e.message}")
                (uiState.value.searchState as? SearchState.Success)?.let { state ->
                    val updatedUsers = state.users.map { user ->
                        if (user.id == userId) {
                            user.copy(isFollowedByCurrentUser = false, isLoading = false)
                        } else {
                            user
                        }
                    }
                    _uiState.value =
                        _uiState.value.copy(searchState = SearchState.Success(updatedUsers))
                }
            }
        }
    }
    
    fun acceptFriendRequest(friendshipId: String) {
        viewModelScope.launch {
            try {
                _uiState.value =
                    _uiState.value.copy(acceptingIds = _uiState.value.acceptingIds + friendshipId)

                updateFriendshipState(friendshipId, isProcessing = true)

                repository.acceptFriendRequest(friendshipId)

                Log.d("FriendsViewModel", "Friend request accepted successfully")

                loadFriends()
                loadPendingRequests()
            } catch (e: Exception) {
                Log.e("FriendsViewModel", "Error accepting friend request: ${e.message}")
            } finally {
                _uiState.value =
                    _uiState.value.copy(acceptingIds = _uiState.value.acceptingIds - friendshipId)
            }
        }
    }
    
    fun declineFriendRequest(friendshipId: String) {
        viewModelScope.launch {
            try {
                _uiState.value =
                    _uiState.value.copy(decliningIds = _uiState.value.decliningIds + friendshipId)

                updateFriendshipState(friendshipId, isProcessing = true)

                repository.declineFriendRequest(friendshipId)

                Log.d("FriendsViewModel", "Friend request declined successfully")

                loadPendingRequests()
            } catch (e: Exception) {
                Log.e("FriendsViewModel", "Error declining friend request: ${e.message}")
            } finally {
                _uiState.value =
                    _uiState.value.copy(decliningIds = _uiState.value.decliningIds - friendshipId)
            }
        }
    }

    fun cancelFriendRequest(friendshipId: String) {
        viewModelScope.launch {
            try {
                _uiState.value =
                    _uiState.value.copy(cancelingIds = _uiState.value.cancelingIds + friendshipId)

                updateFriendshipState(friendshipId, isProcessing = true)

                repository.cancelFriendRequest(friendshipId)

                loadSentRequests()
            } catch (e: Exception) {
                Log.e("FriendsViewModel", "Error canceling friend request: ${e.message}")
            } finally {
                _uiState.value =
                    _uiState.value.copy(cancelingIds = _uiState.value.cancelingIds - friendshipId)
            }
        }
    }

    fun removeFriend(friendshipId: String) {
        viewModelScope.launch {
            try {
                repository.removeFriend(friendshipId)
                
                loadFriends()
            } catch (e: Exception) {
                Log.e("FriendsViewModel", "Error removing friend: ${e.message}")
            }
        }
    }

    private fun updateFriendshipState(
        friendshipId: String,
        isProcessing: Boolean
    ) {
        val currentState = uiState.value

        when (val friendsState = currentState.friendsState) {
            is FriendsState.Success -> {
                val updatedFriendships = friendsState.friendships.map { friendship ->
                    if (friendship.id == friendshipId) {
                        friendship
                    } else {
                        friendship
                    }
                }
                _uiState.value =
                    _uiState.value.copy(friendsState = FriendsState.Success(updatedFriendships))
            }
            else -> {}
        }

        when (val pendingState = currentState.pendingRequestsState) {
            is FriendsState.Success -> {
                val updatedFriendships = pendingState.friendships.map { friendship ->
                    if (friendship.id == friendshipId) {
                        friendship
                    } else {
                        friendship
                    }
                }
                _uiState.value = _uiState.value.copy(
                    pendingRequestsState = FriendsState.Success(updatedFriendships)
                )
            }

            else -> {}
        }

        when (val sentState = currentState.sentRequestsState) {
            is FriendsState.Success -> {
                val updatedFriendships = sentState.friendships.map { friendship ->
                    if (friendship.id == friendshipId) {
                        friendship
                    } else {
                        friendship
                    }
                }
                _uiState.value =
                    _uiState.value.copy(sentRequestsState = FriendsState.Success(updatedFriendships))
            }

            else -> {}
        }
    }
}

sealed class FriendsState {
    object Loading : FriendsState()
    data class Success(val friendships: List<Friendship>) : FriendsState()
    data class Error(val message: String) : FriendsState()
}

sealed class SearchState {
    object Initial : SearchState()
    object Loading : SearchState()
    data class Success(val users: List<User>) : SearchState()
    data class Error(val message: String) : SearchState()
}