package com.example.nam.ui.friends

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nam.data.Friendship
import com.example.nam.data.User
import com.example.nam.ui.theme.LocalCustomColors
import com.example.nam.utils.Navigation
import com.google.firebase.auth.FirebaseAuth
import com.example.nam.R

@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel = viewModel(),
    navController: NavController? = null
) {
    var selectedTab by remember { mutableStateOf(FriendsTab.FRIENDS) }
    var searchQuery by remember { mutableStateOf("") }
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                viewModel.searchUsers(it)
            },
            placeholder = { Text(stringResource(R.string.search_users)) },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        viewModel.searchUsers("")
                    }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear search"
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { viewModel.searchUsers(searchQuery) }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Only show tabs if not searching
        if (uiState.searchState !is SearchState.Success && uiState.searchState !is SearchState.Loading) {
            // Tabs for different sections
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                FriendsTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.title, color = LocalCustomColors.current.textPrimary) }
                    )
                }
            }

            // Content based on selected tab
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                when (selectedTab) {
                    FriendsTab.FRIENDS -> FriendsTabContent(
                        friendsState = uiState.friendsState,
                        onRemoveFriend = { viewModel.removeFriend(it) },
                        onRetry = { viewModel.loadFriends() },
                        navController = navController
                    )

                    FriendsTab.REQUESTS -> RequestsTabContent(
                        pendingRequestsState = uiState.pendingRequestsState,
                        onAccept = { viewModel.acceptFriendRequest(it) },
                        onDecline = { viewModel.declineFriendRequest(it) },
                        viewModel = viewModel,
                        onRetry = { viewModel.loadPendingRequests() },
                        navController = navController
                    )

                    FriendsTab.SENT -> SentRequestsTabContent(
                        sentRequestsState = uiState.sentRequestsState,
                        viewModel = viewModel,
                        onRetry = { viewModel.loadSentRequests() },
                        navController = navController
                    )
                }
            }
        } else {
            // Show search results
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                when (uiState.searchState) {
                    is SearchState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    is SearchState.Success -> {
                        val users = uiState.searchState.users
                        if (users.isEmpty()) {
                            Text(
                                text = stringResource(R.string.no_users_found),
                                modifier = Modifier.align(Alignment.Center),
                                color = LocalCustomColors.current.textPrimary
                            )
                        } else {
                            LazyColumn {
                                items(users) { user ->
                                    UserSearchItem(
                                        user = user,
                                        onSendRequest = { viewModel.sendFriendRequest(user.id) },
                                        navController = navController
                                    )
                                }
                            }
                        }
                    }

                    is SearchState.Error -> {
                        Text(
                            text = stringResource(
                                R.string.error_prefix,
                                uiState.searchState.message
                            ),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    else -> {
                        // Initial state or null state
                        if (searchQuery.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.type_to_search),
                                modifier = Modifier.align(Alignment.Center),
                                color = LocalCustomColors.current.textPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FriendsTabContent(
    friendsState: FriendsState?,
    onRemoveFriend: (String) -> Unit,
    onRetry: () -> Unit = {},
    navController: NavController? = null
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (friendsState) {
            is FriendsState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is FriendsState.Success -> {
                val friendships = friendsState.friendships
                if (friendships.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_friends),
                        modifier = Modifier.align(Alignment.Center),
                        color = LocalCustomColors.current.textPrimary
                    )
                } else {
                    LazyColumn {
                        items(friendships) { friendship ->
                            FriendItem(
                                friendship = friendship,
                                onRemoveFriend = { onRemoveFriend(friendship.id) },
                                navController = navController
                            )
                        }
                    }
                }
            }

            is FriendsState.Error -> {
                Text(
                    text = stringResource(R.string.error_prefix, friendsState.message),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center),

                )
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 48.dp)
                ) {
                    Text(
                        stringResource(R.string.retry),
                        color = LocalCustomColors.current.textPrimary
                    )
                }
            }

            null -> {
                // Handle null state
            }
        }
    }
}

@Composable
fun RequestsTabContent(
    pendingRequestsState: FriendsState?,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
    viewModel: FriendsViewModel,
    onRetry: () -> Unit = { viewModel.loadPendingRequests() },
    navController: NavController? = null
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (pendingRequestsState) {
            is FriendsState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is FriendsState.Success -> {
                val requests = pendingRequestsState.friendships
                if (requests.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_pending_requests),
                        modifier = Modifier.align(Alignment.Center),
                        color = LocalCustomColors.current.textPrimary
                    )
                } else {
                    LazyColumn {
                        items(requests) { request ->
                            FriendRequestItem(
                                friendship = request,
                                onAccept = { onAccept(request.id) },
                                onDecline = { onDecline(request.id) },
                                isAccepting = viewModel.isAcceptingRequest(request.id),
                                isDeclining = viewModel.isDecliningRequest(request.id),
                                navController = navController
                            )
                        }
                    }
                }
            }

            is FriendsState.Error -> {
                Text(
                    text = stringResource(R.string.error_prefix, pendingRequestsState.message),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 48.dp)
                ) {
                    Text(
                        stringResource(R.string.retry),
                        color = LocalCustomColors.current.textPrimary
                    )
                }
            }

            null -> {
                // Handle null state
            }
        }
    }
}

@Composable
fun SentRequestsTabContent(
    sentRequestsState: FriendsState?,
    viewModel: FriendsViewModel,
    onRetry: () -> Unit = { viewModel.loadSentRequests() },
    navController: NavController? = null
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (sentRequestsState) {
            is FriendsState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is FriendsState.Success -> {
                val requests = sentRequestsState.friendships
                if (requests.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_sent_requests),
                        modifier = Modifier.align(Alignment.Center),
                        color = LocalCustomColors.current.textPrimary
                    )
                } else {
                    LazyColumn {
                        items(requests) { request ->
                            SentRequestItem(
                                friendship = request,
                                onCancelRequest = { viewModel.cancelFriendRequest(request.id) },
                                isCanceling = viewModel.isCancelingRequest(request.id),
                                navController = navController
                            )
                        }
                    }
                }
            }

            is FriendsState.Error -> {
                Text(
                    text = stringResource(R.string.error_prefix, sentRequestsState.message),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 48.dp)
                ) {
                    Text(
                        stringResource(R.string.retry),
                        color = LocalCustomColors.current.textPrimary
                    )
                }
            }

            null -> {
                // Handle null state
            }
        }
    }
}

@Composable
fun FriendItem(
    friendship: Friendship,
    onRemoveFriend: () -> Unit,
    navController: NavController? = null
) {
    // Determine if current user is sender or receiver
    val isCurrentUserSender =
        FirebaseAuth.getInstance().currentUser?.uid == friendship.senderId
    val friendName = if (isCurrentUserSender) friendship.receiverName else friendship.senderName
    val friendPhotoUrl =
        if (isCurrentUserSender) friendship.receiverPhotoUrl else friendship.senderPhotoUrl
    val friendId = if (isCurrentUserSender) friendship.receiverId else friendship.senderId

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                if (navController != null) {
                    Navigation.navigateToUserProfile(friendId, navController)
                }
            },
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        color = LocalCustomColors.current.secondaryBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .background(LocalCustomColors.current.secondaryBackground),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile image
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))

            ) {
                if (!friendPhotoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = friendPhotoUrl,
                        contentDescription = "Profile picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Fallback for no profile image
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Name
            Text(
                text = friendName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                color = LocalCustomColors.current.textPrimary
            )

            // Remove button
            var showConfirmDialog by remember { mutableStateOf(false) }
            if (showConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmDialog = false },
                    title = {
                        Text(
                            stringResource(R.string.remove_friend),
                            color = LocalCustomColors.current.textPrimary
                        )
                    },
                    text = {
                        val friendName =
                            if (friendship.senderId == com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid)
                                friendship.receiverName else friendship.senderName
                        Text(
                            stringResource(R.string.remove_friend_confirmation, friendName),
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onRemoveFriend()
                                showConfirmDialog = false
                            }
                        ) {
                            Text(
                                stringResource(R.string.remove),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmDialog = false }) {
                            Text(
                                stringResource(R.string.cancel),
                                color = LocalCustomColors.current.textPrimary
                            )
                        }
                    }
                )
            }
            IconButton(onClick = { showConfirmDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Remove friend",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun FriendRequestItem(
    friendship: Friendship,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    isAccepting: Boolean = false,
    isDeclining: Boolean = false,
    navController: NavController? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                if (navController != null) {
                    Navigation.navigateToUserProfile(friendship.senderId, navController)
                }
            },
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile image
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                if (!friendship.senderPhotoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = friendship.senderPhotoUrl,
                        contentDescription = "Profile picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Fallback for no profile image
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = friendship.senderName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        if (navController != null) {
                            Navigation.navigateToUserProfile(friendship.senderId, navController)
                        }
                    },
                    color = LocalCustomColors.current.textPrimary
                )

                Text(
                    text = stringResource(R.string.sent_friend_request),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalCustomColors.current.textPrimary
                )
            }

            // Buttons
            Row {
                // Accept button
                if (isAccepting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LocalCustomColors.current.secondaryBackground,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(end = 8.dp)

                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Accept",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }

                // Decline button
                if (isDeclining) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Button(
                        onClick = onDecline,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Decline",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SentRequestItem(
    friendship: Friendship,
    onCancelRequest: () -> Unit,
    isCanceling: Boolean = false,
    navController: NavController? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                if (navController != null) {
                    Navigation.navigateToUserProfile(friendship.receiverId, navController)
                }
            },
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile image
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))

            ) {
                if (!friendship.receiverPhotoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = friendship.receiverPhotoUrl,
                        contentDescription = "Profile picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Fallback for no profile image
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = friendship.receiverName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        if (navController != null) {
                            Navigation.navigateToUserProfile(friendship.receiverId, navController)
                        }
                    },
                    color = LocalCustomColors.current.textPrimary
                )

                Text(
                    text = stringResource(R.string.request_pending),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalCustomColors.current.textPrimary
                )
            }

            // Pending indicator and cancel button
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCanceling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Button(
                        onClick = onCancelRequest,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(
                            stringResource(R.string.cancel),
                            color = LocalCustomColors.current.textPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserSearchItem(
    user: User,
    onSendRequest: () -> Unit,
    navController: NavController? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile image
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    .clickable {
                        if (navController != null) {
                            Navigation.navigateToUserProfile(user.id, navController)
                        }
                    }
            ) {
                if (user.photoUrl != null) {
                    AsyncImage(
                        model = user.photoUrl,
                        contentDescription = "Profile picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Fallback for no profile image
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        if (navController != null) {
                            Navigation.navigateToUserProfile(user.id, navController)
                        }
                    },
                    color = LocalCustomColors.current.textPrimary
                )

                if (user.bio != null) {
                    Text(
                        text = user.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalCustomColors.current.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Add friend button (if not already friends/request sent)
            if (!user.isFollowedByCurrentUser) {
                if (user.isLoading) {
                    // Show loading indicator
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Button(
                        onClick = onSendRequest,
                        modifier = Modifier.wrapContentSize(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LocalCustomColors.current.secondaryBackground,
                        ),
                    ) {
                        Text(
                            stringResource(R.string.add_friend),
                            color = LocalCustomColors.current.textPrimary
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.request_sent),
                    color = LocalCustomColors.current.textPrimary
                )
            }
        }
    }
}

enum class FriendsTab(@StringRes val titleResId: Int) {
    FRIENDS(R.string.tab_friends),
    REQUESTS(R.string.tab_requests),
    SENT(R.string.tab_sent);

    val title: String
        @Composable
        get() = stringResource(titleResId)
}