package com.example.kiendatn2.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kiendatn2.R
import com.example.kiendatn2.ui.theme.LocalCustomColors
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource

@Composable
fun NavigationDrawer(
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
    onSignOutClick: () -> Unit = {},
    onItemSelected: (DrawerNavigationItem) -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val selectedItem = remember { mutableStateOf<DrawerNavigationItem?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.70f)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.background
            ) {
                DrawerContent(
                    selectedItem = selectedItem.value,
                    onItemSelected = { selected ->
                        selectedItem.value = selected
                        scope.launch { drawerState.close() }
                        onItemSelected(selected) // Call the passed-in handler
                    },
                    onSignOutClick = onSignOutClick
                )
            }
        },
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)
    ) {
        content()
    }
}

@Composable
fun DrawerContent(
    selectedItem: DrawerNavigationItem?,
    onItemSelected: (DrawerNavigationItem) -> Unit,
    onSignOutClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp)
            .systemBarsPadding()
    ) {
        // Header section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                text = LocalContext.current.getString(R.string.menu),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = LocalCustomColors.current.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Divider()
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Menu items from enum
        DrawerNavigationItem.values().forEach { item ->
            DrawerItem(
                item = item,
                isSelected = item == selectedItem,
                onItemClick = { onItemSelected(item) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        // Footer with sign out
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding()
                .clickable { onSignOutClick() }
        ) {
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = LocalContext.current.getString(R.string.sign_out),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun DrawerItem(
    item: DrawerNavigationItem,
    isSelected: Boolean,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onItemClick() }
            .background(
                if (isSelected) LocalCustomColors.current.secondaryBackground
                else MaterialTheme.colorScheme.surface
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icon from the enum
            CompositionLocalProvider(
                LocalContentColor provides if (isSelected)
                    LocalCustomColors.current.secondaryBackground
                else
                    MaterialTheme.colorScheme.onSurface
            ) {
                item.icon()
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text
            Text(
                text = stringResource(item.titleResId),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}