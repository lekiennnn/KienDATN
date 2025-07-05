package com.example.kiendatn2.repository

import com.example.kiendatn2.data.Friendship
import com.example.kiendatn2.data.FriendshipStatus
import com.example.kiendatn2.data.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FriendsRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")
    private val friendshipsCollection = firestore.collection("friendships")
    private val notificationRepository = NotificationRepository()

    suspend fun sendFriendRequest(receiverId: String): Friendship = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

        try {
            val currentUserDoc = usersCollection.document(currentUser.uid).get().await()
            if (!currentUserDoc.exists()) {
                throw IllegalStateException("Current user document doesn't exist")
            }

            val currentUserData = currentUserDoc.toObject(User::class.java)
                ?.copy(id = currentUser.uid, uid = currentUser.uid)
                ?: throw IllegalStateException("Failed to get current user data")

            val receiverDoc = usersCollection.document(receiverId).get().await()
            if (!receiverDoc.exists()) {
                throw IllegalStateException("Receiver user document doesn't exist: $receiverId")
            }

            val receiverData =
                receiverDoc.toObject(User::class.java)?.copy(id = receiverId, uid = receiverId)
                    ?: throw IllegalStateException("Failed to get receiver user data")

            val existingFriendship = checkExistingFriendship(currentUser.uid, receiverId)
            if (existingFriendship != null) {
                if (existingFriendship.status == FriendshipStatus.DECLINED) {
                    val updatedFriendship = existingFriendship.copy(
                        status = FriendshipStatus.PENDING,
                        updatedAt = Timestamp.now()
                    )

                    firestore.collection("friendships")
                        .document(existingFriendship.id)
                        .set(updatedFriendship)
                        .await()

                    notificationRepository.createFriendRequestNotification(
                        receiverId,
                        currentUserData
                    )

                    return@withContext updatedFriendship
                }
                return@withContext existingFriendship
            }

            val friendshipId = friendshipsCollection.document().id
            val friendship = Friendship(
                id = friendshipId,
                senderId = currentUser.uid,
                senderName = currentUserData.displayName,
                senderPhotoUrl = currentUserData.photoUrl,
                receiverId = receiverId,
                receiverName = receiverData.displayName,
                receiverPhotoUrl = receiverData.photoUrl,
                status = FriendshipStatus.PENDING,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )

            friendshipsCollection.document(friendshipId).set(friendship).await()

            val verifyDoc = friendshipsCollection.document(friendshipId).get().await()
            if (!verifyDoc.exists()) {
                throw IllegalStateException("Failed to create friendship document: $friendshipId")
            }

            notificationRepository.createFriendRequestNotification(receiverId, currentUserData)

            friendship
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun acceptFriendRequest(friendshipId: String): Friendship =
        withContext(Dispatchers.IO) {
            val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

            try {
                val friendshipDoc = friendshipsCollection.document(friendshipId).get().await()
                if (!friendshipDoc.exists()) {
                    throw IllegalStateException("Friend request with ID $friendshipId not found")
                }

                val friendship = friendshipDoc.toObject(Friendship::class.java)
                    ?: throw IllegalStateException("Failed to convert document to Friendship")

                if (friendship.receiverId != currentUser.uid) {
                    throw IllegalStateException("Only the receiver can accept a friend request")
                }

                val updatedFriendship = friendship.copy(
                    status = FriendshipStatus.ACCEPTED,
                    updatedAt = Timestamp.now()
                )

                friendshipsCollection.document(friendshipId).set(updatedFriendship).await()

                usersCollection.document(friendship.senderId).update(
                    "friendCount",
                    com.google.firebase.firestore.FieldValue.increment(1)
                ).await()

                usersCollection.document(friendship.receiverId).update(
                    "friendCount",
                    com.google.firebase.firestore.FieldValue.increment(1)
                ).await()

                val currentUserData =
                    usersCollection.document(currentUser.uid).get().await()
                        .toObject(User::class.java)
                if (currentUserData != null) {
                    notificationRepository.createFriendAcceptedNotification(
                        friendship.senderId,
                        currentUserData
                    )
                }

                updatedFriendship
            } catch (e: Exception) {
                throw e
            }
        }

    suspend fun declineFriendRequest(friendshipId: String): Friendship =
        withContext(Dispatchers.IO) {
            val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

            try {
                val friendshipDoc = friendshipsCollection.document(friendshipId).get().await()
                if (!friendshipDoc.exists()) {
                    throw IllegalStateException("Friend request with ID $friendshipId not found")
                }

                val friendship = friendshipDoc.toObject(Friendship::class.java)
                    ?: throw IllegalStateException("Failed to convert document to Friendship")

                if (friendship.receiverId != currentUser.uid) {
                    throw IllegalStateException("Only the receiver can decline a friend request")
                }

                val updatedFriendship = friendship.copy(
                    status = FriendshipStatus.DECLINED,
                    updatedAt = Timestamp.now()
                )

                friendshipsCollection.document(friendshipId).set(updatedFriendship).await()

                val verifyDoc = friendshipsCollection.document(friendshipId).get().await()
                val verifiedFriendship = verifyDoc.toObject(Friendship::class.java)
                if (verifiedFriendship?.status != FriendshipStatus.DECLINED) {
                    throw IllegalStateException("Failed to update friendship status to DECLINED")
                }

                updatedFriendship
            } catch (e: Exception) {
                throw e
            }
        }

    suspend fun removeFriend(friendshipId: String): Boolean = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

        val friendshipDoc = friendshipsCollection.document(friendshipId).get().await()
        val friendship = friendshipDoc.toObject(Friendship::class.java)
            ?: throw IllegalStateException("Friendship not found")

        if (friendship.senderId != currentUser.uid && friendship.receiverId != currentUser.uid) {
            throw IllegalStateException("You cannot remove this friendship")
        }

        if (friendship.status != FriendshipStatus.ACCEPTED) {
            throw IllegalStateException("This friendship is not active")
        }

        friendshipsCollection.document(friendshipId).delete().await()

        usersCollection.document(friendship.senderId).update(
            "friendCount",
            com.google.firebase.firestore.FieldValue.increment(-1)
        ).await()

        usersCollection.document(friendship.receiverId).update(
            "friendCount",
            com.google.firebase.firestore.FieldValue.increment(-1)
        ).await()

        true
    }

    suspend fun cancelFriendRequest(friendshipId: String): Boolean = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

        try {
            val friendshipDoc = friendshipsCollection.document(friendshipId).get().await()
            if (!friendshipDoc.exists()) {
                throw IllegalStateException("Friend request with ID $friendshipId not found")
            }

            val friendship = friendshipDoc.toObject(Friendship::class.java)
                ?: throw IllegalStateException("Failed to convert document to Friendship")

            if (friendship.senderId != currentUser.uid) {
                throw IllegalStateException("Only the sender can cancel a friend request")
            }

            if (friendship.status != FriendshipStatus.PENDING) {
                throw IllegalStateException("This friend request is not in pending state")
            }

            friendshipsCollection.document(friendshipId).delete().await()

            true
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getPendingFriendRequests(): Flow<List<Friendship>> = flow {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

        try {
            val snapshot = friendshipsCollection
                .whereEqualTo("receiverId", currentUser.uid)
                .whereEqualTo("status", FriendshipStatus.PENDING)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val pendingRequests = snapshot.documents.mapNotNull { doc ->
                val friendship = doc.toObject(Friendship::class.java)
                if (friendship != null) {
                    friendship.copy(id = doc.id)
                } else {
                    null
                }
            }

            val updatedRequests = pendingRequests.map { friendship ->
                try {
                    val senderDoc = usersCollection.document(friendship.senderId).get().await()
                    val sender = senderDoc.toObject(User::class.java)

                    if (sender != null) {
                        friendship.copy(
                            senderPhotoUrl = sender.photoUrl ?: friendship.senderPhotoUrl,
                            senderName = sender.displayName
                        )
                    } else {
                        friendship
                    }
                } catch (e: Exception) {
                    friendship
                }
            }

            emit(updatedRequests)
        } catch (e: Exception) {
            emit(emptyList<Friendship>())
        }
    }

    suspend fun getSentFriendRequests(): Flow<List<Friendship>> = flow {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

        try {
            val snapshot = friendshipsCollection
                .whereEqualTo("senderId", currentUser.uid)
                .whereEqualTo("status", FriendshipStatus.PENDING)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val sentRequests = snapshot.documents.mapNotNull { doc ->
                val friendship = doc.toObject(Friendship::class.java)
                if (friendship != null) {
                    friendship.copy(id = doc.id)
                } else {
                    null
                }
            }

            val updatedRequests = sentRequests.map { friendship ->
                try {
                    val receiverDoc = usersCollection.document(friendship.receiverId).get().await()
                    val receiver = receiverDoc.toObject(User::class.java)

                    if (receiver != null) {
                        friendship.copy(
                            receiverPhotoUrl = receiver.photoUrl ?: friendship.receiverPhotoUrl,
                            receiverName = receiver.displayName
                        )
                    } else {
                        friendship
                    }
                } catch (e: Exception) {
                    friendship
                }
            }

            emit(updatedRequests)
        } catch (e: Exception) {
            emit(emptyList<Friendship>())
        }
    }

    suspend fun getFriends(): Flow<List<Friendship>> = flow {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

        try {
            val senderSnapshot = friendshipsCollection
                .whereEqualTo("senderId", currentUser.uid)
                .whereEqualTo("status", FriendshipStatus.ACCEPTED)
                .get()
                .await()

            val receiverSnapshot = friendshipsCollection
                .whereEqualTo("receiverId", currentUser.uid)
                .whereEqualTo("status", FriendshipStatus.ACCEPTED)
                .get()
                .await()

            val senderFriendships = senderSnapshot.documents.mapNotNull { doc ->
                val friendship = doc.toObject(Friendship::class.java)
                if (friendship != null) {
                    friendship.copy(id = doc.id)
                } else {
                    null
                }
            }

            val receiverFriendships = receiverSnapshot.documents.mapNotNull { doc ->
                val friendship = doc.toObject(Friendship::class.java)
                if (friendship != null) {
                    friendship.copy(id = doc.id)
                } else {
                    null
                }
            }

            val updatedSenderFriendships = senderFriendships.map { friendship ->
                try {
                    val receiverDoc = usersCollection.document(friendship.receiverId).get().await()
                    val receiver = receiverDoc.toObject(User::class.java)

                    if (receiver != null) {
                        friendship.copy(
                            receiverPhotoUrl = receiver.photoUrl ?: friendship.receiverPhotoUrl,
                            receiverName = receiver.displayName
                        )
                    } else {
                        friendship
                    }
                } catch (e: Exception) {
                    friendship
                }
            }

            val updatedReceiverFriendships = receiverFriendships.map { friendship ->
                try {
                    val senderDoc = usersCollection.document(friendship.senderId).get().await()
                    val sender = senderDoc.toObject(User::class.java)

                    if (sender != null) {
                        friendship.copy(
                            senderPhotoUrl = sender.photoUrl ?: friendship.senderPhotoUrl,
                            senderName = sender.displayName
                        )
                    } else {
                        friendship
                    }
                } catch (e: Exception) {
                    friendship
                }
            }

            val allFriendships = (updatedSenderFriendships + updatedReceiverFriendships)
                .sortedByDescending { it.updatedAt }

            emit(allFriendships)
        } catch (e: Exception) {
            emit(emptyList<Friendship>())
        }
    }

    suspend fun searchUsers(query: String): List<User> = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

        try {
            val snapshot = usersCollection
                .orderBy("displayName")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(20)
                .get()
                .await()

            val users = snapshot.documents.mapNotNull { doc ->
                try {
                    if (doc.id == currentUser.uid) {
                        return@mapNotNull null
                    }

                    val userData = doc.toObject(User::class.java)
                    if (userData != null) {
                        userData.copy(id = doc.id, uid = doc.id)
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }

            val friendships = getFriendshipStatuses(currentUser.uid, users.map { it.id })

            users.map { user ->
                val friendship = friendships[user.id]
                val isFollowed = friendship != null &&
                        (friendship.status == FriendshipStatus.ACCEPTED ||
                                (friendship.status == FriendshipStatus.PENDING && friendship.senderId == currentUser.uid))

                try {
                    val latestUserDoc = usersCollection.document(user.id).get().await()
                    val latestUser = latestUserDoc.toObject(User::class.java)
                    if (latestUser != null) {
                        latestUser.copy(
                            id = user.id,
                            uid = user.id,
                            isFollowedByCurrentUser = isFollowed
                        )
                    } else {
                        user.copy(isFollowedByCurrentUser = isFollowed)
                    }
                } catch (e: Exception) {
                    user.copy(isFollowedByCurrentUser = isFollowed)
                }
            }
        } catch (e: Exception) {
            emptyList<User>()
        }
    }

    private suspend fun checkExistingFriendship(userId1: String, userId2: String): Friendship? =
        withContext(Dispatchers.IO) {
            try {
                val query1 = friendshipsCollection
                    .whereEqualTo("senderId", userId1)
                    .whereEqualTo("receiverId", userId2)
                    .get()
                    .await()

                if (!query1.isEmpty) {
                    val doc = query1.documents.first()
                    val friendship = doc.toObject(Friendship::class.java)
                    if (friendship != null) {
                        return@withContext friendship.copy(id = doc.id)
                    }
                }

                val query2 = friendshipsCollection
                    .whereEqualTo("senderId", userId2)
                    .whereEqualTo("receiverId", userId1)
                    .get()
                    .await()

                if (!query2.isEmpty) {
                    val doc = query2.documents.first()
                    val friendship = doc.toObject(Friendship::class.java)
                    if (friendship != null) {
                        return@withContext friendship.copy(id = doc.id)
                    }
                }

                return@withContext null
            } catch (e: Exception) {
                return@withContext null
            }
        }

    private suspend fun getFriendshipStatuses(
        userId: String,
        otherUserIds: List<String>
    ): Map<String, Friendship> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, Friendship>()

        try {
            if (otherUserIds.isEmpty()) {
                return@withContext emptyMap<String, Friendship>()
            }

            val chunks = otherUserIds.chunked(10)

            for (chunk in chunks) {
                val senderQuery = friendshipsCollection
                    .whereEqualTo("senderId", userId)
                    .whereIn("receiverId", chunk)
                    .get()
                    .await()

                for (doc in senderQuery.documents) {
                    val friendship = doc.toObject(Friendship::class.java) ?: continue
                    result[friendship.receiverId] = friendship.copy(id = doc.id)
                }

                val receiverQuery = friendshipsCollection
                    .whereEqualTo("receiverId", userId)
                    .whereIn("senderId", chunk)
                    .get()
                    .await()

                for (doc in receiverQuery.documents) {
                    val friendship = doc.toObject(Friendship::class.java) ?: continue
                    result[friendship.senderId] = friendship.copy(id = doc.id)
                }
            }

            return@withContext result
        } catch (e: Exception) {
            return@withContext emptyMap<String, Friendship>()
        }
    }
}