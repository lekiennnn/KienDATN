package com.example.nam.ui.post

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nam.repository.FirebaseRepository
import com.example.nam.data.Comment
import com.example.nam.data.Post
import com.example.nam.data.PostVisibility
import com.example.nam.service.CloudinaryUploader
import com.example.nam.service.MediaUploadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PostViewModel : ViewModel() {
    private val repository = FirebaseRepository()
    private val cloudinaryUploader = CloudinaryUploader()

    private val _uiState = MutableStateFlow(PostUIState())
    val uiState: StateFlow<PostUIState> = _uiState.asStateFlow()

    init {
        loadPosts()
        loadFriendsPosts()
    }

    fun loadPosts() {
        viewModelScope.launch {
            _uiState.update { it.copy(postsState = PostState.Loading) }
            try {
                val posts = repository.getFilteredPostsWithLikeStatus()
                _uiState.update { it.copy(postsState = PostState.Success(posts)) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(postsState = PostState.Error(e.message ?: "Error loading posts"))
                }
            }
        }
    }

    fun loadFriendsPosts() {
        viewModelScope.launch {
            _uiState.update { it.copy(friendsPostsState = PostState.Loading) }
            try {
                repository.debugFriendships()
                val posts = repository.getFriendsPostsWithLikeStatus()
                _uiState.update { it.copy(friendsPostsState = PostState.Success(posts)) }
                Log.d("PostViewModel", "Loaded ${posts.size} friend posts")
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error loading friends posts", e)
                _uiState.update {
                    it.copy(
                        friendsPostsState = PostState.Error(
                            e.message ?: "Error loading friends' posts"
                        )
                    )
                }
            }
        }
    }

    fun reloadPosts() {
        Log.d("dcmcuocdoi", "reloadPosts: ")
        viewModelScope.launch {
            val currentState = _uiState.value

            loadPosts()

            loadFriendsPosts()

            when (val postState = currentState.currentPostState) {
                is CurrentPostState.PostLoaded -> {
                    loadPostById(postState.post.id)
                }
                else -> {}
            }
        }
    }

    fun createPost(
        context: Context,
        text: String,
        mediaUri: Uri?,
        isVideo: Boolean = false,
        visibility: PostVisibility = PostVisibility.PUBLIC
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    postsState = PostState.Loading,
                    uploadingMedia = true
                )
            }

            try {
                var mediaUrl: String? = null
                if (mediaUri != null) {
                    when (val uploadResult = uploadMedia(context, mediaUri, isVideo)) {
                        is MediaUploadState.Success -> {
                            mediaUrl = uploadResult.mediaUrl
                            Log.d("PostViewModel", "Media uploaded: $mediaUrl")
                        }

                        is MediaUploadState.Error -> {
                            throw Exception("Failed to upload media: ${uploadResult.message}")
                        }

                        else -> {
                            throw Exception("Unexpected upload state")
                        }
                    }
                }

                repository.createPostWithMediaUrl(text, mediaUrl, isVideo, visibility)

                cloudinaryUploader.resetState()
                loadPosts()
                loadFriendsPosts()
                _uiState.update {
                    it.copy(
                        postCreationState = PostCreationState.Success,
                        uploadingMedia = false
                    )
                }
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error creating post", e)
                _uiState.update {
                    it.copy(
                        postsState = PostState.Error(e.message ?: "Failed to create post"),
                        postCreationState = PostCreationState.Error(
                            e.message ?: "Failed to create post"
                        ),
                        uploadingMedia = false
                    )
                }
            }
        }
    }

    fun getCommentsForPost(postId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(commentsState = CommentsState.Loading) }
            try {
                val comments = repository.getCommentsForPost(postId)
                _uiState.update { it.copy(commentsState = CommentsState.Success(comments)) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        commentsState = CommentsState.Error(
                            e.message ?: "Unknown error loading comments"
                        )
                    )
                }
            }
        }
    }

    fun addComment(
        context: Context,
        postId: String,
        text: String,
        mediaUri: Uri?,
        isVideo: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(uploadingMedia = true) }

            try {
                var mediaUrl: String? = null
                if (mediaUri != null) {
                    when (val uploadResult = uploadMedia(context, mediaUri, isVideo)) {
                        is MediaUploadState.Success -> {
                            mediaUrl = uploadResult.mediaUrl
                            Log.d("PostViewModel", "Media uploaded: $mediaUrl")
                        }

                        is MediaUploadState.Error -> {
                            throw Exception("Failed to upload media: ${uploadResult.message}")
                        }

                        else -> {
                            throw Exception("Unexpected upload state")
                        }
                    }
                }

                repository.addCommentWithMediaUrl(postId, text, mediaUrl, isVideo)

                cloudinaryUploader.resetState()
                getCommentsForPost(postId)
                _uiState.update { it.copy(uploadingMedia = false) }
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error adding comment", e)
                _uiState.update {
                    it.copy(
                        commentsState = CommentsState.Error(e.message ?: "Failed to add comment"),
                        uploadingMedia = false
                    )
                }
            }
        }
    }

    fun addReply(
        context: Context,
        parentCommentId: String,
        text: String,
        imageUri: Uri? = null,
        isVideo: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                val postId = _uiState.value.currentPostState.let {
                    if (it is CurrentPostState.PostLoaded) it.post.id else throw IllegalStateException(
                        "No post selected"
                    )
                }
                val mediaUrl = if (imageUri != null) {
                    _uiState.update { it.copy(uploadingMedia = true) }
                    val uploadResult = uploadMedia(context, imageUri, isVideo)
                    if (uploadResult is MediaUploadState.Success) {
                        uploadResult.mediaUrl
                    } else {
                        throw Exception("Failed to upload media")
                    }
                } else null

                repository.addReplyWithMediaUrl(parentCommentId, text, mediaUrl, isVideo)
                getCommentsForPost(postId) // Refresh the entire comment list
                loadReplies(parentCommentId) // Ensure repliesState is updated
                _uiState.update { it.copy(uploadingMedia = false) }
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error adding reply", e)
                _uiState.update {
                    it.copy(
                        commentsState = CommentsState.Error(e.message ?: "Failed to add reply"),
                        uploadingMedia = false
                    )
                }
            }
        }
    }

    fun loadReplies(commentId: String) {
        viewModelScope.launch {
            try {
                val replies = repository.getRepliesForComment(commentId)

                _uiState.update { currentState ->
                    val updatedReplies = currentState.repliesState + (commentId to replies)
                    currentState.copy(repliesState = updatedReplies)
                }
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error loading replies", e)
            }
        }
    }

    fun getRepliesForComment(commentId: String): List<Comment> {
        val currentState = _uiState.value
        return when (val commentsState = currentState.commentsState) {
            is CommentsState.Success -> {
                commentsState.comments.filter { it.parentCommentId == commentId }
            }

            else -> emptyList()
        }
    }

    fun setReplyingTo(comment: Comment) {
        _uiState.update { it.copy(replyingToState = comment) }
    }

    fun cancelReply() {
        _uiState.update { it.copy(replyingToState = null) }
    }

    // Function to get a comment by its ID from all available comments in the UI state
    fun getCommentById(commentId: String): Comment? {
        val currentState = _uiState.value

        // Check in main comments
        val commentsState = currentState.commentsState
        if (commentsState is CommentsState.Success) {
            val foundInComments = commentsState.comments.find { it.id == commentId }
            if (foundInComments != null) {
                return foundInComments
            }
        }

        // Check in replies
        for ((_, replies) in currentState.repliesState) {
            val foundInReplies = replies.find { it.id == commentId }
            if (foundInReplies != null) {
                return foundInReplies
            }
        }

        return null
    }

    fun createPost(context: Context, text: String, imageUri: Uri?) {
        createPost(context, text, imageUri, isVideo = false)
    }

    fun addComment(context: Context, postId: String, text: String, imageUri: Uri?) {
        addComment(context, postId, text, imageUri, isVideo = false)
    }

    fun addReply(context: Context, parentCommentId: String, text: String, imageUri: Uri?) {
        addReply(context, parentCommentId, text, imageUri, isVideo = false)
    }

    private suspend fun uploadMedia(
        context: Context,
        mediaUri: Uri?,
        isVideo: Boolean
    ): MediaUploadState {
        if (mediaUri == null) return MediaUploadState.Idle

        try {
            val result = cloudinaryUploader.uploadMedia(context, mediaUri, isVideo)
            _uiState.update { it.copy(mediaUploadState = result) }

            if (result is MediaUploadState.Uploading) {
                return MediaUploadState.Error("Upload timed out")
            } else {
                return result
            }
        } catch (e: Exception) {
            Log.e("PostViewModel", "Media upload failed", e)
            val errorState =
                MediaUploadState.Error(e.message ?: "Unknown error during media upload")
            _uiState.update { it.copy(mediaUploadState = errorState) }
            return errorState
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            try {
                val isLiked = repository.toggleLike(postId)

                _uiState.update { currentState ->
                    val updatedPostsState =
                        updatePostLikeState(postId, isLiked, currentState.postsState)
                    val updatedFriendsPostsState =
                        updatePostLikeState(postId, isLiked, currentState.friendsPostsState)

                    val updatedCurrentPostState = if (
                        currentState.currentPostState is CurrentPostState.PostLoaded &&
                        currentState.currentPostState.post.id == postId
                    ) {
                        val currentPost =
                            currentState.currentPostState.post
                        val newLikeCount =
                            if (isLiked) currentPost.likeCount + 1 else currentPost.likeCount - 1
                        CurrentPostState.PostLoaded(
                            currentPost.copy(
                                likeCount = newLikeCount.coerceAtLeast(0),
                                isLikedByCurrentUser = isLiked
                            )
                        )
                    } else {
                        currentState.currentPostState
                    }

                    currentState.copy(
                        postsState = updatedPostsState,
                        friendsPostsState = updatedFriendsPostsState,
                        currentPostState = updatedCurrentPostState
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(postsState = PostState.Error(e.message ?: "Failed to toggle like"))
                }
            }
        }
    }

    fun toggleArchive(postId: String) {
        viewModelScope.launch {
            try {
                // Check current archive status
                val isCurrentlyArchived = isPostArchivedByCurrentUser(postId)
                val willBeArchived = !isCurrentlyArchived

                // Immediately update UI for better responsiveness
                _uiState.update { currentState ->
                    val updatedPostsState =
                        updatePostArchiveState(postId, willBeArchived, currentState.postsState)
                    val updatedFriendsPostsState = updatePostArchiveState(
                        postId,
                        willBeArchived,
                        currentState.friendsPostsState
                    )

                    // Update current post state if it's the toggled post
                    val updatedCurrentPostState = if (
                        currentState.currentPostState is CurrentPostState.PostLoaded &&
                        (currentState.currentPostState as CurrentPostState.PostLoaded).post.id == postId
                    ) {
                        val currentPost =
                            (currentState.currentPostState as CurrentPostState.PostLoaded).post
                        CurrentPostState.PostLoaded(
                            currentPost.copy(
                                isArchivedByCurrentUser = willBeArchived
                            )
                        )
                    } else {
                        currentState.currentPostState
                    }

                    currentState.copy(
                        postsState = updatedPostsState,
                        friendsPostsState = updatedFriendsPostsState,
                        currentPostState = updatedCurrentPostState
                    )
                }

                // Perform actual archive/unarchive operation
                if (willBeArchived) {
                    repository.archivePost(postId)
                } else {
                    repository.unarchivePost(postId)
                }

                // Quietly update archived posts state in the background
                loadArchivedPosts()

            } catch (e: Exception) {
                Log.e("PostViewModel", "Error toggling archive status", e)
            }
        }
    }

    fun sharePost(postId: String, isPrivate: Boolean = false) {
        viewModelScope.launch {
            try {
                Log.d("PostViewModel", "Starting share for post ID: $postId, isPrivate: $isPrivate")

                val isCurrentlyShared = isPostSharedByCurrentUser(postId)
                val willBeShared = !isCurrentlyShared

                Log.d(
                    "PostViewModel",
                    "Post $postId - current sharing state: isShared=$isCurrentlyShared, will be shared=$willBeShared"
                )

                // Get the current post if we're on the comment screen
                val currentPostState = _uiState.value.currentPostState
                val currentPost = if (currentPostState is CurrentPostState.PostLoaded &&
                    currentPostState.post.id == postId
                ) {
                    currentPostState.post
                } else {
                    null
                }

                if (currentPost != null) {
                    Log.d(
                        "PostViewModel",
                        "Updating current post UI state - before: shareCount=${currentPost.shareCount}, isShared=${currentPost.isSharedByCurrentUser}"
                    )
                }

                // Immediately update UI for better responsiveness
                _uiState.update { currentState ->
                    val updatedPostsState =
                        updatePostShareToggleState(postId, willBeShared, currentState.postsState)
                    val updatedFriendsPostsState =
                        updatePostShareToggleState(
                            postId,
                            willBeShared,
                            currentState.friendsPostsState
                        )

                    // Update current post directly if it's the one being shared
                    val updatedCurrentPostState = if (
                        currentState.currentPostState is CurrentPostState.PostLoaded &&
                        (currentState.currentPostState as CurrentPostState.PostLoaded).post.id == postId
                    ) {
                        val currentPost =
                            (currentState.currentPostState as CurrentPostState.PostLoaded).post
                        val newShareCount = if (willBeShared) {
                            currentPost.shareCount + 1
                        } else {
                            (currentPost.shareCount - 1).coerceAtLeast(0)
                        }

                        Log.d(
                            "PostViewModel",
                            "Updating current post UI state - after: shareCount=$newShareCount, isShared=$willBeShared"
                        )

                        CurrentPostState.PostLoaded(
                            currentPost.copy(
                                shareCount = newShareCount,
                                isSharedByCurrentUser = willBeShared
                            )
                        )
                    } else {
                        currentState.currentPostState
                    }

                    currentState.copy(
                        postsState = updatedPostsState,
                        friendsPostsState = updatedFriendsPostsState,
                        currentPostState = updatedCurrentPostState
                    )
                }

                // Then perform the actual share operation
                val success = repository.sharePost(postId, isPrivate)
                Log.d("PostViewModel", "Share result for post ID $postId: $success")

                if (!success) {
                    // Check if this was an attempt to share a private post with friends
                    val post = getPostById(postId)
                    if (post?.visibility == PostVisibility.PRIVATE && !isPrivate) {
                        _uiState.update {
                            it.copy(postsState = PostState.Error("Private posts can only be shared as 'Only Me'"))
                        }
                    }
                }

                // Always reload friends posts to ensure they show all shared posts, including those by current user
                loadFriendsPosts()
            } catch (e: Exception) {
                Log.e("PostViewModel", "Exception during share: ${e.message}", e)
                _uiState.update {
                    it.copy(postsState = PostState.Error(e.message ?: "Failed to share post"))
                }
            }
        }
    }

    fun updatePost(postId: String, newText: String, newVisibility: PostVisibility) {
        viewModelScope.launch {
            try {
                val success = repository.updatePost(postId, newText, newVisibility)
                if (success) {
                    loadPosts()
                    loadFriendsPosts()

                    // Update current post state if it's the edited post
                    val currentPostState = _uiState.value.currentPostState
                    if (currentPostState is CurrentPostState.PostLoaded && currentPostState.post.id == postId) {
                        val updatedPost = currentPostState.post.copy(
                            text = newText,
                            visibility = newVisibility,
                            isPrivate = newVisibility == PostVisibility.PRIVATE
                        )
                        _uiState.update {
                            it.copy(currentPostState = CurrentPostState.PostLoaded(updatedPost))
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            postCreationState = PostCreationState.Error("Failed to update post")
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error updating post", e)
                _uiState.update {
                    it.copy(
                        postCreationState = PostCreationState.Error(
                            e.message ?: "Failed to update post"
                        )
                    )
                }
            }
        }
    }

    private fun isPostArchivedByCurrentUser(postId: String): Boolean {
        val forYouPosts = (_uiState.value.postsState as? PostState.Success)?.posts ?: emptyList()
        val friendsPosts =
            (_uiState.value.friendsPostsState as? PostState.Success)?.posts ?: emptyList()
        val currentPost = (_uiState.value.currentPostState as? CurrentPostState.PostLoaded)?.post

        val post = forYouPosts.find { it.id == postId }
            ?: friendsPosts.find { it.id == postId }
            ?: currentPost?.takeIf { it.id == postId }

        return post?.isArchivedByCurrentUser ?: false
    }

    private fun updatePostArchiveState(
        postId: String,
        isArchived: Boolean,
        state: PostState
    ): PostState {
        return when (state) {
            is PostState.Success -> {
                val updatedPosts = state.posts.map { post ->
                    if (post.id == postId) {
                        post.copy(isArchivedByCurrentUser = isArchived)
                    } else post
                }
                PostState.Success(updatedPosts)
            }

            else -> state
        }
    }

    private fun isPostSharedByCurrentUser(postId: String): Boolean {
        val forYouPosts = (_uiState.value.postsState as? PostState.Success)?.posts ?: emptyList()
        val friendsPosts =
            (_uiState.value.friendsPostsState as? PostState.Success)?.posts ?: emptyList()
        val currentPost = (_uiState.value.currentPostState as? CurrentPostState.PostLoaded)?.post

        val post = forYouPosts.find { it.id == postId }
            ?: friendsPosts.find { it.id == postId }
            ?: currentPost?.takeIf { it.id == postId }

        return post?.isSharedByCurrentUser ?: false
    }

    private fun updatePostLikeState(
        postId: String,
        isLiked: Boolean,
        state: PostState
    ): PostState {
        return when (state) {
            is PostState.Success -> {
                val updatedPosts = state.posts.map { post ->
                    if (post.id == postId) {
                        val newLikeCount = if (isLiked) post.likeCount + 1 else post.likeCount - 1
                        post.copy(
                            likeCount = newLikeCount.coerceAtLeast(0),
                            isLikedByCurrentUser = isLiked
                        )
                    } else post
                }
                PostState.Success(updatedPosts)
            }

            else -> state
        }
    }

    private fun updatePostShareState(
        postId: String,
        state: PostState
    ): PostState {
        return when (state) {
            is PostState.Success -> {
                val updatedPosts = state.posts.map { post ->
                    if (post.id == postId) {
                        post.copy(shareCount = post.shareCount + 1)
                    } else post
                }
                PostState.Success(updatedPosts)
            }

            else -> state
        }
    }

    private fun updatePostShareToggleState(
        postId: String,
        wasShared: Boolean,
        state: PostState
    ): PostState {
        return when (state) {
            is PostState.Success -> {
                val updatedPosts = state.posts.map { post ->
                    if (post.id == postId) {
                        val newShareCount = if (wasShared) {
                            post.shareCount + 1
                        } else {
                            (post.shareCount - 1).coerceAtLeast(0)
                        }

                        Log.d(
                            "PostViewModel",
                            "Updating post $postId share state: isShared=$wasShared, newCount=$newShareCount"
                        )

                        Log.d(
                            "PostViewModel",
                            "Post before update - isSharedByCurrentUser: ${post.isSharedByCurrentUser}"
                        )

                        val updatedPost = post.copy(
                            shareCount = newShareCount,
                            isSharedByCurrentUser = wasShared
                        )

                        Log.d(
                            "PostViewModel",
                            "Post after update - isSharedByCurrentUser: ${updatedPost.isSharedByCurrentUser}"
                        )

                        updatedPost
                    } else post
                }
                PostState.Success(updatedPosts)
            }
            else -> state
        }
    }

    fun loadPostById(postId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(currentPostState = CurrentPostState.Loading) }
            try {
                val post = repository.getPostById(postId)
                if (post != null) {
                    _uiState.update { it.copy(currentPostState = CurrentPostState.PostLoaded(post)) }
                    getCommentsForPost(postId)
                } else {
                    _uiState.update { it.copy(currentPostState = CurrentPostState.Error("Post not found")) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        currentPostState = CurrentPostState.Error(
                            e.message ?: "Error loading post"
                        )
                    )
                }
            }
        }
    }

    fun clearCurrentPost() {
        _uiState.update { it.copy(currentPostState = CurrentPostState.NotSelected) }
    }

    fun toggleCommentLike(commentId: String) {
        viewModelScope.launch {
            try {
                val isLiked = repository.toggleCommentLike(commentId)

                _uiState.update { currentState ->
                    val updatedCommentsState =
                        when (val commentsState = currentState.commentsState) {
                            is CommentsState.Success -> {
                                val updatedComments = commentsState.comments.map { comment ->
                                    if (comment.id == commentId) {
                                        val newLikeCount = if (isLiked) {
                                            comment.likeCount + 1
                                        } else {
                                            (comment.likeCount - 1).coerceAtLeast(0)
                                        }
                                        comment.copy(
                                            likeCount = newLikeCount,
                                            isLikedByCurrentUser = isLiked
                                        )
                                    } else {
                                        comment
                                    }
                                }
                                CommentsState.Success(updatedComments)
                            }
                            else -> commentsState
                        }

                    val updatedRepliesState = currentState.repliesState.mapValues { (_, replies) ->
                        replies.map { reply ->
                            if (reply.id == commentId) {
                                val newLikeCount = if (isLiked) {
                                    reply.likeCount + 1
                                } else {
                                    (reply.likeCount - 1).coerceAtLeast(0)
                                }
                                reply.copy(
                                    likeCount = newLikeCount,
                                    isLikedByCurrentUser = isLiked
                                )
                            } else {
                                reply
                            }
                        }
                    }

                    currentState.copy(
                        commentsState = updatedCommentsState,
                        repliesState = updatedRepliesState
                    )
                }
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error toggling comment like", e)
            }
        }
    }

    fun resetPostCreationState() {
        _uiState.update { it.copy(postCreationState = PostCreationState.Initial) }
    }

    fun loadHiddenPosts() {
        viewModelScope.launch {
            _uiState.update { it.copy(hiddenPostsState = PostState.Loading) }
            try {
                val posts = repository.getHiddenPosts()
                _uiState.update { it.copy(hiddenPostsState = PostState.Success(posts)) }
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error loading hidden posts", e)
                _uiState.update {
                    it.copy(
                        hiddenPostsState = PostState.Error(
                            e.message ?: "Error loading hidden posts"
                        )
                    )
                }
            }
        }
    }

    fun loadArchivedPosts() {
        viewModelScope.launch {
            _uiState.update { it.copy(archivedPostsState = PostState.Loading) }
            try {
                val posts = repository.getArchivedPosts()
                _uiState.update { it.copy(archivedPostsState = PostState.Success(posts)) }
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error loading archived posts", e)
                _uiState.update {
                    it.copy(
                        archivedPostsState = PostState.Error(
                            e.message ?: "Error loading archived posts"
                        )
                    )
                }
            }
        }
    }

    /**
     * Archives a post without removing it from the main feed.
     * The post will be added to the archived posts collection and also shown in the archive screen.
     */
    fun archivePost(postId: String) {
        viewModelScope.launch {
            try {
                val success = repository.archivePost(postId)
                if (success) {
                    Log.d("PostViewModel", "Successfully archived post: $postId")

                    // Load archived posts to include this post in the archives screen
                    loadArchivedPosts()
                } else {
                    Log.e("PostViewModel", "Failed to archive post: $postId")
                }
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error archiving post", e)
            }
        }
    }

    fun unarchivePost(postId: String) {
        viewModelScope.launch {
            try {
                val success = repository.unarchivePost(postId)
                if (success) {
                    Log.d("PostViewModel", "Successfully unarchived post: $postId")

                    // Remove the post from archived posts state
                    _uiState.update { currentState ->
                        val updatedArchivedPostsState =
                            when (val archivedPostsState = currentState.archivedPostsState) {
                                is PostState.Success -> {
                                    val updatedPosts =
                                        archivedPostsState.posts.filter { it.id != postId }
                                    PostState.Success(updatedPosts)
                                }

                                else -> currentState.archivedPostsState
                            }

                        currentState.copy(archivedPostsState = updatedArchivedPostsState)
                    }

                    // Refresh the main posts list to include the unarchived post
                    loadPosts()
                } else {
                    Log.e("PostViewModel", "Failed to unarchive post: $postId")
                }
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error unarchiving post", e)
            }
        }
    }

    fun debugFriendships() {
        viewModelScope.launch {
            try {
                repository.debugFriendships()
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error debugging friendships", e)
            }
        }
    }

    // Delete a post - only the post owner can delete their post
    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                val success = repository.deletePost(postId)
                if (success) {
                    Log.d("PostViewModel", "Successfully deleted post: $postId")

                    // Remove the post from all states
                    _uiState.update { currentState ->
                        // Remove from posts state
                        val updatedPostsState = when (val postsState = currentState.postsState) {
                            is PostState.Success -> {
                                val updatedPosts = postsState.posts.filter { it.id != postId }
                                PostState.Success(updatedPosts)
                            }

                            else -> currentState.postsState
                        }

                        // Remove from friends posts state
                        val updatedFriendsPostsState =
                            when (val friendsPostsState = currentState.friendsPostsState) {
                                is PostState.Success -> {
                                    val updatedPosts =
                                        friendsPostsState.posts.filter { it.id != postId }
                                    PostState.Success(updatedPosts)
                                }

                                else -> currentState.friendsPostsState
                            }

                        // Clear current post state if it's the deleted post
                        val updatedCurrentPostState = if (
                            currentState.currentPostState is CurrentPostState.PostLoaded &&
                            (currentState.currentPostState as CurrentPostState.PostLoaded).post.id == postId
                        ) {
                            CurrentPostState.NotSelected
                        } else {
                            currentState.currentPostState
                        }

                        currentState.copy(
                            postsState = updatedPostsState,
                            friendsPostsState = updatedFriendsPostsState,
                            currentPostState = updatedCurrentPostState
                        )
                    }
                } else {
                    Log.e("PostViewModel", "Failed to delete post: $postId")
                }
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error deleting post", e)
            }
        }
    }

    // Hide a post - any user can hide any post from their view
    fun hidePost(postId: String) {
        viewModelScope.launch {
            try {
                val success = repository.hidePost(postId)
                if (success) {
                    Log.d("PostViewModel", "Successfully hid post: $postId")

                    // Remove the post from all states (similar to delete, but only for the current user's view)
                    _uiState.update { currentState ->
                        // Remove from posts state
                        val updatedPostsState = when (val postsState = currentState.postsState) {
                            is PostState.Success -> {
                                val updatedPosts = postsState.posts.filter { it.id != postId }
                                PostState.Success(updatedPosts)
                            }

                            else -> currentState.postsState
                        }

                        // Remove from friends posts state
                        val updatedFriendsPostsState =
                            when (val friendsPostsState = currentState.friendsPostsState) {
                                is PostState.Success -> {
                                    val updatedPosts =
                                        friendsPostsState.posts.filter { it.id != postId }
                                    PostState.Success(updatedPosts)
                                }

                                else -> currentState.friendsPostsState
                            }

                        // Clear current post state if it's the hidden post
                        val updatedCurrentPostState = if (
                            currentState.currentPostState is CurrentPostState.PostLoaded &&
                            (currentState.currentPostState as CurrentPostState.PostLoaded).post.id == postId
                        ) {
                            CurrentPostState.NotSelected
                        } else {
                            currentState.currentPostState
                        }

                        currentState.copy(
                            postsState = updatedPostsState,
                            friendsPostsState = updatedFriendsPostsState,
                            currentPostState = updatedCurrentPostState
                        )
                    }
                } else {
                    Log.e("PostViewModel", "Failed to hide post: $postId")
                }
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error hiding post", e)
            }
        }
    }

    // Unhide a post - any user can unhide a post they previously hid
    fun unhidePost(postId: String) {
        viewModelScope.launch {
            try {
                val success = repository.unhidePost(postId)
                if (success) {
                    Log.d("PostViewModel", "Successfully unhid post: $postId")

                    // Remove the post from hidden posts state
                    _uiState.update { currentState ->
                        val updatedHiddenPostsState =
                            when (val hiddenPostsState = currentState.hiddenPostsState) {
                                is PostState.Success -> {
                                    val updatedPosts =
                                        hiddenPostsState.posts.filter { it.id != postId }
                                    PostState.Success(updatedPosts)
                                }

                                else -> currentState.hiddenPostsState
                            }

                        currentState.copy(hiddenPostsState = updatedHiddenPostsState)
                    }

                    // Refresh the main posts list to include the unhidden post
                    loadPosts()
                } else {
                    Log.e("PostViewModel", "Failed to unhide post: $postId")
                }
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error unhiding post", e)
            }
        }
    }

    // Check if the current user is the owner of a post
    fun isPostOwner(post: Post): Boolean {
        val currentUser = repository.getCurrentFirebaseUser() ?: return false
        return post.userId == currentUser.uid
    }

    // This method is already properly defined earlier in the file, no need to add it again

    private suspend fun getPostById(postId: String): Post? {
        return try {
            val currentPostState = _uiState.value.currentPostState
            if (currentPostState is CurrentPostState.PostLoaded && currentPostState.post.id == postId) {
                currentPostState.post
            } else {
                repository.getPostById(postId)
            }
        } catch (e: Exception) {
            null
        }
    }
}

sealed class PostState {
    object Loading : PostState()
    data class Success(val posts: List<Post>) : PostState()
    data class Error(val message: String) : PostState()
}

sealed class CommentsState {
    object Loading : CommentsState()
    data class Success(val comments: List<Comment>) : CommentsState()
    data class Error(val message: String) : CommentsState()
}

sealed class CurrentPostState {
    object NotSelected : CurrentPostState()
    object Loading : CurrentPostState()
    data class PostLoaded(val post: Post) : CurrentPostState()
    data class Error(val message: String) : CurrentPostState()
}

sealed class PostCreationState {
    object Initial : PostCreationState()
    object Success : PostCreationState()
    data class Error(val message: String) : PostCreationState()
}

// Remove duplicate PostUIState declaration - it's already defined earlier in the file