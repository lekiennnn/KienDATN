package com.example.kiendatn2.repository

import com.example.kiendatn2.data.FirebaseNotification
import com.example.kiendatn2.data.Notification
import com.example.kiendatn2.data.NotificationType
import com.example.kiendatn2.data.Post
import com.example.kiendatn2.data.User
import com.example.kiendatn2.data.toFirebaseNotification
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.firebase.auth.FirebaseAuth

class NotificationRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun createLikeNotification(post: Post, currentUser: User) {
        if (post.userId == currentUser.id) {
            return
        }

        val notificationId = firestore.collection("notifications").document().id

        val notification = Notification(
            id = notificationId,
            recipientId = post.userId,
            senderId = currentUser.id,
            senderName = currentUser.displayName,
            senderProfileImageUrl = currentUser.photoUrl ?: "",
            type = NotificationType.LIKE,
            postId = post.id,
            isRead = false
        )

        val firebaseNotification = notification.toFirebaseNotification()

        firestore.collection("notifications")
            .document(notificationId)
            .set(firebaseNotification)
            .await()
    }

    suspend fun createCommentNotification(post: Post, commentId: String, currentUser: User) {
        if (post.userId == currentUser.id) {
            return
        }

        val notificationId = firestore.collection("notifications").document().id

        val notification = Notification(
            id = notificationId,
            recipientId = post.userId,
            senderId = currentUser.id,
            senderName = currentUser.displayName,
            senderProfileImageUrl = currentUser.photoUrl ?: "",
            type = NotificationType.COMMENT,
            postId = post.id,
            commentId = commentId,
            isRead = false
        )

        val firebaseNotification = notification.toFirebaseNotification()

        firestore.collection("notifications")
            .document(notificationId)
            .set(firebaseNotification)
            .await()
    }

    suspend fun createFriendRequestNotification(recipientId: String, currentUser: User) {
        val notificationId = firestore.collection("notifications").document().id

        val notification = Notification(
            id = notificationId,
            recipientId = recipientId,
            senderId = currentUser.id,
            senderName = currentUser.displayName,
            senderProfileImageUrl = currentUser.photoUrl ?: "",
            type = NotificationType.FRIEND_REQUEST,
            isRead = false
        )

        val firebaseNotification = notification.toFirebaseNotification()

        firestore.collection("notifications")
            .document(notificationId)
            .set(firebaseNotification)
            .await()
    }

    suspend fun createFriendAcceptedNotification(recipientId: String, currentUser: User) {
        val notificationId = firestore.collection("notifications").document().id

        val notification = Notification(
            id = notificationId,
            recipientId = recipientId,
            senderId = currentUser.id,
            senderName = currentUser.displayName,
            senderProfileImageUrl = currentUser.photoUrl ?: "",
            type = NotificationType.FRIEND_ACCEPTED,
            isRead = false
        )

        val firebaseNotification = notification.toFirebaseNotification()

        firestore.collection("notifications")
            .document(notificationId)
            .set(firebaseNotification)
            .await()
    }

    fun getNotificationsForUser(userId: String): Flow<List<Notification>> = callbackFlow {
        try {
            Log.d("NotificationRepository", "Setting up notifications listener for user: $userId")
            val listenerRegistration = firestore.collection("notifications")
                .whereEqualTo("recipientId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("NotificationRepository", "Error getting notifications: ${error.message}", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    val notifications = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            val firebaseNotification = doc.toObject(FirebaseNotification::class.java)
                            firebaseNotification?.toNotification()
                        } catch (e: Exception) {
                            Log.e("NotificationRepository", "Error converting notification: ${e.message}")
                            null
                        }
                    }?.filter { it.senderId != it.recipientId } ?: listOf()

                    Log.d(
                        "NotificationRepository",
                        "Received ${notifications.size} notifications for user: $userId"
                    )
                    trySend(notifications)
                }

            awaitClose {
                Log.d("NotificationRepository", "Removing notifications listener for user: $userId")
                listenerRegistration.remove()
            }
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Exception in notification flow: ${e.message}", e)
            trySend(emptyList())
        }
    }

    suspend fun markNotificationAsRead(notificationId: String) {
        firestore.collection("notifications")
            .document(notificationId)
            .update("read", true)
            .await()
    }

    suspend fun markAllNotificationsAsRead(userId: String) {
        val batch = firestore.batch()

        val notificationDocs = firestore.collection("notifications")
            .whereEqualTo("recipientId", userId)
            .whereEqualTo("read", false)
            .get()
            .await()

        for (doc in notificationDocs.documents) {
            batch.update(doc.reference, "read", true)
        }

        if (notificationDocs.size() > 0) {
            batch.commit().await()
        }
    }

    suspend fun checkSelfNotifications() = withContext(Dispatchers.IO) {
        try {
            val currentUser = getCurrentUser() ?: return@withContext

            val notifications = firestore.collection("notifications")
                .whereEqualTo("recipientId", currentUser.id)
                .whereEqualTo("senderId", currentUser.id)
                .get()
                .await()

            if (!notifications.isEmpty) {
                Log.e("FirebaseRepository", "Found ${notifications.size()} self-notifications for user ${currentUser.id}")

                val batch = firestore.batch()
                for (doc in notifications.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit()
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error checking self-notifications: ${e.message}")
        }
    }

    private suspend fun getCurrentUser(): User? {
        try {
            val firebaseUser = auth.currentUser ?: return null

            val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
            if (!userDoc.exists()) {
                return null
            }

            return userDoc.toObject(User::class.java)
                ?.copy(id = firebaseUser.uid, uid = firebaseUser.uid)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error getting current user: ${e.message}")
            return null
        }
    }
}