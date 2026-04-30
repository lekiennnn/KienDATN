package com.example.nam.ui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nam.R
import com.example.nam.ui.notification.NotificationState
import com.example.nam.ui.notification.NotificationViewModel
import com.example.nam.ui.theme.LocalCustomColors
import kotlinx.coroutines.delay

@Composable
fun NotificationBadge(
    onClick: () -> Unit,
    viewModel: NotificationViewModel = viewModel()
) {
    val notificationsState = viewModel.uiState.collectAsStateWithLifecycle().value
    var unreadCount by remember { mutableStateOf(0) }
    
    LaunchedEffect(notificationsState) {
        val state = notificationsState.notificationsState
        if (state is NotificationState.Success) {
            unreadCount = state.notifications.count { !it.isRead }
        }
    }
    
    // Periodically refresh notifications
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000) // Check for new notifications every minute
            viewModel.loadNotifications()
        }
    }
    
    Box(contentAlignment = Alignment.Center) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications"
            )
        }
        
        // Show badge if there are unread notifications
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color.Red),
                contentAlignment = Alignment.Center
            ) {
                if (unreadCount < 10) {
                    Text(
                        text = unreadCount.toString(),
                        color = LocalCustomColors.current.textPrimary,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = stringResource(R.string.badge_more_than_nine),
                        color = LocalCustomColors.current.textPrimary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}