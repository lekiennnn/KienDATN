package com.example.kiendatn2.ui.post

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.kiendatn2.data.Post
import com.example.kiendatn2.data.PostVisibility
import com.example.kiendatn2.repository.FirebaseRepository

@Composable
fun PostItem(
    modifier: Modifier = Modifier,
    post: Post,
    postViewModel: PostViewModel,
    navController: NavController? = null,
    onEditClick: ((Post) -> Unit)? = null
) {
    // State to track if post is being loaded for comments
    var isLoading by remember { mutableStateOf(false) }

    // Local state to track sharing status for immediate UI feedback
    var isSharedByCurrentUser by remember { mutableStateOf(post.isSharedByCurrentUser) }

    // Local state to track if post is archived
    var isArchived by remember { mutableStateOf(post.isArchivedByCurrentUser) }

    // Update local state when post archive state changes
    LaunchedEffect(post.isArchivedByCurrentUser) {
        isArchived = post.isArchivedByCurrentUser
    }

    // Force a strong value for the shared status for debugging
    LaunchedEffect(key1 = true) {
        if (post.isSharedByCurrentUser) {
            android.util.Log.d(
                "PostItem",
                "Force setting isSharedByCurrentUser=true on initial render for post ${post.id}"
            )
            isSharedByCurrentUser = true
        }
    }

    // Keep local state in sync with prop changes
    LaunchedEffect(post.isSharedByCurrentUser) {
        isSharedByCurrentUser = post.isSharedByCurrentUser
        android.util.Log.d(
            "PostItem",
            "LaunchedEffect: Post ${post.id} isSharedByCurrentUser updated to $isSharedByCurrentUser"
        )
    }

    // Log image URL for debugging
    LaunchedEffect(post.id) {
        android.util.Log.d(
            "PostItem",
            "Post ID: ${post.id}, Has image: ${post.imageUrl != null}, Has video: ${post.hasVideo}, Video URL: ${post.videoUrl ?: "none"}"
        )

        // Debug log for shared post properties
        if (post.isSharedPost) {
            android.util.Log.d(
                "PostItem",
                "Shared post - ID: ${post.id}, sharedByUser: ${post.sharedByUserName}"
            )
        }

        // Debug log for current user shares
        if (post.isSharedByCurrentUser) {
            android.util.Log.d(
                "PostItem",
                "Post ${post.id} is shared by current user"
            )
        }
    }

    PostItemDetailed(
        modifier = modifier,
        authorName = post.userDisplayName,
        profilePictureUrl = post.userProfilePicture,
        content = post.text,
        imageUrl = post.imageUrl,
        videoUrl = post.videoUrl,
        hasVideo = post.hasVideo,
        likeCount = post.likeCount,
        commentCount = post.commentCount,
        shareCount = post.shareCount,
        isLikedByCurrentUser = post.isLikedByCurrentUser,
        userId = post.userId,
        navController = navController,
        onLikeClick = { postViewModel.toggleLike(post.id) },
        onCommentClick = {
            isLoading = true
            postViewModel.loadPostById(post.id)
        },
        onShareClick = { isPrivate ->
            if (isSharedByCurrentUser) {
                isSharedByCurrentUser = false
                android.util.Log.d(
                    "PostItem",
                    "Unsharing post ${post.id}, updating UI immediately"
                )
            } else {
                isSharedByCurrentUser = true
                android.util.Log.d(
                    "PostItem",
                    "Sharing post ${post.id}, updating UI immediately"
                )
            }
            android.util.Log.d(
                "PostItem",
                "Share button clicked, toggling to $isSharedByCurrentUser and calling postViewModel.sharePost()"
            )
            postViewModel.sharePost(post.id, isPrivate)
        },
        isSharedPost = post.isSharedPost,
        sharedByUserName = post.sharedByUserName,
        isSharedByCurrentUser = isSharedByCurrentUser,
        post = post,
        onDeleteClick = { postId -> postViewModel.deletePost(postId) },
        onHideClick = { postId -> postViewModel.hidePost(postId) },
        onArchiveClick = { postId ->
            isArchived = true
            postViewModel.toggleArchive(postId)
        },
        onUnarchiveClick = { postId ->
            isArchived = false
            postViewModel.toggleArchive(postId)
        },
        onEditClick = onEditClick,
        isArchived = isArchived
    )

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp)
            )
        }
    }
}