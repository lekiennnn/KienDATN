package com.example.kiendatn2.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kiendatn2.R
import com.example.kiendatn2.ui.theme.LocalCustomColors

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = viewModel(),
    onAuthenticated: () -> Unit,
    onNotAuthenticated: () -> Unit
) {
    val splashState by viewModel.splashState.collectAsState()

    LaunchedEffect(splashState) {
        when (splashState) {
            SplashState.Authenticated -> onAuthenticated()
            SplashState.NotAuthenticated -> onNotAuthenticated()
            SplashState.Loading -> { /* Loading state, do nothing yet */ }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalCustomColors.current.primaryBackground)
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_dark_background),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(100.dp))
                .align(Alignment.Center)
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        ) {
            LinearProgressIndicator(
                color = LocalCustomColors.current.textSecondary,
                modifier = Modifier.height(16.dp).width(360.dp).clip(RoundedCornerShape(100.dp)),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}