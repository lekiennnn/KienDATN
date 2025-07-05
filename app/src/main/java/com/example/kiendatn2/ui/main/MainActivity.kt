package com.example.kiendatn2.ui.main

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.kiendatn2.ui.auth.AuthViewModel
import com.example.kiendatn2.navigation.MyAppNavigation
import com.example.kiendatn2.ui.notification.NotificationViewModel
import com.example.kiendatn2.ui.theme.Kiendatn2Theme
import com.example.kiendatn2.ui.friends.FriendsViewModel
import com.example.kiendatn2.ui.post.PostViewModel
import com.example.kiendatn2.ui.base.BaseComposeActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : BaseComposeActivity() {

    private lateinit var authViewModel: AuthViewModel
    private val notificationViewModel: NotificationViewModel by viewModels()
    private val friendsViewModel: FriendsViewModel by viewModels()
    private val postViewModel: PostViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.navigationBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        window.statusBarColor = getColor(android.R.color.black)

        authViewModel = ViewModelProvider(
            this,
            viewModelFactory {
                initializer { AuthViewModel(application) }
            }
        ).get(AuthViewModel::class.java)

        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun AppContent() {
        Kiendatn2Theme {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                MyAppNavigation(
                    modifier = Modifier.padding(innerPadding),
                    authViewModel = authViewModel,
                    notificationViewModel = notificationViewModel,
                    friendsViewModel = friendsViewModel,
                    postViewModel = postViewModel
                )
            }
        }
    }
}