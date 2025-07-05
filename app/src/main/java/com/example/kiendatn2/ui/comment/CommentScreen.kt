package com.example.kiendatn2.ui.comment

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.kiendatn2.R
import com.example.kiendatn2.data.Comment
import com.example.kiendatn2.ui.components.VideoPlayer
import com.example.kiendatn2.ui.media.mediapicker.MediaPicker
import com.example.kiendatn2.ui.post.CommentsState
import com.example.kiendatn2.ui.post.CurrentPostState
import com.example.kiendatn2.ui.post.PostItemDetailed
import com.example.kiendatn2.ui.post.PostViewModel
import com.example.kiendatn2.ui.theme.LocalCustomColors
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavController? = null,
    postViewModel: PostViewModel = viewModel()
) {
    val uiState = postViewModel.uiState.collectAsStateWithLifecycle().value

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    var commentText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isVideo by remember { mutableStateOf(false) }
    var showMediaPicker by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState()

    var hasStoragePermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_VIDEO
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_IMAGES] == true &&
            permissions[Manifest.permission.READ_MEDIA_VIDEO] == true
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }
    }

    fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            )
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            )
        }
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri)
            isVideo = mimeType?.startsWith("video/") ?: false
            selectedImageUri = uri
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.post_details),
                        color = LocalCustomColors.current.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        postViewModel.clearCurrentPost()
                        onBackClick()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LocalCustomColors.current.navBarsBackground
                ),
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState.currentPostState) {
                is CurrentPostState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is CurrentPostState.PostLoaded -> {
                    val post = uiState.currentPostState.post

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Main content - scrollable
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 80.dp) // Add space for comment input
                        ) {
                            // Post details
                            item {
                                PostItemDetailed(
                                    authorName = post.userDisplayName,
                                    content = post.text,
                                    imageUrl = post.imageUrl,
                                    videoUrl = post.videoUrl,
                                    hasVideo = post.hasVideo,
                                    likeCount = post.likeCount,
                                    profilePictureUrl = post.userProfilePicture,
                                    commentCount = post.commentCount,
                                    shareCount = post.shareCount,
                                    isLikedByCurrentUser = post.isLikedByCurrentUser,
                                    userId = post.userId,
                                    navController = navController,
                                    onLikeClick = { postViewModel.toggleLike(post.id) },
                                    onCommentClick = { /* Already on comment screen */ },
                                    onShareClick = { isPrivate ->
                                        postViewModel.sharePost(post.id, isPrivate)
                                    },
                                    isSharedByCurrentUser = post.isSharedByCurrentUser,
                                    post = post,
                                    onDeleteClick = { postId -> postViewModel.deletePost(postId) },
                                    onArchiveClick = { postId -> postViewModel.toggleArchive(postId) },
                                    onUnarchiveClick = { postId ->
                                        postViewModel.toggleArchive(
                                            postId
                                        )
                                    },
                                    isArchived = post.isArchivedByCurrentUser,
                                    onEditClick = { editedPost -> postViewModel.updatePost(editedPost.id, editedPost.text, editedPost.visibility) }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Comments section divider
                            item {
                                HorizontalDivider(
                                    color = Color.LightGray,
                                    thickness = 4.dp
                                )
                            }

                            item {
                                Text(
                                    text = stringResource(R.string.comments),
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }

                            // Display comments
                            when (uiState.commentsState) {
                                is CommentsState.Loading -> {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                }
                                is CommentsState.Success -> {
                                    val comments = uiState.commentsState.comments

                                    if (comments.isEmpty()) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(32.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(stringResource(R.string.no_comments_yet))
                                            }
                                        }
                                    } else {
                                        // Build a map of comment ID to its children
                                        val commentMap = mutableMapOf<String?, List<Comment>>()
                                        comments.forEach { comment ->
                                            val parentId = comment.parentCommentId
                                            val currentList = commentMap[parentId] ?: emptyList()
                                            commentMap[parentId] = currentList + comment
                                        }

                                        // Display top-level comments (those with no parent)
                                        val topLevelComments = commentMap[null] ?: emptyList()
                                        items(topLevelComments) { topLevelComment ->
                                            CommentItem(
                                                comment = topLevelComment,
                                                postViewModel = postViewModel,
                                                profilePictureUrl = post.userProfilePicture,
                                                replies = commentMap[topLevelComment.id]
                                                    ?: emptyList(),
                                                isShowingReplies = uiState.repliesState.containsKey(
                                                    topLevelComment.id
                                                ),
                                                userId = topLevelComment.userId,
                                                navController = navController,
                                                onReplyClick = { clickedComment ->
                                                    postViewModel.setReplyingTo(clickedComment)
                                                },
                                                indentLevel = 0
                                            )
                                        }
                                    }
                                }
                                is CommentsState.Error -> {
                                    item {
                                        Text(
                                            text = stringResource(
                                                R.string.error_loading_comments,
                                                uiState.commentsState.message
                                            ),
                                            color = Color.Red,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                                null -> {
                                    item {
                                        Text(
                                            text = stringResource(R.string.loading_comments),
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Comment input section - fixed at bottom
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                // Show who we're replying to, if applicable
                                uiState.replyingToState?.let { comment ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(
                                                R.string.replying_to,
                                                comment.userDisplayName
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { postViewModel.cancelReply() }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Cancel reply"
                                            )
                                        }
                                    }
                                }

                                // Show selected image preview if any
                                selectedImageUri?.let { uri ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                            .padding(bottom = 8.dp)
                                    ) {
                                        if (isVideo) {
                                            VideoPlayer(
                                                videoUrl = uri.toString(),
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                        } else {
                                            AsyncImage(
                                                model = uri,
                                                contentDescription = "Selected image",
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }

                                        // Close/remove image button
                                        Box(
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .size(22.dp)
                                                .align(Alignment.TopEnd)
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(
                                                        alpha = 0.7f
                                                    ),
                                                    CircleShape
                                                )
                                                .clickable { selectedImageUri = null },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Remove image",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }

                                // Show upload progress if uploading
                                if (uiState.uploadingMedia) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            stringResource(R.string.uploading),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = commentText,
                                        onValueChange = { commentText = it },
                                        modifier = Modifier.weight(1f),
                                        placeholder = {
                                            Text(
                                                if (uiState.replyingToState != null) {
                                                    stringResource(R.string.write_a_reply)
                                                } else {
                                                    stringResource(R.string.add_a_comment)
                                                }
                                            )
                                        },
                                        maxLines = 3
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Image picker button
                                    IconButton(
                                        onClick = { 
                                            if (!hasStoragePermission) {
                                                requestPermissions()
                                            }
                                            showMediaPicker = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = "Attach media",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (commentText.isNotBlank() || selectedImageUri != null) {
                                                val replyingToComment = uiState.replyingToState

                                                if (replyingToComment != null) {
                                                    postViewModel.addReply(
                                                        context,
                                                        replyingToComment.id,
                                                        commentText,
                                                        selectedImageUri,
                                                        isVideo
                                                    )
                                                } else {
                                                    postViewModel.addComment(
                                                        context,
                                                        post.id,
                                                        commentText,
                                                        selectedImageUri,
                                                        isVideo
                                                    )
                                                }
                                                commentText = ""
                                                selectedImageUri = null
                                                keyboardController?.hide()
                                                focusManager.clearFocus()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = if (uiState.replyingToState != null) "Send reply" else "Send comment",
                                            tint = if (commentText.isNotBlank() || selectedImageUri != null)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                is CurrentPostState.Error -> {
                    Text(
                        text = stringResource(R.string.error, uiState.currentPostState.message),
                        color = Color.Red,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                CurrentPostState.NotSelected -> {
                    Text(
                        text = stringResource(R.string.no_post_selected),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }

    if (showMediaPicker) {
        ModalBottomSheet(
            onDismissRequest = { showMediaPicker = false },
            sheetState = bottomSheetState,
            dragHandle = null
        ) {
            MediaPicker(
                onMediaSelected = { uri, isMediaVideo ->
                    selectedImageUri = uri
                    isVideo = isMediaVideo
                    showMediaPicker = false
                },
                onDismiss = { showMediaPicker = false },
                hasPermission = hasStoragePermission,
                requestPermission = { requestPermissions() },
                includeVideos = true
            )
        }
    }
}