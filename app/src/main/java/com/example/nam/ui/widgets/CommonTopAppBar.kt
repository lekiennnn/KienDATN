package com.example.nam.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.nam.R
import com.example.nam.data.User
import com.example.nam.ui.notification.NotificationViewModel
import com.example.nam.ui.post.PostViewModel
import com.example.nam.ui.theme.LocalCustomColors
import kotlinx.coroutines.CoroutineScope
import com.example.nam.utils.openDrawer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonTopAppBar(
    title: String,
    drawerState: DrawerState,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
    user: User,
    actions: @Composable (RowScope.() -> Unit) = {},
    isPostScreen: Boolean = false,
    isNotProfileScreen: Boolean = true,
    postViewModel: PostViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel(),
    isNotificationScreen: Boolean = false
) {
    if (isNotProfileScreen) {
        TopAppBar(
            title = { Text(title, color = LocalCustomColors.current.textPrimary) },
            navigationIcon = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(LocalCustomColors.current.navBarsBackground)
                        .clickable { openDrawer(coroutineScope, drawerState) }
                ) {
                    if (user.photoUrl != null) {
                        AsyncImage(
                            model = user.photoUrl,
                            contentDescription = "Profile picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            },
            actions = {
                if (isPostScreen) {
                    IconButton(onClick = { postViewModel.reloadPosts() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload")
                    }
                }

                if (isNotificationScreen) {
                    TextButton(
                        onClick = { notificationViewModel.markAllAsRead() }
                    ) {
                        Text(text = stringResource(R.string.mark_all_as_read))
                    }
                }
            },
            windowInsets = WindowInsets(top = 0.dp),
            modifier = modifier
        )
    }
}