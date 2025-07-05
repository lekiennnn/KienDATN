package com.example.kiendatn2.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.kiendatn2.R
import com.example.kiendatn2.data.SearchHistory
import com.example.kiendatn2.ui.theme.LocalCustomColors

@Composable
fun SearchHistorySection(
    searchHistory: List<SearchHistory>,
    onHistoryItemClick: (String) -> Unit,
    onDeleteHistoryItem: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    if (searchHistory.isEmpty()) {
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Divider(modifier = Modifier.padding(vertical = 8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.recent_searches),
                style = MaterialTheme.typography.titleMedium,
                color = LocalCustomColors.current.textPrimary
            )

            IconButton(onClick = onClearHistory) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.clear_search_history),
                    tint = LocalCustomColors.current.textSecondary
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp) // Set a max height for the history list
        ) {
            items(searchHistory) { historyItem ->
                SearchHistoryItem(
                    historyItem = historyItem,
                    onItemClick = { onHistoryItemClick(historyItem.query) },
                    onDeleteClick = { onDeleteHistoryItem(historyItem.id) }
                )
            }
        }
    }
}

@Composable
fun SearchHistoryItem(
    historyItem: SearchHistory,
    onItemClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Surface(
        onClick = onItemClick,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Search icon and query text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.recent_searches),
                    modifier = Modifier.padding(end = 16.dp),
                    tint = LocalCustomColors.current.iconColor
                )
                Text(
                    text = historyItem.query,
                    color = LocalCustomColors.current.textPrimary,
                    maxLines = 1
                )
            }

            // Delete button
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = stringResource(R.string.remove_from_history),
                    tint = LocalCustomColors.current.textSecondary
                )
            }
        }
        Divider(modifier = Modifier.fillMaxWidth())
    }
}