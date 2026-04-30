package com.example.nam.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nam.R
import com.example.nam.ui.theme.LocalCustomColors

@Composable
fun SettingsContent(
    selectedItem: SettingsNavigationItem?,
    onItemSelected: (SettingsNavigationItem) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Menu items from enum
        SettingsNavigationItem.values().forEach { item ->
            SettingsItem(
                item = item,
                isSelected = item == selectedItem,
                onItemClick = { onItemSelected(item) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun SettingsItem(
    item: SettingsNavigationItem,
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
                text = stringResource(id = item.title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}