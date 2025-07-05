package com.example.kiendatn2.ui.post

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.kiendatn2.R
import com.example.kiendatn2.data.Post
import com.example.kiendatn2.data.PostVisibility
import com.example.kiendatn2.ui.components.VideoPlayer
import com.example.kiendatn2.ui.media.MediaViewerScreen
import com.example.kiendatn2.ui.theme.LocalCustomColors
import com.example.kiendatn2.utils.Navigation
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun PostItemDetailed(
    modifier: Modifier = Modifier,
    authorName: String,
    content: String,
    imageUrl: String? = null,
    videoUrl: String? = null,
    hasVideo: Boolean = false,
    likeCount: Int = 0,
    isLikedByCurrentUser: Boolean = false,
    commentCount: Int = 0,
    shareCount: Int = 0,
    profilePictureUrl: String? = null,
    onLikeClick: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    onShareClick: (Boolean) -> Unit = { _ -> },
    userId: String? = null,
    navController: NavController? = null,
    onProfileClick: (() -> Unit)? = null,
    isSharedPost: Boolean = false,
    sharedByUserName: String = "",
    isSharedByCurrentUser: Boolean = false,
    post: Post? = null,
    onDeleteClick: ((String) -> Unit)? = null,
    onHideClick: ((String) -> Unit)? = null,
    onEditClick: ((Post) -> Unit)? = null,
    onUnhideClick: ((String) -> Unit)? = null,
    isInHiddenPostScreen: Boolean = false,
    onArchiveClick: ((String) -> Unit)? = null,
    onUnarchiveClick: ((String) -> Unit)? = null,
    isInArchivedPostScreen: Boolean = false,
    isArchived: Boolean = false
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        tonalElevation = 2.dp,
        color = LocalCustomColors.current.secondaryBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            // Post shared information
            if (isSharedPost) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_share),
                        contentDescription = "Share icon",
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp)
                    )

                    Text(
                        text = stringResource(R.string.shared_post, sharedByUserName),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = LocalCustomColors.current.textSecondary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

            }
            
            // Author info row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                // Profile picture
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        .clickable {
                            if (onProfileClick != null) {
                                onProfileClick()
                            } else if (userId != null && navController != null) {
                                Navigation.navigateToUserProfile(userId, navController)
                            }
                        }
                ) {
                    if (profilePictureUrl != null) {
                        AsyncImage(
                            model = profilePictureUrl,
                            contentDescription = "Profile picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // User initial as placeholder - use first letter of display name
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = authorName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = LocalCustomColors.current.textPrimary,
                            modifier = Modifier
                                .padding(top = 4.dp, start = 2.dp)
                                .clickable {
                                    if (onProfileClick != null) {
                                        onProfileClick()
                                    } else if (userId != null && navController != null) {
                                        Navigation.navigateToUserProfile(userId, navController)
                                    }
                                }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    post?.createdAt?.let { timestamp ->
                        val date = timestamp.toDate()
                        val formatter =
                            SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
                        val formattedDate = formatter.format(date)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 2.dp)
                        ) {
                            Text(
                                text = formattedDate,
                                fontSize = 12.sp,
                                color = LocalCustomColors.current.textSecondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            post?.let {
                                Icon(
                                    painter = when (it.visibility) {
                                        PostVisibility.PUBLIC -> painterResource(id = R.drawable.ic_language)
                                        PostVisibility.FRIENDS_ONLY -> painterResource(id = R.drawable.ic_friends)
                                        PostVisibility.PRIVATE -> painterResource(id = R.drawable.ic_lock)
                                    },
                                    contentDescription = "Post visibility",
                                    tint = LocalCustomColors.current.textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = LocalCustomColors.current.textPrimary
                        )
                    }

                    val currentUser = FirebaseAuth.getInstance().currentUser
                    val isPostOwner = currentUser?.uid == userId

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (isPostOwner) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.edit_post)) },
                                onClick = {
                                    post?.let {
                                        onEditClick?.invoke(it)
                                    }
                                    showMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete_post)) },
                                onClick = {
                                    post?.id?.let { postId ->
                                        onDeleteClick?.invoke(postId)
                                    }
                                    showMenu = false
                                }
                            )
                        } else if (isInArchivedPostScreen) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.unarchive_post)) },
                                onClick = {
                                    post?.id?.let { postId ->
                                        onUnarchiveClick?.invoke(postId)
                                    }
                                    showMenu = false
                                }
                            )
                        } else if (isInHiddenPostScreen) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.unhide_post)) },
                                onClick = {
                                    post?.id?.let { postId ->
                                        onUnhideClick?.invoke(postId)
                                    }
                                    showMenu = false
                                }
                            )
                        } else {
                            if (isPostOwner) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.archive_post)) },
                                    onClick = {
                                        post?.id?.let { postId ->
                                            onArchiveClick?.invoke(postId)
                                        }
                                        showMenu = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.hide_post)) },
                                onClick = {
                                    post?.id?.let { postId ->
                                        onHideClick?.invoke(postId)
                                    }
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Post content
            Text(
                text = content,
                fontSize = 20.sp,
                color = LocalCustomColors.current.textPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Display image or video if available
            if (hasVideo && videoUrl != null) {
                var showFullScreenVideo by remember { mutableStateOf(false) }

                VideoPlayer(
                    videoUrl = videoUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showFullScreenVideo = true }
                )

                if (showFullScreenVideo) {
                    MediaViewerScreen(
                        mediaUrl = videoUrl,
                        isVideo = true,
                        onDismiss = { showFullScreenVideo = false }
                    )
                }
            } else if (imageUrl != null) {
                var showFullScreenImage by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(bottom = 5.dp)
                        .clickable { showFullScreenImage = true }
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.Center)
                    )

                    AsyncImage(
                        model = if (imageUrl.startsWith("content:") || imageUrl.startsWith("file:")) {
                            Uri.parse(imageUrl)
                        } else {
                            imageUrl
                        },
                        contentDescription = "Post image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        error = painterResource(id = R.drawable.ic_error_image),
                        placeholder = painterResource(id = R.drawable.ic_error_image),
                        onLoading = {
                            android.util.Log.d("PostItemDetailed", "Loading image: $imageUrl")
                        },
                        onError = {
                            android.util.Log.e(
                                "PostItemDetailed",
                                "Error loading image: $imageUrl, error: $it"
                            )
                        },
                        onSuccess = {
                            android.util.Log.d(
                                "PostItemDetailed",
                                "Successfully loaded image: $imageUrl"
                            )
                        }
                    )
                }

                // Show full screen image viewer when clicked
                if (showFullScreenImage) {
                    MediaViewerScreen(
                        mediaUrl = imageUrl,
                        isVideo = false,
                        onDismiss = { showFullScreenImage = false }
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 8.dp),
                color = Color.LightGray,
                thickness = 0.5.dp
            )

            // Action buttons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceAround
            ) {
                // Like button
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(vertical = 8.dp, horizontal = 8.dp)
                        .clickable {
                            onLikeClick()
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(
                                if (isLikedByCurrentUser) R.drawable.ic_like_filled
                                else R.drawable.ic_like
                            ),
                            contentDescription = "Like icon",
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 4.dp)
                        )

                        Text(
                            text = "$likeCount",
                            fontSize = 16.sp,
                            color = if (isLikedByCurrentUser) LocalCustomColors.current.textIsLiked else LocalCustomColors.current.textPrimary,
                            modifier = Modifier
                        )
                    }
                }

                // Comment button
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(vertical = 8.dp, horizontal = 8.dp)
                        .clickable {
                            onCommentClick()
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_comment),
                            contentDescription = "Comment icon",
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 4.dp)
                        )

                        Text(
                            text = "$commentCount",
                            fontSize = 16.sp,
                            color = LocalCustomColors.current.textPrimary,
                            modifier = Modifier
                        )
                    }
                }

                // Share button
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    var showShareMenu by remember { mutableStateOf(false) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showShareMenu = true }
                    ) {
                        Image(
                            painter = painterResource(
                                if (isSharedByCurrentUser) R.drawable.ic_shared
                                else R.drawable.ic_share
                            ),
                            contentDescription = "Share icon",
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 4.dp)
                        )

                        Text(
                            text = "$shareCount",
                            fontSize = 16.sp,
                            color = LocalCustomColors.current.textPrimary,
                            modifier = Modifier
                        )
                    }

                    DropdownMenu(
                        expanded = showShareMenu,
                        onDismissRequest = { showShareMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.repost_only_me)) },
                            onClick = {
                                post?.id?.let { postId ->
                                    onShareClick(true)
                                }
                                showShareMenu = false
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.share_for_friends)) },
                            enabled = post?.visibility != PostVisibility.PRIVATE,
                            onClick = {
                                post?.id?.let { postId ->
                                    onShareClick(false)
                                }
                                showShareMenu = false
                            }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(vertical = 8.dp, horizontal = 8.dp)
                        .clickable {
                            // Just toggle the archive status with one function call
                            post?.id?.let { postId ->
                                // Both functions call the same toggleArchive under the hood
                                if (isArchived || isInArchivedPostScreen || post.isArchivedByCurrentUser) {
                                    onUnarchiveClick?.invoke(postId)
                                } else {
                                    onArchiveClick?.invoke(postId)
                                }
                            }
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isArchived =
                            isArchived || isInArchivedPostScreen || post?.isArchivedByCurrentUser == true

                        Image(
                            painter = painterResource(
                                if (isArchived) R.drawable.ic_bookmarked else R.drawable.ic_bookmark
                            ),
                            contentDescription = "Archive icon",
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 4.dp),
                        )

                        Text(
                            text = "",
                            fontSize = 16.sp,
                            color = LocalCustomColors.current.textPrimary,
                            modifier = Modifier
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}