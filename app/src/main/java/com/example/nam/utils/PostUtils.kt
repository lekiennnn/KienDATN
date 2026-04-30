package com.example.nam.utils

import androidx.compose.material3.DrawerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun openDrawer(scope: CoroutineScope, drawerState: DrawerState) {
    scope.launch {
        drawerState.open()
    }
}