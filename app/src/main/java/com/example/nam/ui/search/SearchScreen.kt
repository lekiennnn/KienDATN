package com.example.nam.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key.Companion.Delete
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.nam.R
import com.example.nam.data.SearchHistory
import com.example.nam.ui.post.PostItem
import com.example.nam.ui.post.PostViewModel
import com.example.nam.ui.theme.LocalCustomColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = viewModel(),
    navController: NavController? = null,
    onNavigateToComments: (String) -> Unit = {},
    postViewModel: PostViewModel = viewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadSearchHistory()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                },
                placeholder = { Text(stringResource(R.string.search)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            viewModel.searchPosts("")
                            viewModel.loadSearchHistory()
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = stringResource(R.string.clear_search)
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (searchQuery.length >= 2) {
                            viewModel.saveSearchToHistory(searchQuery)
                            viewModel.searchPosts(searchQuery)
                        }
                    }
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (searchQuery.length >= 2) {
                        viewModel.saveSearchToHistory(searchQuery)
                        viewModel.searchPosts(searchQuery)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = LocalCustomColors.current.secondaryBackground,
                ),
            ) {
                Text(text = stringResource(R.string.search), color = LocalCustomColors.current.textPrimary)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            when (val searchState = uiState.searchState) {
                is SearchState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is SearchState.Success -> {
                    val posts = searchState.posts
                    if (posts.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_posts_found),
                            modifier = Modifier.align(Alignment.Center),
                            color = LocalCustomColors.current.textPrimary
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(posts) { post ->
                                PostItem(
                                    post = post,
                                    postViewModel = postViewModel,
                                    navController = navController
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }

                is SearchState.Error -> {
                    Text(
                        text = stringResource(R.string.error_prefix, searchState.message),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is SearchState.Initial -> {
                    // Show search history when no active search
                    if (uiState.searchHistory.isNotEmpty()) {
                        SearchHistorySection(
                            searchHistory = uiState.searchHistory,
                            onHistoryItemClick = { query ->
                                searchQuery = query
                                viewModel.searchPosts(query)
                            },
                            onDeleteHistoryItem = { historyId ->
                                viewModel.deleteSearchHistoryItem(historyId)
                            },
                            onClearHistory = {
                                viewModel.clearSearchHistory()
                            }
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.type_to_search),
                            modifier = Modifier.align(Alignment.Center),
                            color = LocalCustomColors.current.textPrimary
                        )
                    }
                }
            }
        }
    }
}