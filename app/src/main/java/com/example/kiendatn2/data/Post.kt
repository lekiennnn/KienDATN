package com.example.kiendatn2.data

import com.google.firebase.Timestamp

enum class PostVisibility {
    PUBLIC,
    FRIENDS_ONLY,
    PRIVATE
}

data class Post(
    val id: String = "",
    val userId: String = "",
    val userDisplayName: String = "",
    val userProfilePicture: String? = null,
    val text: String = "",
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val hasVideo: Boolean = false,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val shareCount: Int = 0,
    val isLikedByCurrentUser: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val isPrivate: Boolean = false,
    val isSharedPost: Boolean = false,
    val sharedByUserId: String = "",
    val sharedByUserName: String = "",
    val isSharedByCurrentUser: Boolean = false,
    val visibility: PostVisibility = PostVisibility.PUBLIC,
    val isArchivedByCurrentUser: Boolean = false
)