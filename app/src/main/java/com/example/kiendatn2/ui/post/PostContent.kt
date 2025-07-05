package com.example.kiendatn2.ui.post

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.kiendatn2.R
import android.util.Log
import androidx.compose.runtime.key
import com.example.kiendatn2.data.Post

@Composable
fun PostContent(
    modifier: Modifier = Modifier,
    postsState: PostState?,
    postViewModel: PostViewModel,
    isFriendsPost: Boolean = false,
    navController: NavController? = null,
    onRetry: () -> Unit,
    key: String = "",
    onEditPost: ((Post) -> Unit)? = null
) {
    // Use the key to force recomposition when the tab changes
    key(key) {
        when (val state = postsState) {
            is PostState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.loading_posts))
                }
            }

            is PostState.Success -> {
                if (state.posts.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = stringResource(R.string.no_posts))
                    }
                } else {
                    if (isFriendsPost) {
                        state.posts.forEach { post ->
                            if (post.isSharedPost) {
                                Log.d(
                                    "PostContent",
                                    "Found shared post in FriendsPosts: ID=${post.id}, sharedBy=${post.sharedByUserName}"
                                )
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.posts) { post ->
                            PostItem(
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 8.dp
                                ),
                                post = post,
                                postViewModel = postViewModel,
                                navController = navController,
                                onEditClick = { post -> onEditPost?.invoke(post) }
                            )
                        }
                    }
                }
            }

            is PostState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.error, state.message),
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onRetry) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }

            null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}