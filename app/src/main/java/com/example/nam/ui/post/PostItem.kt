package com.example.nam.ui.post

import android.util.Log
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
import com.example.nam.data.Post
import com.example.nam.utils.Navigation

@Composable
fun PostItem(
    modifier: Modifier = Modifier,
    post: Post,
    postViewModel: PostViewModel,
    navController: NavController? = null,
    onEditClick: ((Post) -> Unit)? = null
) {
    var isLoading by remember { mutableStateOf(false) }

    var isSharedByCurrentUser by remember { mutableStateOf(post.isSharedByCurrentUser) }

    var isArchived by remember { mutableStateOf(post.isArchivedByCurrentUser) }

    LaunchedEffect(post.isArchivedByCurrentUser) {
        isArchived = post.isArchivedByCurrentUser
    }

    LaunchedEffect(key1 = true) {
        if (post.isSharedByCurrentUser) {
            isSharedByCurrentUser = true
        }
    }

    LaunchedEffect(post.isSharedByCurrentUser) {
        isSharedByCurrentUser = post.isSharedByCurrentUser
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
            } else {
                isSharedByCurrentUser = true
            }
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