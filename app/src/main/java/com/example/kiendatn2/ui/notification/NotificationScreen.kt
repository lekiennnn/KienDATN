package com.example.kiendatn2.ui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.kiendatn2.R
import com.example.kiendatn2.data.Notification
import com.example.kiendatn2.data.NotificationType
import com.example.kiendatn2.ui.theme.LocalCustomColors
import com.example.kiendatn2.utils.Navigation
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationScreen(
    viewModel: NotificationViewModel = viewModel(),
    onPostClick: (String) -> Unit,
    navController: NavController? = null
) {
    val notificationsState = viewModel.uiState.collectAsStateWithLifecycle().value
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        when (notificationsState.notificationsState) {
            is NotificationState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is NotificationState.Success -> {
                val notifications = notificationsState.notificationsState.notifications
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = when (notificationsState.currentFilter) {
                                        NotificationDateFilter.RECENT -> stringResource(R.string.recent)
                                        NotificationDateFilter.ALL -> stringResource(R.string.all)
                                        NotificationDateFilter.WEEK -> stringResource(R.string.last_week)
                                        NotificationDateFilter.MONTH -> stringResource(R.string.last_month)
                                    }
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            }

                            DropdownMenu(
                                modifier = Modifier.padding(8.dp),
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.recent)) },
                                    onClick = {
                                        viewModel.setDateFilter(NotificationDateFilter.RECENT)
                                        expanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.last_week)) },
                                    onClick = {
                                        viewModel.setDateFilter(NotificationDateFilter.WEEK)
                                        expanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.last_month)) },
                                    onClick = {
                                        viewModel.setDateFilter(NotificationDateFilter.MONTH)
                                        expanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.all)) },
                                    onClick = {
                                        viewModel.setDateFilter(NotificationDateFilter.ALL)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (notifications.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_notifications),
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(notifications) { notification ->
                                NotificationItem(
                                    notification = notification,
                                    onClick = {
                                        viewModel.markAsRead(notification.id)
                                        when (notification.type) {
                                            NotificationType.FRIEND_REQUEST, NotificationType.FRIEND_ACCEPTED -> {
                                                if (navController != null) {
                                                    Navigation.navigateToUserProfile(
                                                        notification.senderId,
                                                        navController
                                                    )
                                                }
                                            }

                                            NotificationType.LIKE, NotificationType.COMMENT, NotificationType.REPLY -> {
                                                notification.postId.let { postId ->
                                                    onPostClick(postId)
                                                }
                                            }
                                        }
                                    },
                                    onProfileClick = {
                                        if (navController != null) {
                                            Navigation.navigateToUserProfile(
                                                notification.senderId,
                                                navController
                                            )
                                        }
                                    },
                                    navController = navController
                                )
                            }
                        }
                    }
                }
            }

            is NotificationState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(
                            R.string.error,
                            (notificationsState.notificationsState as NotificationState.Error).message
                        ),
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadNotifications() }
                    ) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }

            null -> {
                // No state yet
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    navController: NavController? = null
) {
    val backgroundColor = if (!notification.isRead) {
        LocalCustomColors.current.secondaryBackground
    } else {
        LocalCustomColors.current.secondaryBackground.copy(alpha = 0.2f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        color = backgroundColor,
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium
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
                        onProfileClick()
                    }
            ) {
                if (notification.senderProfileImageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = notification.senderProfileImageUrl,
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

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Create notification message based on type
                val message = when (notification.type) {
                    NotificationType.LIKE -> "${notification.senderName} ${stringResource(R.string.liked_your_post)}"
                    NotificationType.COMMENT -> "${notification.senderName} ${stringResource(R.string.commented_on_your_post)}"
                    NotificationType.REPLY -> "${notification.senderName} ${stringResource(R.string.replied_to_your_comment)}"
                    NotificationType.FRIEND_REQUEST -> "${notification.senderName} ${
                        stringResource(
                            R.string.sent_you_a_friend_request
                        )
                    }"

                    NotificationType.FRIEND_ACCEPTED -> "${notification.senderName} ${
                        stringResource(
                            R.string.accepted_your_friend_request
                        )
                    }"
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notification.senderName,
                        style = if (notification.isRead)
                            MaterialTheme.typography.bodyMedium
                        else
                            MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        color = LocalCustomColors.current.textPrimary,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable {
                            if (navController != null) {
                                Navigation.navigateToUserProfile(
                                    notification.senderId,
                                    navController
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = when (notification.type) {
                            NotificationType.LIKE -> stringResource(R.string.liked_your_post)
                            NotificationType.COMMENT -> stringResource(R.string.commented_on_your_post)
                            NotificationType.REPLY -> stringResource(R.string.replied_to_your_comment)
                            NotificationType.FRIEND_REQUEST -> stringResource(R.string.sent_you_a_friend_request)
                            NotificationType.FRIEND_ACCEPTED -> stringResource(R.string.accepted_your_friend_request)
                        },
                        style = if (notification.isRead)
                            MaterialTheme.typography.bodyMedium
                        else
                            MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = LocalCustomColors.current.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatTimestamp(notification.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

// Format timestamp to a readable string
fun formatTimestamp(timestamp: Timestamp): String {
    val now = Calendar.getInstance()
    val notificationTime = Calendar.getInstance()
    notificationTime.time = timestamp.toDate()

    return when {
        // Today
        now.get(Calendar.DATE) == notificationTime.get(Calendar.DATE) &&
                now.get(Calendar.MONTH) == notificationTime.get(Calendar.MONTH) &&
                now.get(Calendar.YEAR) == notificationTime.get(Calendar.YEAR) -> {
            val format = SimpleDateFormat("h:mm a", Locale.getDefault())
            "Today at ${format.format(timestamp.toDate())}"
        }
        // Yesterday
        now.get(Calendar.DATE) - notificationTime.get(Calendar.DATE) == 1 &&
                now.get(Calendar.MONTH) == notificationTime.get(Calendar.MONTH) &&
                now.get(Calendar.YEAR) == notificationTime.get(Calendar.YEAR) -> {
            val format = SimpleDateFormat("h:mm a", Locale.getDefault())
            "Yesterday at ${format.format(timestamp.toDate())}"
        }
        // This week
        now.get(Calendar.WEEK_OF_YEAR) == notificationTime.get(Calendar.WEEK_OF_YEAR) &&
                now.get(Calendar.YEAR) == notificationTime.get(Calendar.YEAR) -> {
            val format = SimpleDateFormat("EEE h:mm a", Locale.getDefault())
            format.format(timestamp.toDate())
        }
        // This year
        now.get(Calendar.YEAR) == notificationTime.get(Calendar.YEAR) -> {
            val format = SimpleDateFormat("MMM d h:mm a", Locale.getDefault())
            format.format(timestamp.toDate())
        }
        // Older
        else -> {
            val format = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
            format.format(timestamp.toDate())
        }
    }
}