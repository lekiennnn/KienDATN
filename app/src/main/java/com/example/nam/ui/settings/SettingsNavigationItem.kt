package com.example.nam.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nam.R

enum class SettingsNavigationItem(
    @StringRes val title: Int,
    val icon: @Composable () -> Unit,
) {
    Language(
        title = R.string.language,
        icon = {
            Image(
                painter = painterResource(R.drawable.ic_language),
                contentDescription = stringResource(R.string.language_content_description),
                modifier = Modifier.size(24.dp)
            )
        }
    ),
    ChangePassword(
        title = R.string.change_password,
        icon = { Icon(imageVector = Icons.Filled.Lock, contentDescription = "Change Password") }
    ),
    HiddenPosts(
        title = R.string.hidden_posts,
        icon = {
            Image(
                painter = painterResource(R.drawable.ic_hidden_posts),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    ),
    ArchivedPosts(
        title = R.string.archived_posts,
        icon = {
            Image(
                painter = painterResource(R.drawable.ic_bookmark),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    ),
}