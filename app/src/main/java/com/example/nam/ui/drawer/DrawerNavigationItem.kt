package com.example.nam.ui.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nam.R

enum class DrawerNavigationItem(
    val title: String,
    val titleResId: Int,
    val icon: @Composable () -> Unit,
) {
    Profile(
        title = "Profile",
        titleResId = R.string.profile,
        icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") }
    ),
    Settings(
        title = "Settings",
        titleResId = R.string.settings,
        icon = {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.settings_content_description)
            )
        }
    )
}