package com.example.nam.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nam.R
import com.example.nam.ui.theme.LocalCustomColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onItemSelected: (SettingsNavigationItem) -> Unit,
    onShowChangePassword: () -> Unit,
    onBackClick: () -> Unit = {},
) {
    val selectedItem = remember { mutableStateOf<SettingsNavigationItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Text(
                        stringResource(R.string.settings),
                        color = LocalCustomColors.current.textPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LocalCustomColors.current.navBarsBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            SettingsContent(
                selectedItem = selectedItem.value,
                onItemSelected = { selected ->
                    selectedItem.value = selected

                    // Handle the change password navigation
                    if (selected == SettingsNavigationItem.ChangePassword) {
                        onShowChangePassword()
                    } else {
                        onItemSelected(selected)
                    }
                },
            )
        }
    }
}

@Composable
fun ChangePasswordRoute(
    viewModel: SettingsViewModel = viewModel(),
    onBackClick: () -> Unit = {}
) {
    ChangePasswordScreen(
        viewModel = viewModel,
        onBackClick = onBackClick
    )
}