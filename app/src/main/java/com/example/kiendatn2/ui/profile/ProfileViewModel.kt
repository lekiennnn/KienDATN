package com.example.kiendatn2.ui.profile

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kiendatn2.data.FriendshipStatus
import com.example.kiendatn2.data.User
import com.example.kiendatn2.repository.FirebaseRepository
import com.example.kiendatn2.data.Post
import com.example.kiendatn2.data.PostVisibility
import com.example.kiendatn2.service.CloudinaryUploader
import com.example.kiendatn2.service.ImageUploadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class ProfileUIState(
    val profileState: ProfileState = ProfileState.Loading,
    val userPostsState: UserPostsState = UserPostsState.Loading,
    val imageUploadState: ImageUploadState = ImageUploadState.Idle,
    val profileUpdated: Boolean = false,
    val friendshipStatus: FriendshipStatus? = null,
    val isFriendRequestInProgress: Boolean = false,
    val isRequestSender: Boolean = false
)

class ProfileViewModel : ViewModel() {
    private val repository = FirebaseRepository()
    private val cloudinaryUploader = CloudinaryUploader()

    private val _uiState = MutableStateFlow(ProfileUIState())
    val uiState: StateFlow<ProfileUIState> = _uiState

    init {
        observeImageUploadState()
    }

    private fun observeImageUploadState() {
        cloudinaryUploader.imageUploadState
            .onEach { uploadState ->
                _uiState.value = _uiState.value.copy(imageUploadState = uploadState)
            }
            .launchIn(viewModelScope)
    }

    fun loadUserProfile(userId: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(profileState = ProfileState.Loading)
            try {
                if (userId == null) {
                    val user = repository.getCurrentUser()
                    if (user != null) {
                        _uiState.value =
                            _uiState.value.copy(profileState = ProfileState.Success(user))
                    } else {
                        try {
                            val currentFirebaseUser = repository.getCurrentFirebaseUser()
                            if (currentFirebaseUser != null) {
                                val newUser = User(
                                    id = currentFirebaseUser.uid,
                                    displayName = currentFirebaseUser.displayName
                                        ?: currentFirebaseUser.email?.substringBefore('@')
                                        ?: "User",
                                    email = currentFirebaseUser.email ?: ""
                                )
                                repository.createUserProfile(newUser)
                                _uiState.value =
                                    _uiState.value.copy(profileState = ProfileState.Success(newUser))
                            } else {
                                _uiState.value =
                                    _uiState.value.copy(profileState = ProfileState.Error("Not signed in"))
                            }
                        } catch (e: Exception) {
                            _uiState.value = _uiState.value.copy(
                                profileState = ProfileState.Error("Failed to create user profile: ${e.message}")
                            )
                        }
                    }
                } else if (userId.isNotEmpty()) {
                    val user = repository.getUserById(userId)
                    user?.let {
                        _uiState.value =
                            _uiState.value.copy(profileState = ProfileState.Success(it))
                        // Check friendship status when viewing another user's profile
                        checkFriendshipStatus(userId)
                    } ?: run {
                        _uiState.value =
                            _uiState.value.copy(profileState = ProfileState.Error("User not found"))
                    }
                } else {
                    _uiState.value =
                        _uiState.value.copy(profileState = ProfileState.Error("Invalid user ID"))
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    profileState = ProfileState.Error(e.message ?: "Error loading profile")
                )
            }
        }
    }

    fun loadUserPosts(userId: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(userPostsState = UserPostsState.Loading)
            try {
                Log.d("ProfileViewModel", "Loading posts for user ${userId ?: "current user"}")

                val posts = if (userId == null) {
                    repository.getUserPosts()
                } else {
                    repository.getUserPostsById(userId)
                }

                // Load shared posts as well
                val sharedPosts = if (userId == null) {
                    repository.getPostsSharedByUser()
                } else {
                    repository.getPostsSharedByUser(userId)
                }

                // Filter out private posts from other users
                val currentUserId = repository.getCurrentFirebaseUser()?.uid
                val filteredPosts = posts.filter {
                    it.visibility == PostVisibility.PUBLIC || it.userId == currentUserId
                }
                val filteredSharedPosts = sharedPosts.filter {
                    it.visibility == PostVisibility.PUBLIC || it.userId == currentUserId
                }

                // Combine both regular posts and shared posts
                val combinedPosts =
                    (filteredPosts + filteredSharedPosts).sortedByDescending { it.createdAt }

                // Debug logging for post visibility
                Log.d(
                    "ProfileViewModel",
                    "Loaded ${combinedPosts.size} posts (${filteredPosts.size} regular, ${filteredSharedPosts.size} shared) for user ${userId ?: "current user"}"
                )

                // Debug each post's visibility
                combinedPosts.forEach { post ->
                    Log.d(
                        "ProfileViewModel",
                        "Post ${post.id}: visibility=${post.visibility}, isSharedPost=${post.isSharedPost}, text='${
                            post.text.take(20).replace('\n', ' ')
                        }'"
                    )
                }

                _uiState.value =
                    _uiState.value.copy(userPostsState = UserPostsState.Success(combinedPosts))
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading posts: ${e.message}", e)
                _uiState.value =
                    _uiState.value.copy(
                        userPostsState = UserPostsState.Error(
                            e.message ?: "Error loading posts"
                        )
                    )
            }
        }
    }
    
    fun updateProfile(displayName: String, bio: String?, photoUri: Uri?) {
        viewModelScope.launch {
            try {
                repository.updateUserProfile(displayName, bio, photoUri)
                loadUserProfile()
                _uiState.value = _uiState.value.copy(profileUpdated = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    profileState = ProfileState.Error(e.message ?: "Error updating profile")
                )
            }
        }
    }

    fun updateProfilePictureWithCloudinary(context: Context, photoUri: Uri?) {
        viewModelScope.launch {
            try {
                if (photoUri == null) {
                    _uiState.value = _uiState.value.copy(
                        profileState = ProfileState.Error("No image selected")
                    )
                    return@launch
                }

                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(photoUri)

                if (mimeType?.startsWith("video/") == true) {
                    _uiState.value = _uiState.value.copy(
                        profileState = ProfileState.Error("Profile picture can't be a video")
                    )
                    return@launch
                }

                cloudinaryUploader.uploadProfileImage(context, photoUri)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    profileState = ProfileState.Error(e.message ?: "Error updating profile picture")
                )
                cloudinaryUploader.resetImageState()
            }
        }
    }

    fun updateUserWithCloudinaryUrl(imageUrl: String) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value.profileState
                if (currentState is ProfileState.Success) {
                    repository.updateUserProfileWithCloudinaryUrl(
                        currentState.user.displayName,
                        currentState.user.bio,
                        imageUrl
                    )
                    loadUserProfile()
                    _uiState.value = _uiState.value.copy(profileUpdated = true)
                    cloudinaryUploader.resetImageState()
                } else {
                    _uiState.value = _uiState.value.copy(
                        profileState = ProfileState.Error("Cannot update profile picture while profile is not loaded")
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    profileState = ProfileState.Error(
                        e.message ?: "Error updating profile with Cloudinary URL"
                    )
                )
                cloudinaryUploader.resetImageState()
            }
        }
    }

    fun resetProfileUpdatedFlag() {
        _uiState.value = _uiState.value.copy(profileUpdated = false)
    }

    private fun checkFriendshipStatus(userId: String) {
        viewModelScope.launch {
            try {
                val currentUser = repository.getCurrentFirebaseUser() ?: return@launch
                val result = repository.getFriendshipStatusWithRole(currentUser.uid, userId)
                _uiState.value = _uiState.value.copy(
                    friendshipStatus = result.first,
                    isRequestSender = result.second
                )
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error checking friendship status: ${e.message}", e)
            }
        }
    }

    fun sendFriendRequest(userId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isFriendRequestInProgress = true)
                val success = repository.sendFriendRequest(userId)
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        friendshipStatus = FriendshipStatus.PENDING,
                        isFriendRequestInProgress = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isFriendRequestInProgress = false)
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error sending friend request: ${e.message}", e)
                _uiState.value = _uiState.value.copy(isFriendRequestInProgress = false)
            }
        }
    }

    fun removeFriend(userId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isFriendRequestInProgress = true)
                val success = repository.removeFriend(userId)
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        friendshipStatus = null,
                        isFriendRequestInProgress = false,
                        isRequestSender = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isFriendRequestInProgress = false)
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error removing friend: ${e.message}", e)
                _uiState.value = _uiState.value.copy(isFriendRequestInProgress = false)
            }
        }
    }

    fun cancelFriendRequest(userId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isFriendRequestInProgress = true)
                val success = repository.cancelFriendRequest(userId)
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        friendshipStatus = null,
                        isFriendRequestInProgress = false,
                        isRequestSender = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isFriendRequestInProgress = false)
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error canceling friend request: ${e.message}", e)
                _uiState.value = _uiState.value.copy(isFriendRequestInProgress = false)
            }
        }
    }
}

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val user: User) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

sealed class UserPostsState {
    object Loading : UserPostsState()
    data class Success(val posts: List<Post>) : UserPostsState()
    data class Error(val message: String) : UserPostsState()
}