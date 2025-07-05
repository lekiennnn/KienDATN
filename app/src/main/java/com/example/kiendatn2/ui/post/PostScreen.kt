package com.example.kiendatn2.ui.post

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.kiendatn2.R
import com.example.kiendatn2.data.PostVisibility
import com.example.kiendatn2.service.MediaUploadState
import com.example.kiendatn2.ui.components.VideoPlayer
import com.example.kiendatn2.ui.theme.LocalCustomColors
import com.example.kiendatn2.ui.media.mediapicker.MediaPicker
import androidx.core.content.ContextCompat
import com.example.kiendatn2.data.Post

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PostScreen(
    postViewModel: PostViewModel,
    navController: NavController? = null,
    showCreatePost: Boolean = false,
    onCreatePostChange: (Boolean) -> Unit = {}
) {
    val uiState = postViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val tabs = listOf(
        stringResource(R.string.tab_for_you),
        stringResource(R.string.tab_friends_posts)
    )
    var showCreatePostState by remember { mutableStateOf(showCreatePost) }
    var postText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isVideo by remember { mutableStateOf(false) }
    var isCreatingPost by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showMediaPicker by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState()
    var selectedVisibility by remember { mutableStateOf(PostVisibility.PUBLIC) }
    var expandVisibilityDropdown by remember { mutableStateOf(false) }

    // State for edit post dialog
    var showEditPostDialog by remember { mutableStateOf(false) }
    var postToEdit by remember { mutableStateOf<Post?>(null) }
    var editPostText by remember { mutableStateOf("") }
    var editPostVisibility by remember { mutableStateOf(PostVisibility.PUBLIC) }

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

    LaunchedEffect(uiState.value) {
        when (val state = uiState.value.postsState) {
            is PostState.Error -> {
                errorMessage = state.message
                showError = true
            }

            else -> {}
        }
    }

    // Handle upload errors
    LaunchedEffect(uiState.value) {
        when (val state = uiState.value.mediaUploadState) {
            is MediaUploadState.Error -> {
                errorMessage = "Image upload failed: ${state.message}"
                showError = true
            }

            else -> {}
        }
    }

    LaunchedEffect(uiState.value) {
        when (val state = uiState.value.postCreationState) {
            is PostCreationState.Success -> {
                showCreatePostState = false
                onCreatePostChange(false)
                postViewModel.resetPostCreationState()
            }

            is PostCreationState.Error -> {
                errorMessage = state.message
                showError = true
                postViewModel.resetPostCreationState()
            }

            else -> {}
        }
    }

    LaunchedEffect(showCreatePost) {
        showCreatePostState = showCreatePost
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            selectedTabIndex = index
                            android.util.Log.d("PostScreen", "Selected tab: $title")
                        },
                        text = {
                            Text(
                                text = title,
                                color = LocalCustomColors.current.textSecondary
                            )
                        }
                    )
                }
            }

            // Only show posts content when not creating a post
            if (!showCreatePostState) {
                when (selectedTabIndex) {
                    0 -> PostContent(
                        postsState = uiState.value.postsState,
                        postViewModel = postViewModel,
                        onRetry = { postViewModel.loadPosts() },
                        navController = navController,
                        key = "for_you_tab",
                        onEditPost = { post ->
                            postToEdit = post
                            editPostText = post.text
                            editPostVisibility = post.visibility
                            showEditPostDialog = true
                        }
                    )

                    1 -> PostContent(
                        postsState = uiState.value.friendsPostsState,
                        postViewModel = postViewModel,
                        isFriendsPost = true,
                        onRetry = { postViewModel.loadFriendsPosts() },
                        navController = navController,
                        key = "friends_tab",
                        onEditPost = { post ->
                            postToEdit = post
                            editPostText = post.text
                            editPostVisibility = post.visibility
                            showEditPostDialog = true
                        }
                    )
                }
            }

            if (showCreatePostState) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.create_post),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LocalCustomColors.current.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = postText,
                            onValueChange = { postText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.whats_on_your_mind)) },
                            maxLines = 5,
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.post_visibility_label),
                                color = LocalCustomColors.current.textSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Box {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .clickable { expandVisibilityDropdown = true }
                                ) {
                                    Icon(
                                        painter = when (selectedVisibility) {
                                            PostVisibility.PUBLIC -> painterResource(id = R.drawable.ic_language)
                                            PostVisibility.FRIENDS_ONLY -> painterResource(id = R.drawable.ic_friends)
                                            PostVisibility.PRIVATE -> painterResource(id = R.drawable.ic_lock)
                                        },
                                        contentDescription = "Visibility",
                                        tint = LocalCustomColors.current.textPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    Text(
                                        text = when (selectedVisibility) {
                                            PostVisibility.PUBLIC -> stringResource(id = R.string.post_visibility_public)
                                            PostVisibility.FRIENDS_ONLY -> stringResource(id = R.string.post_visibility_friends)
                                            PostVisibility.PRIVATE -> stringResource(id = R.string.post_visibility_private)
                                        },
                                        color = LocalCustomColors.current.textPrimary
                                    )
                                }

                                DropdownMenu(
                                    expanded = expandVisibilityDropdown,
                                    onDismissRequest = { expandVisibilityDropdown = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_language),
                                                    contentDescription = "Public",
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(stringResource(id = R.string.post_visibility_public))
                                            }
                                        },
                                        onClick = {
                                            selectedVisibility = PostVisibility.PUBLIC
                                            expandVisibilityDropdown = false
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_friends),
                                                    contentDescription = "Friends only",
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(stringResource(id = R.string.post_visibility_friends))
                                            }
                                        },
                                        onClick = {
                                            selectedVisibility = PostVisibility.FRIENDS_ONLY
                                            expandVisibilityDropdown = false
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_lock),
                                                    contentDescription = "Only me",
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(stringResource(id = R.string.post_visibility_private))
                                            }
                                        },
                                        onClick = {
                                            selectedVisibility = PostVisibility.PRIVATE
                                            expandVisibilityDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        selectedImageUri?.let { uri ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .height(200.dp)
                            ) {
                                if (isVideo) {
                                    // Display video player
                                    VideoPlayer(
                                        videoUrl = uri.toString(),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    // Display image
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(uri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Selected image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                }

                                // Add remove button
                                IconButton(
                                    onClick = { selectedImageUri = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(36.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove media",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Show upload progress if uploading
                        if (uiState.value.uploadingMedia) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.uploading),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LocalCustomColors.current.textPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                modifier = Modifier.padding(start = 8.dp),
                                onClick = {
                                    if (!hasStoragePermission) {
                                        requestPermissions()
                                    }
                                    showMediaPicker = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add media",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Button(
                                onClick = {
                                    if (postText.isNotBlank() || selectedImageUri != null) {
                                        isCreatingPost = true
                                        postViewModel.createPost(
                                            context,
                                            postText,
                                            selectedImageUri,
                                            isVideo,
                                            selectedVisibility
                                        )
                                        postText = ""
                                        selectedImageUri = null
                                        isVideo = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LocalCustomColors.current.secondaryBackground,
                                    disabledContainerColor = LocalCustomColors.current.secondaryBackground,
                                ),
                                enabled = (postText.isNotBlank() || selectedImageUri != null) &&
                                        !uiState.value.uploadingMedia
                            ) {
                                Text(
                                    stringResource(R.string.post),
                                    color = LocalCustomColors.current.textPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showError) {
            AlertDialog(
                onDismissRequest = { showError = false },
                title = { Text(stringResource(R.string.error_dialog_title)) },
                text = { Text(errorMessage) },
                confirmButton = {
                    TextButton(onClick = { showError = false }) {
                        Text(
                            stringResource(R.string.ok),
                            color = LocalCustomColors.current.textPrimary
                        )
                    }
                }
            )
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

        // Edit Post Dialog
        if (showEditPostDialog && postToEdit != null) {
            AlertDialog(
                onDismissRequest = { showEditPostDialog = false },
                title = { Text(stringResource(R.string.edit_post)) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editPostText,
                            onValueChange = { editPostText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.whats_on_your_mind)) },
                            maxLines = 5,
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.post_visibility_label),
                                color = LocalCustomColors.current.textSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Box {
                                var expandEditVisibilityDropdown by remember { mutableStateOf(false) }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .clickable { expandEditVisibilityDropdown = true }
                                ) {
                                    Icon(
                                        painter = when (editPostVisibility) {
                                            PostVisibility.PUBLIC -> painterResource(id = R.drawable.ic_language)
                                            PostVisibility.FRIENDS_ONLY -> painterResource(id = R.drawable.ic_friends)
                                            PostVisibility.PRIVATE -> painterResource(id = R.drawable.ic_lock)
                                        },
                                        contentDescription = "Visibility",
                                        tint = LocalCustomColors.current.textPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    Text(
                                        text = when (editPostVisibility) {
                                            PostVisibility.PUBLIC -> stringResource(id = R.string.post_visibility_public)
                                            PostVisibility.FRIENDS_ONLY -> stringResource(id = R.string.post_visibility_friends)
                                            PostVisibility.PRIVATE -> stringResource(id = R.string.post_visibility_private)
                                        },
                                        color = LocalCustomColors.current.textPrimary
                                    )
                                }

                                DropdownMenu(
                                    expanded = expandEditVisibilityDropdown,
                                    onDismissRequest = { expandEditVisibilityDropdown = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_language),
                                                    contentDescription = "Public",
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(stringResource(id = R.string.post_visibility_public))
                                            }
                                        },
                                        onClick = {
                                            editPostVisibility = PostVisibility.PUBLIC
                                            expandEditVisibilityDropdown = false
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_friends),
                                                    contentDescription = "Friends only",
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(stringResource(id = R.string.post_visibility_friends))
                                            }
                                        },
                                        onClick = {
                                            editPostVisibility = PostVisibility.FRIENDS_ONLY
                                            expandEditVisibilityDropdown = false
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_lock),
                                                    contentDescription = "Only me",
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(stringResource(id = R.string.post_visibility_private))
                                            }
                                        },
                                        onClick = {
                                            editPostVisibility = PostVisibility.PRIVATE
                                            expandEditVisibilityDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            postToEdit?.let { post ->
                                postViewModel.updatePost(post.id, editPostText, editPostVisibility)
                            }
                            showEditPostDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LocalCustomColors.current.secondaryBackground
                        ),
                        enabled = editPostText.isNotBlank()
                    ) {
                        Text(
                            stringResource(R.string.save),
                            color = LocalCustomColors.current.textPrimary
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showEditPostDialog = false }
                    ) {
                        Text(
                            stringResource(R.string.cancel),
                            color = LocalCustomColors.current.textSecondary
                        )
                    }
                }
            )
        }
    }
}