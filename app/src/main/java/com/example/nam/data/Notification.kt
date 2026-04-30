package com.example.nam.data

import com.google.firebase.Timestamp

// This class is used for UI and app-level operations
data class Notification(
    val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderProfileImageUrl: String = "",
    val type: NotificationType = NotificationType.LIKE,
    val postId: String = "",
    val commentId: String? = null,
    val timestamp: Timestamp = Timestamp.now(),
    val isRead: Boolean = false
)

// This class is used specifically for Firebase serialization/deserialization
data class FirebaseNotification(
    val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderProfileImageUrl: String = "",
    val type: NotificationType = NotificationType.LIKE,
    val postId: String = "",
    val commentId: String? = null,
    val timestamp: Timestamp = Timestamp.now(),
    val read: Boolean = false
) {
    // Convert to app Notification
    fun toNotification(): Notification {
        return Notification(
            id = id,
            recipientId = recipientId,
            senderId = senderId,
            senderName = senderName,
            senderProfileImageUrl = senderProfileImageUrl,
            type = type,
            postId = postId,
            commentId = commentId,
            timestamp = timestamp,
            isRead = read
        )
    }
}

// Extension function to convert app Notification to Firebase model
fun Notification.toFirebaseNotification(): FirebaseNotification {
    return FirebaseNotification(
        id = id,
        recipientId = recipientId,
        senderId = senderId,
        senderName = senderName,
        senderProfileImageUrl = senderProfileImageUrl,
        type = type,
        postId = postId,
        commentId = commentId,
        timestamp = timestamp,
        read = isRead
    )
}

enum class NotificationType {
    LIKE,
    COMMENT,
    REPLY,
    FRIEND_REQUEST,
    FRIEND_ACCEPTED
}