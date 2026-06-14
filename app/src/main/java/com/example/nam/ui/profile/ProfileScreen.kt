package com.example.nam.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nam.R
import com.example.nam.data.Post
import com.example.nam.data.PostVisibility
import com.example.nam.service.ImageUploadState
import com.example.nam.ui.media.mediapicker.MediaPicker
import com.example.nam.ui.post.PostItem
import com.example.nam.ui.post.PostViewModel
import com.example.nam.ui.theme.LocalCustomColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    postViewModel: PostViewModel,
    onBackClick: () -> Unit = {},
    profileViewModel: ProfileViewModel = viewModel(),
    userId: String? = null
) {
    val isOwnProfile = userId == null
    val uiState = profileViewModel.uiState.collectAsStateWithLifecycle().value
    val (isUpdatingImage, setIsUpdatingImage) = remember { mutableStateOf(false) }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val selectedImageUri = remember { mutableStateOf<Uri?>(null) }
    var showMediaPicker by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState()

    var showEditOptions by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var newDisplayName by rememberSaveable { mutableStateOf("") }

    var hasStoragePermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES
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
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasStoragePermission = isGranted
        if (!isGranted) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Storage permission is required to access images")
            }
        }
    }

    val requestPermission = {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        permissionLauncher.launch(permission)
    }

    val currentUser = (uiState.profileState as? ProfileState.Success)?.user

    fun updateDisplayName(name: String) {
        if (name.isNotBlank() && currentUser != null) {
            profileViewModel.updateProfile(
                displayName = name,
                bio = currentUser.bio,
                photoUri = null
            )
        }
        showNameDialog = false
    }

    LaunchedEffect(Unit) {
        if (userId == null) {
            profileViewModel.loadUserProfile()
            profileViewModel.loadUserPosts()
        } else if (userId.isNotEmpty()) {
            profileViewModel.loadUserProfile(userId)
            profileViewModel.loadUserPosts(userId)
        } else {
            // Handle the case where userId is empty
            profileViewModel.loadUserProfile() // Fallback to loading own profile
            profileViewModel.loadUserPosts()   // Fallback to loading own posts
        }
    }

    LaunchedEffect(currentUser) {
        currentUser?.let {
            newDisplayName = it.displayName
        }
    }

    LaunchedEffect(uiState.profileState) {
        if (uiState.profileState is ProfileState.Success) {
            setIsUpdatingImage(false)
        }
    }

    LaunchedEffect(uiState.imageUploadState) {
        when (uiState.imageUploadState) {
            is ImageUploadState.Success -> {
                val imageUrl = (uiState.imageUploadState as ImageUploadState.Success).imageUrl
                profileViewModel.updateUserWithCloudinaryUrl(imageUrl)
            }

            is ImageUploadState.Error -> {
                val errorMsg = (uiState.imageUploadState as ImageUploadState.Error).message
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Failed to upload image: $errorMsg")
                }
                setIsUpdatingImage(false)
            }

            else -> {}
        }
    }

    LaunchedEffect(uiState.profileUpdated) {
        if (uiState.profileUpdated) {
            postViewModel.loadPosts()
            profileViewModel.resetProfileUpdatedFlag()
        }
    }

    LaunchedEffect(uiState.friendshipStatus) {
        postViewModel.loadPosts()
    }

    var showEditPostDialog by remember { mutableStateOf(false) }
    var postToEdit by remember { mutableStateOf<Post?>(null) }
    var editPostText by remember { mutableStateOf("") }
    var editPostVisibility by remember { mutableStateOf(PostVisibility.PUBLIC) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isOwnProfile) stringResource(R.string.my_profile) else stringResource(R.string.profile),
                        color = LocalCustomColors.current.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (userId == null) {
                            profileViewModel.loadUserProfile()
                        } else if (userId.isNotEmpty()) {
                            profileViewModel.loadUserProfile(userId)
                        } else {
                            profileViewModel.loadUserProfile()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Profile"
                        )
                    }
                    if (isOwnProfile) {
                        IconButton(onClick = { showEditOptions = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LocalCustomColors.current.navBarsBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState.profileState) {
                is ProfileState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is ProfileState.Success -> {
                    val user = uiState.profileState.user
                    val postCount = uiState.userPostsState.let {
                        if (it is UserPostsState.Success) it.posts.size.toString() else "0"
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            ProfileHeader(
                                user = user,
                                postCount = postCount,
                                isLoading = isUpdatingImage,
                                isOwnProfile = isOwnProfile,
                                onImageClick = {
                                    if (isOwnProfile) {
                                        if (!hasStoragePermission) requestPermission()
                                        showMediaPicker = true
                                        showEditOptions = false
                                    }
                                },
                                friendshipStatus = uiState.friendshipStatus,
                                isFriendRequestInProgress = uiState.isFriendRequestInProgress,
                                onAddFriendClick = {
                                    userId?.let { profileViewModel.sendFriendRequest(it) }
                                },
                                onRemoveFriendClick = {
                                    userId?.let { profileViewModel.removeFriend(it) }
                                },
                                onCancelRequestClick = {
                                    userId?.let { profileViewModel.cancelFriendRequest(it) }
                                },
                                isRequestSender = uiState.isRequestSender
                            )

                            HorizontalDivider(
                                color = Color.LightGray,
                                thickness = 4.dp,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )

                            Text(
                                text = stringResource(R.string.my_posts_and_shares),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = LocalCustomColors.current.textPrimary
                            )
                        }

                        when (uiState.userPostsState) {
                            is UserPostsState.Loading -> {
                                Log.d("DCM", "ProfileScreen: LOADING")
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }

                            is UserPostsState.Success -> {
                                Log.d("DCM", "ProfileScreen: SUCCESS")
                                val posts = uiState.userPostsState.posts
                                Log.d("DCM", "ProfileScreen: ${posts.size}")
                                if (posts.isEmpty()) {
                                    Log.d("DCM", "ProfileScreen ")
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (isOwnProfile) stringResource(R.string.no_posts_yet)
                                                else stringResource(R.string.user_no_posts_yet),
                                                color = LocalCustomColors.current.textPrimary
                                            )
                                        }
                                    }
                                } else {
                                    items(posts) { post ->
                                        Log.d("DCM", "ProfileScreen: ${post.text}")
                                        PostItem(
                                            post = post,
                                            postViewModel = postViewModel,
                                            onEditClick = { post ->
                                                postToEdit = post
                                                editPostText = post.text
                                                editPostVisibility = post.visibility
                                                showEditPostDialog = true
                                            }
                                        )
                                    }
                                }
                            }

                            is UserPostsState.Error -> {
                                Log.d("DCM", "ProfileScreen: ERROR")
                                item {
                                    Text(
                                        text = "Error: ${uiState.userPostsState.message}",
                                        color = Color.Red,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }

                            null -> {
                                item {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                is ProfileState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error loading profile: ${uiState.profileState.message}",
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        IconButton(
                            onClick = { profileViewModel.loadUserProfile() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry"
                            )
                        }
                    }
                }

                null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }

    if (showEditOptions) {
        ModalBottomSheet(
            onDismissRequest = { showEditOptions = false },
            sheetState = bottomSheetState,
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    stringResource(R.string.edit_profile),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = LocalCustomColors.current.textPrimary
                )
                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(R.string.change_profile_picture),
                            color = LocalCustomColors.current.textPrimary
                        )
                    },
                    modifier = Modifier.clickable {
                        if (!hasStoragePermission) requestPermission()
                        showMediaPicker = true
                        showEditOptions = false
                    }
                )
                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(R.string.change_display_name),
                            color = LocalCustomColors.current.textPrimary
                        )
                    },
                    modifier = Modifier.clickable {
                        showEditOptions = false
                        showNameDialog = true
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
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
                onMediaSelected = { uri, _ ->
                    selectedImageUri.value = uri
                    profileViewModel.updateProfilePictureWithCloudinary(context, uri)
                    setIsUpdatingImage(true)
                    showMediaPicker = false
                },
                onDismiss = { showMediaPicker = false },
                hasPermission = hasStoragePermission,
                requestPermission = requestPermission,
                includeVideos = false
            )
        }
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = {
                Text(
                    stringResource(R.string.change_display_name),
                    color = LocalCustomColors.current.textPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = newDisplayName,
                    onValueChange = { newDisplayName = it },
                    label = {
                        Text(
                            stringResource(R.string.display_name),
                            color = LocalCustomColors.current.textPrimary
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { updateDisplayName(newDisplayName) }
                ) {
                    Text(
                        stringResource(R.string.save),
                        color = LocalCustomColors.current.textPrimary
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showNameDialog = false }
                ) {
                    Text(
                        stringResource(R.string.cancel),
                        color = LocalCustomColors.current.textPrimary
                    )
                }
            }
        )
    }

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

                    // Visibility Dropdown
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
                TextButton(
                    onClick = {
                        postToEdit?.let { post ->
                            postViewModel.updatePost(post.id, editPostText, editPostVisibility)
                            // Also reload user posts to reflect the changes
                            if (userId == null) {
                                profileViewModel.loadUserPosts()
                            } else if (userId.isNotEmpty()) {
                                profileViewModel.loadUserPosts(userId)
                            } else {
                                profileViewModel.loadUserPosts() // Fallback to own posts if userId is empty
                            }
                        }
                        showEditPostDialog = false
                    },
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
                        color = LocalCustomColors.current.textPrimary
                    )
                }
            }
        )
    }
}
