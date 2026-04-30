package com.example.nam.ui.comment

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.example.nam.data.Comment
import com.example.nam.ui.post.PostViewModel

@Composable
fun ReplyItem(
    reply: Comment,
    postViewModel: PostViewModel,
    onReplyClick: (Comment) -> Unit,
    profilePictureUrl: String? = null,
    userId: String? = null,
    navController: NavController? = null,
    indentLevel: Int = 1
) {
    CommentItem(
        comment = reply,
        postViewModel = postViewModel,
        replies = emptyList(), // No nesting beyond this level
        isShowingReplies = false,
        onReplyClick = onReplyClick,
        profilePictureUrl = profilePictureUrl,
        userId = userId,
        navController = navController,
        indentLevel = indentLevel
    )
}