package com.example.kiendatn2.ui.comment

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.example.kiendatn2.data.Comment
import com.example.kiendatn2.ui.post.PostViewModel

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