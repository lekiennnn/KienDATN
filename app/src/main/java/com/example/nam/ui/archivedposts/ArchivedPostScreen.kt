package com.example.nam.ui.archivedposts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.nam.R
import com.example.nam.ui.post.PostItemDetailed
import com.example.nam.ui.post.PostState
import com.example.nam.ui.post.PostViewModel
import com.example.nam.ui.theme.LocalCustomColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedPostScreen(
    modifier: Modifier = Modifier,
    postViewModel: PostViewModel = viewModel(),
    navController: NavController? = null,
    onBackClick: () -> Unit = {}
) {
    val uiState by postViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        postViewModel.loadArchivedPosts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    stringResource(R.string.archived_posts)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LocalCustomColors.current.primaryBackground,
                    titleContentColor = LocalCustomColors.current.textPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val archivedPostsState = uiState.archivedPostsState) {
                is PostState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is PostState.Success -> {
                    if (archivedPostsState.posts.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_archived_posts),
                                style = MaterialTheme.typography.bodyLarge,
                                color = LocalCustomColors.current.textSecondary
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(archivedPostsState.posts) { post ->
                                Column {
                                    PostItemDetailed(
                                        modifier = Modifier.padding(
                                            horizontal = 16.dp,
                                            vertical = 8.dp
                                        ),
                                        authorName = post.userDisplayName,
                                        content = post.text,
                                        imageUrl = post.imageUrl,
                                        videoUrl = post.videoUrl,
                                        hasVideo = post.hasVideo,
                                        likeCount = post.likeCount,
                                        isLikedByCurrentUser = post.isLikedByCurrentUser,
                                        commentCount = post.commentCount,
                                        shareCount = post.shareCount,
                                        profilePictureUrl = post.userProfilePicture,
                                        onLikeClick = { postViewModel.toggleLike(post.id) },
                                        onCommentClick = { postViewModel.loadPostById(post.id) },
                                        onShareClick = { isPrivate ->
                                            postViewModel.sharePost(
                                                post.id,
                                                isPrivate
                                            )
                                        },
                                        userId = post.userId,
                                        navController = navController,
                                        isSharedPost = post.isSharedPost,
                                        sharedByUserName = post.sharedByUserName ?: "",
                                        isSharedByCurrentUser = post.isSharedByCurrentUser,
                                        post = post,
                                        onDeleteClick = { postId -> postViewModel.deletePost(postId) },
                                        onUnarchiveClick = { postId ->
                                            postViewModel.unarchivePost(
                                                postId
                                            )
                                        },
                                        isInArchivedPostScreen = true,
                                        isArchived = true
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }

                is PostState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = archivedPostsState.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}