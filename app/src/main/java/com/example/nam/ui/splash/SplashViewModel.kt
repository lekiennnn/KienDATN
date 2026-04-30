package com.example.nam.ui.splash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nam.repository.FirebaseRepository
import com.example.nam.utils.SharedPreferenceManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SplashViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferenceManager = SharedPreferenceManager.getInstance(application)
    private val repository = FirebaseRepository()

    private val _splashState = MutableStateFlow<SplashState>(SplashState.Loading)
    val splashState: StateFlow<SplashState> = _splashState.asStateFlow()

    init {
        initializeApp()
    }

    private fun initializeApp() {
        viewModelScope.launch {
            // First check if user is logged in
            val isLoggedIn = sharedPreferenceManager.isLoggedIn()

            if (!isLoggedIn) {
                _splashState.value = SplashState.NotAuthenticated
                return@launch
            }

            try {
                // Preload posts data
                repository.getPostsWithLikeStatus()
                repository.getFriendsPostsWithLikeStatus()

                // Add a minimum delay of 2 seconds for splash screen display
                delay(2000)

                _splashState.value = SplashState.Authenticated
            } catch (exception: Exception) {
                // Even if post loading fails, we'll still navigate to main screen
                // but after the delay
                delay(2000)
                _splashState.value = SplashState.Authenticated
            }
        }
    }
}

sealed class SplashState {
    object Loading : SplashState()
    object Authenticated : SplashState()
    object NotAuthenticated : SplashState()
}