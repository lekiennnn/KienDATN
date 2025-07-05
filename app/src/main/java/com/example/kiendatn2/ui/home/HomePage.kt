package com.example.kiendatn2.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.kiendatn2.R
import com.example.kiendatn2.data.User
import com.example.kiendatn2.ui.auth.AuthState
import com.example.kiendatn2.ui.auth.AuthViewModel
import com.example.kiendatn2.ui.drawer.DrawerNavigationItem
import com.example.kiendatn2.ui.drawer.NavigationDrawer
import com.example.kiendatn2.ui.friends.FriendsScreen
import com.example.kiendatn2.ui.friends.FriendsViewModel
import com.example.kiendatn2.ui.notification.NotificationScreen
import com.example.kiendatn2.ui.notification.NotificationViewModel
import com.example.kiendatn2.ui.post.PostScreen
import com.example.kiendatn2.ui.post.PostViewModel
import com.example.kiendatn2.ui.profile.ProfileState
import com.example.kiendatn2.ui.profile.ProfileViewModel
import com.example.kiendatn2.ui.search.SearchScreen
import com.example.kiendatn2.ui.theme.LocalCustomColors
import com.example.kiendatn2.ui.widgets.CommonTopAppBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    postViewModel: PostViewModel,
    notificationViewModel: NotificationViewModel,
    profileViewModel: ProfileViewModel = viewModel(),
    friendsViewModel: FriendsViewModel = viewModel(),
) {
    val authState = authViewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = 0) { 4 }
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val profileState = profileViewModel.uiState.collectAsStateWithLifecycle()
    val profileUpdated = profileViewModel.uiState.collectAsStateWithLifecycle().value.profileUpdated
    val showCreatePost = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        profileViewModel.loadUserProfile()
    }

    LaunchedEffect(authState.value) {
        when (authState.value.authState) {
            is AuthState.Unauthenticated -> navController.navigate("login")
            else -> Unit
        }
    }

    LaunchedEffect(profileUpdated) {
        if (profileUpdated) {
            postViewModel.loadPosts()
            postViewModel.loadFriendsPosts()
            profileViewModel.resetProfileUpdatedFlag()
        }
    }

    val user = if (profileState.value.profileState is ProfileState.Success) {
        (profileState.value.profileState as ProfileState.Success).user
    } else {
        User(
            id = "",
            email = "",
            displayName = "User",
            photoUrl = null,
            bio = null
        )
    }
    val handleDrawerItemSelected = { item: DrawerNavigationItem ->
        coroutineScope.launch {
            drawerState.close()
        }

        when (item) {
            DrawerNavigationItem.Profile -> {
                navController.navigate("profile")
            }

//            DrawerNavigationItem.Language -> {
//                navController.navigate("language")
//            }

            DrawerNavigationItem.Settings -> {
                navController.navigate("settings")
            }
        }
    }

    NavigationDrawer(
        modifier = Modifier,
        drawerState = drawerState,
        onSignOutClick = {
            // Sign out the user
            authViewModel.signout()
            // Close the drawer
            coroutineScope.launch {
                drawerState.close()
            }
        },
        onItemSelected = handleDrawerItemSelected
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    {
                        when (pagerState.currentPage) {
                            0 -> CommonTopAppBar(
                                title = stringResource(R.string.posts),
                                drawerState = drawerState,
                                coroutineScope = coroutineScope,
                                user = user,
                                isPostScreen = true,
                            )

                            1 -> CommonTopAppBar(
                                title = stringResource(R.string.search),
                                drawerState = drawerState,
                                coroutineScope = coroutineScope,
                                user = user
                            )

                            2 -> CommonTopAppBar(
                                title = stringResource(R.string.friends),
                                drawerState = drawerState,
                                coroutineScope = coroutineScope,
                                user = user
                            )

                            3 -> CommonTopAppBar(
                                title = stringResource(R.string.notifications),
                                drawerState = drawerState,
                                coroutineScope = coroutineScope,
                                user = user,
                                isNotificationScreen = true,
                            )

                            else -> CommonTopAppBar(
                                title = stringResource(R.string.posts),
                                drawerState = drawerState,
                                coroutineScope = coroutineScope,
                                user = user
                            )
                        }
                    }
                )
            },
            bottomBar = {
                Box(contentAlignment = Alignment.TopCenter) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    Icons.Filled.Home,
                                    contentDescription = "Home",
                                    tint = LocalCustomColors.current.iconColor
                                )
                            },
                            selected = pagerState.currentPage == 0,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.scrollToPage(0)
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = LocalCustomColors.current.iconColor,
                                indicatorColor = LocalCustomColors.current.isSelectedBackground
                            )
                        )

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = "Profile",
                                    tint = LocalCustomColors.current.iconColor
                                )
                            },
                            selected = pagerState.currentPage == 1,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.scrollToPage(1)
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = LocalCustomColors.current.iconColor,
                                indicatorColor = LocalCustomColors.current.isSelectedBackground
                            )
                        )

                        // Empty space for FAB
                        NavigationBarItem(
                            icon = { Box(modifier = Modifier.size(24.dp)) { } },
                            selected = false,
                            onClick = { },
                            enabled = false,
                            modifier = Modifier.background(Color.Transparent)
                        )

                        NavigationBarItem(
                            icon = {
                                Image(
                                    painter = painterResource(R.drawable.ic_friends),
                                    contentDescription = "Friends",
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            selected = pagerState.currentPage == 2,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.scrollToPage(2)
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = LocalCustomColors.current.iconColor,
                                indicatorColor = LocalCustomColors.current.isSelectedBackground
                            )
                        )

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    Icons.Filled.Notifications,
                                    contentDescription = "Notifications",
                                    tint = LocalCustomColors.current.iconColor
                                )
                            },
                            selected = pagerState.currentPage == 3,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.scrollToPage(3)
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = LocalCustomColors.current.iconColor,
                                indicatorColor = LocalCustomColors.current.isSelectedBackground
                            )
                        )

                    }

                    FloatingActionButton(
                        onClick = {
                            showCreatePost.value = !showCreatePost.value
                        },
                        modifier = Modifier
                            .offset(y = (-16).dp),
                        containerColor = LocalCustomColors.current.fabBackground,
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        if (showCreatePost.value) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Cancel Create",
                                tint = LocalCustomColors.current.iconColor
                            )
                        } else {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Create",
                                tint = LocalCustomColors.current.iconColor
                            )
                        }
                    }
                }
            }

        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(
                        top = paddingValues.calculateTopPadding(),
                        bottom = paddingValues.calculateBottomPadding()
                    )
                    .fillMaxSize()
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    when (page) {
                        0 -> PostScreen(
                            postViewModel = postViewModel,
                            navController = navController,
                            showCreatePost = showCreatePost.value,
                            onCreatePostChange = { show -> showCreatePost.value = show }
                        )

                        1 -> SearchScreen(
                            postViewModel = postViewModel,
                            navController = navController,
                            onNavigateToComments = { postId ->
                                postViewModel.loadPostById(postId)
                            }
                        )

                        2 -> FriendsScreen(
                            viewModel = friendsViewModel,
                            navController = navController
                        )

                        3 -> NotificationScreen(
                            viewModel = notificationViewModel,
                            onPostClick = { postId ->
                                postViewModel.loadPostById(postId)
                            },
                            navController = navController
                        )

                    }
                }
            }
        }
    }
}