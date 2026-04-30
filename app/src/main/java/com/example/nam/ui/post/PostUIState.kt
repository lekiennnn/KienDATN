package com.example.nam.ui.post

import com.example.nam.data.Comment
import com.example.nam.service.MediaUploadState

data class PostUIState(
    val postsState: PostState = PostState.Loading,
    val friendsPostsState: PostState = PostState.Loading,
    val commentsState: CommentsState = CommentsState.Loading,
    val currentPostState: CurrentPostState = CurrentPostState.NotSelected,
    val repliesState: Map<String, List<Comment>> = emptyMap(),
    val replyingToState: Comment? = null,
    val mediaUploadState: MediaUploadState = MediaUploadState.Idle,
    val postCreationState: PostCreationState = PostCreationState.Initial,
    val uploadingMedia: Boolean = false,
    val hiddenPostsState: PostState = PostState.Loading,
    val archivedPostsState: PostState = PostState.Loading
)