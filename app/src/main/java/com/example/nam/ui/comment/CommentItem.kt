package com.example.nam.ui.comment

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nam.R
import com.example.nam.data.Comment
import com.example.nam.ui.post.PostViewModel
import com.example.nam.ui.theme.LocalCustomColors
import com.example.nam.utils.Navigation

@Composable
fun CommentItem(
    comment: Comment,
    postViewModel: PostViewModel,
    replies: List<Comment> = emptyList(),
    isShowingReplies: Boolean = false,
    profilePictureUrl: String? = null,
    onReplyClick: (Comment) -> Unit,
    userId: String? = null,
    navController: NavController? = null,
    onProfileClick: (() -> Unit)? = null,
    indentLevel: Int = 0
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start =  (16 + indentLevel * 16).dp,
                top = 4.dp,
                bottom = 4.dp,
                end = 8.dp
            )
            .border(
                1.dp,
                Color.White,
                shape = MaterialTheme.shapes.medium
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                    // First try to use the authorProfilePicture from the comment itself,
                    // then fall back to the passed profilePictureUrl
                    val pictureUrl = comment.authorProfilePicture ?: profilePictureUrl

                    if (pictureUrl != null) {
                        AsyncImage(
                            model = pictureUrl,
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

                Text(
                    text = comment.userDisplayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
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
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (comment.parentCommentId != null) {
                val parentUsername =
                    findParentCommentUsername(comment.parentCommentId, replies, postViewModel)
                Text(
                    text = "Replying to $parentUsername's comment",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Text(
                text = comment.text,
                fontSize = 14.sp,
                color = LocalCustomColors.current.textPrimary,
                modifier = Modifier.padding(
                    horizontal = 4.dp,
                    vertical = 6.dp
                )
            )

            if (comment.imageUrl != null) {
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = comment.imageUrl,
                    contentDescription = "Comment image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 8.dp),
                color = Color.LightGray,
                thickness = 0.5.dp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Like button
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(vertical = 8.dp, horizontal = 8.dp)
                        .clickable {
                            postViewModel.toggleCommentLike(comment.id)
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(if (comment.isLikedByCurrentUser) R.drawable.ic_like_filled else R.drawable.ic_like),
                            contentDescription = "Like",
                            tint = if (comment.isLikedByCurrentUser)
                                Color(0xFFE91E63) else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${comment.likeCount}",
                            fontSize = 16.sp,
                            color = if (comment.isLikedByCurrentUser)
                                LocalCustomColors.current.textIsLiked else LocalCustomColors.current.textPrimary
                        )
                    }
                }

                // Reply button
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(vertical = 8.dp, horizontal = 8.dp)
                        .clickable {
                            onReplyClick(comment)
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
                            text = "${comment.replyCount}",
                            fontSize = 16.sp,
                            color = LocalCustomColors.current.textPrimary,
                            modifier = Modifier
                        )
                    }
                }
            }
        }
    }

    if (replies.isNotEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            replies.forEach { reply ->
                val showRepliesState = remember { mutableStateOf(false) }
                val showReplies = showRepliesState.value
                val parentInfo = findParentCommentInfo(
                    parentId = reply.parentCommentId ?: "",
                    replies = replies,
                    postViewModel = postViewModel
                )
                val replyProfilePicture =
                    reply.authorProfilePicture ?: parentInfo.profilePicture
                val nestedReplies = postViewModel.getRepliesForComment(reply.id)
                CommentItem(
                    comment = reply,
                    postViewModel = postViewModel,
                    onReplyClick = onReplyClick,
                    replies = if (showReplies) nestedReplies else emptyList(),
                    isShowingReplies = showReplies,
                    profilePictureUrl = replyProfilePicture,
                    userId = reply.userId,
                    navController = navController,
                    indentLevel = indentLevel + 1,
                    onProfileClick = if (reply.userId != null && reply.userId.isNotEmpty()) {
                        {
                            if (navController != null) {
                                Navigation.navigateToUserProfile(
                                    reply.userId,
                                    navController
                                )
                            }
                        }
                    } else null
                )

                if (reply.replyCount > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = (32 + indentLevel * 16).dp,
                                top = 4.dp,
                                bottom = 4.dp
                            )
                            .clickable {
                                showRepliesState.value = !showReplies
                            }
                    ) {
                        Text(
                            text = if (showReplies) "Hide replies" else "View ${reply.replyCount} replies",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

data class ParentCommentInfo(
    val username: String,
    val profilePicture: String? = null
)

fun findParentCommentInfo(
    parentId: String,
    replies: List<Comment> = emptyList(),
    postViewModel: PostViewModel
): ParentCommentInfo {
    val parentInReplies = replies.find { it.id == parentId }
    if (parentInReplies != null) {
        return ParentCommentInfo(
            username = parentInReplies.userDisplayName,
            profilePicture = parentInReplies.authorProfilePicture
        )
    }

    val parentComment = postViewModel.getCommentById(parentId)
    return if (parentComment != null) {
        ParentCommentInfo(
            username = parentComment.userDisplayName,
            profilePicture = parentComment.authorProfilePicture
        )
    } else {
        ParentCommentInfo(username = "Unknown User")
    }
}

fun findParentCommentUsername(
    parentId: String,
    replies: List<Comment> = emptyList(),
    postViewModel: PostViewModel
): String {
    return findParentCommentInfo(parentId, replies, postViewModel).username
}