package com.example.nam.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.nam.ui.archivedposts.ArchivedPostScreen
import com.example.nam.ui.auth.AuthViewModel
import com.example.nam.ui.auth.LoginPage
import com.example.nam.ui.auth.SignupPage
import com.example.nam.ui.comment.CommentScreen
import com.example.nam.ui.friends.FriendsScreen
import com.example.nam.ui.friends.FriendsViewModel
import com.example.nam.ui.hiddenposts.HiddenPostScreen
import com.example.nam.ui.home.HomePage
import com.example.nam.ui.language.LanguageScreen
import com.example.nam.ui.language.LanguageViewModel
import com.example.nam.ui.notification.NotificationScreen
import com.example.nam.ui.notification.NotificationViewModel
import com.example.nam.ui.post.CurrentPostState
import com.example.nam.ui.post.PostViewModel
import com.example.nam.ui.profile.ProfileScreen
import com.example.nam.ui.search.SearchScreen
import com.example.nam.ui.settings.ChangePasswordRoute
import com.example.nam.ui.settings.SettingsNavigationItem
import com.example.nam.ui.settings.SettingsScreen
import com.example.nam.ui.settings.SettingsViewModel
import com.example.nam.ui.splash.SplashScreen

@Composable
fun MyAppNavigation(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel,
    notificationViewModel: NotificationViewModel,
    friendsViewModel: FriendsViewModel,
    postViewModel: PostViewModel
) {
    val navController = rememberNavController()
    val settingsViewModel: SettingsViewModel = viewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentPostState by postViewModel.uiState.collectAsStateWithLifecycle()

    val handleSettingsItemSelected = { item: SettingsNavigationItem ->
        when (item) {
            SettingsNavigationItem.Language -> {
                navController.navigate("language")
            }

            SettingsNavigationItem.ChangePassword -> {
                navController.navigate("change_password")
            }

            SettingsNavigationItem.HiddenPosts -> {
                navController.navigate("hidden_posts")
            }

            SettingsNavigationItem.ArchivedPosts -> {
                navController.navigate("archived_posts")
            }
        }
    }

    LaunchedEffect(key1 = currentPostState) {
        if (currentPostState.currentPostState is CurrentPostState.PostLoaded &&
            navBackStackEntry?.destination?.route != "comments"
        ) {
            navController.navigate("comments") {
                launchSingleTop = true
                // Avoid nested navigation stacks
                popUpTo("home")
            }
        }
    }

    NavHost(navController = navController, startDestination = "splash", builder = {
        composable("splash") {
            SplashScreen(
                onAuthenticated = {
                    // Navigate to home screen and clear back stack
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onNotAuthenticated = {
                    // Navigate to login screen and clear back stack
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("login") {
            LoginPage(modifier, navController, authViewModel)
        }

        composable("signup") {
            SignupPage(modifier, navController, authViewModel)
        }

        composable("home") {
            HomePage(
                modifier,
                navController,
                authViewModel,
                postViewModel,
                notificationViewModel,
            )
        }

        composable("comments") {
            CommentScreen(
                postViewModel = postViewModel,
                onBackClick = { navController.navigateUp() }
            )
        }

        composable("profile") {
            ProfileScreen(
                postViewModel = postViewModel,
                onBackClick = { navController.navigateUp() }
            )
        }

        composable(
            route = "profile/{userId}",
            arguments = listOf(
                navArgument("userId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            ProfileScreen(
                postViewModel = postViewModel,
                userId = userId,
                onBackClick = { navController.navigateUp() }
            )
        }

        composable("notifications") {
            NotificationScreen(
                viewModel = notificationViewModel,
                onPostClick = { postId ->
                    postViewModel.loadPostById(postId)
                },
            )
        }

        composable("friends") {
            FriendsScreen(
                viewModel = friendsViewModel
            )
        }

        composable("post_search") {
            SearchScreen(
                postViewModel = postViewModel,
                navController = navController,
                onNavigateToComments = { postId ->
                    postViewModel.loadPostById(postId)
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                viewModel = settingsViewModel,
                onItemSelected = handleSettingsItemSelected,
                onShowChangePassword = {
                    navController.navigate("change_password")
                },
                onBackClick = { navController.navigateUp() }
            )
        }

        composable("language") {
            val languageViewModel: LanguageViewModel = viewModel()
            LanguageScreen(
                viewModel = languageViewModel,
                onBackClick = { navController.navigateUp() }
            )
        }

        composable("change_password") {
            ChangePasswordRoute(
                viewModel = settingsViewModel,
                onBackClick = { navController.navigateUp() }
            )
        }

        composable("hidden_posts") {
            HiddenPostScreen(
                postViewModel = postViewModel,
                navController = navController,
                onBackClick = { navController.navigateUp() }
            )
        }

        composable("archived_posts") {
            ArchivedPostScreen(
                postViewModel = postViewModel,
                navController = navController,
                onBackClick = { navController.navigateUp() }
            )
        }
    })
}