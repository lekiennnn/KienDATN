package com.example.nam.repository

import android.net.Uri
import android.util.Log
import com.example.nam.data.Comment
import com.example.nam.data.Friendship
import com.example.nam.data.FriendshipStatus
import com.example.nam.data.Notification
import com.example.nam.data.Post
import com.example.nam.data.SearchHistory
import com.example.nam.data.SharedPost
import com.example.nam.data.User
import com.example.nam.data.PostVisibility
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val usersCollection = firestore.collection("users")
    private val userDocument = { userId: String -> usersCollection.document(userId) }
    private val postsCollection = firestore.collection("posts")
    private val commentsCollection = firestore.collection("comments")
    private val likesCollection = firestore.collection("likes")
    private val commentLikesCollection = firestore.collection("commentLikes")
    private val friendshipsCollection = firestore.collection("friendships")
    private val notificationRepository = NotificationRepository()
    private val searchHistoryCollection = firestore.collection("searchHistory")
    private val sharedPostsCollection = firestore.collection("sharedPosts")

    fun getCurrentFirebaseUser() = auth.currentUser

    suspend fun createUserProfile(user: User) = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")
        val userData = user.copy(id = currentUser.uid, email = currentUser.email ?: "")
        usersCollection.document(currentUser.uid).set(userData).await()
    }

    suspend fun cancelFriendRequest(targetUserId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

            // Find pending friendship where current user is sender
            val query = friendshipsCollection
                .whereEqualTo("senderId", currentUser.uid)
                .whereEqualTo("receiverId", targetUserId)
                .whereEqualTo("status", FriendshipStatus.PENDING.name)
                .get()
                .await()

            if (query.isEmpty) {
                Log.d("FirebaseRepository", "No pending friend request found to cancel")
                return@withContext false
            }

            // Delete the friendship request
            val friendshipDoc = query.documents.first()
            friendshipsCollection.document(friendshipDoc.id).delete().await()

            Log.d(
                "FirebaseRepository",
                "Friend request canceled: ${currentUser.uid} -> $targetUserId"
            )
            return@withContext true
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error canceling friend request: ${e.message}", e)
            return@withContext false
        }
    }

    suspend fun getCurrentUser(): User? = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: return@withContext null
            val userDoc = usersCollection.document(currentUser.uid).get().await()

            if (!userDoc.exists()) {
                Log.e("FirebaseRepository", "User document doesn't exist for UID: ${currentUser.uid}")
                return@withContext null
            } else {
                Log.d("FirebaseRepository", "Retrieved user doc data: ${userDoc.data}")
            }

            val user = userDoc.toObject(User::class.java)

            if (user != null && user.id.isBlank()) {
                Log.d("FirebaseRepository", "User exists but has blank ID, using UID: ${currentUser.uid}")
                return@withContext user.copy(id = currentUser.uid)
            }

            return@withContext user
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error getting current user", e)
            throw e
        }
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val currentUser = auth.currentUser ?: return@withContext Result.failure(
                    IllegalStateException("No user logged in")
                )
                val email = currentUser.email ?: return@withContext Result.failure(
                    IllegalStateException("User has no email")
                )

                val credential =
                    com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPassword)
                currentUser.reauthenticate(credential).await()

                currentUser.updatePassword(newPassword).await()
                return@withContext Result.success(Unit)
            } catch (e: Exception) {
                Log.e("FirebaseRepository", "Error changing password", e)
                return@withContext Result.failure(e)
            }
        }

    suspend fun updateUserProfile(displayName: String, bio: String?, photoUri: Uri?): User = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

        var photoUrl = getCurrentUser()?.photoUrl
        if (photoUri != null) {
            try {
                photoUrl = uploadImage(photoUri, "profiles")
            } catch (e: Exception) {
                Log.e("FirebaseRepository", "Failed to upload profile photo: ${e.message}")
            }
        }

        val userData = mapOf(
            "displayName" to displayName,
            "bio" to bio,
            "photoUrl" to photoUrl
        )

        usersCollection.document(currentUser.uid).update(userData).await()

        updateUserPostsProfilePicture(photoUrl, displayName)

        updateUserCommentsDisplayName(displayName)

        getCurrentUser() ?: throw IllegalStateException("Failed to get updated user")
    }

    suspend fun updateUserProfileWithCloudinaryUrl(
        displayName: String,
        bio: String?,
        cloudinaryUrl: String?
    ): User = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

        val userData = mapOf(
            "displayName" to displayName,
            "bio" to bio,
            "photoUrl" to cloudinaryUrl
        )

        usersCollection.document(currentUser.uid).update(userData).await()

        updateUserPostsProfilePicture(cloudinaryUrl, displayName)

        updateUserCommentsDisplayName(displayName)

        getCurrentUser() ?: throw IllegalStateException("Failed to get updated user")
    }

    private suspend fun updateUserPostsProfilePicture(
        profilePictureUrl: String?,
        displayName: String? = null
    ) =
        withContext(Dispatchers.IO) {
            try {
                val currentUser = auth.currentUser ?: return@withContext

                val userPosts = postsCollection
                    .whereEqualTo("userId", currentUser.uid)
                    .get()
                    .await()

                if (!userPosts.isEmpty) {
                    val batch = firestore.batch()

                    for (postDoc in userPosts.documents) {
                        if (profilePictureUrl != null && displayName != null) {
                            batch.update(
                                postDoc.reference,
                                mapOf(
                                    "userProfilePicture" to profilePictureUrl,
                                    "userDisplayName" to displayName
                                )
                            )
                        } else if (profilePictureUrl != null) {
                            batch.update(
                                postDoc.reference,
                                "userProfilePicture",
                                profilePictureUrl
                            )
                        } else if (displayName != null) {
                            batch.update(
                                postDoc.reference,
                                "userDisplayName",
                                displayName
                            )
                        }
                    }

                    batch.commit().await()

                    val updatedFields = mutableListOf<String>()
                    if (profilePictureUrl != null) updatedFields.add("profile picture")
                    if (displayName != null) updatedFields.add("display name")

                    Log.d("FirebaseRepository", "Updated ${updatedFields.joinToString(" and ")} in ${userPosts.size()} posts")
                }
            } catch (e: Exception) {
                val updatedFields = mutableListOf<String>()
                if (profilePictureUrl != null) updatedFields.add("profile picture")
                if (displayName != null) updatedFields.add("display name")
                Log.e("FirebaseRepository", "Error updating ${updatedFields.joinToString(" and ")} in posts: ${e.message}", e)
            }
        }

    private suspend fun updateUserCommentsDisplayName(displayName: String) =
        withContext(Dispatchers.IO) {
            try {
                val currentUser = auth.currentUser ?: return@withContext

                val userComments = commentsCollection
                    .whereEqualTo("userId", currentUser.uid)
                    .get()
                    .await()

                var totalUpdated = 0

                if (!userComments.isEmpty) {
                    val batch = firestore.batch()

                    for (commentDoc in userComments.documents) {
                        batch.update(
                            commentDoc.reference,
                            "userDisplayName",
                            displayName
                        )
                    }

                    batch.commit().await()
                    totalUpdated += userComments.size()
                }

                val userReplies = commentsCollection
                    .whereEqualTo("userId", currentUser.uid)
                    .whereGreaterThan("parentCommentId", "")
                    .get()
                    .await()

                if (!userReplies.isEmpty) {
                    val batch = firestore.batch()

                    for (replyDoc in userReplies.documents) {
                        batch.update(
                            replyDoc.reference,
                            "userDisplayName",
                            displayName
                        )
                    }

                    batch.commit().await()
                    totalUpdated += userReplies.size()
                }

                Log.d("FirebaseRepository", "Updated display name in $totalUpdated comments and replies")
            } catch (e: Exception) {
                Log.e("FirebaseRepository", "Error updating display name in comments and replies: ${e.message}", e)
            }
        }

    suspend fun getUserById(userId: String): User? = withContext(Dispatchers.IO) {
        try {
            val userDoc = usersCollection.document(userId).get().await()

            if (!userDoc.exists()) {
                Log.e("FirebaseRepository", "User document doesn't exist for UID: $userId")
                return@withContext null
            }

            return@withContext userDoc.toObject(User::class.java)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error getting user by ID", e)
            throw e
        }
    }

    suspend fun getUserPostsById(userId: String): List<Post> = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser

        try {
            Log.d("FirebaseRepository", "Getting posts for user ID: $userId")
            val postsQuery = postsCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)

            val snapshot = firestore.collection("posts")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            Log.d("FirebaseRepository", "Found ${snapshot.size()} posts")

            val posts = snapshot.toObjects(Post::class.java)
            Log.d("FirebaseRepository", "Posts before filtering: ${posts.size}")

            // Log the visibility of each post for debugging
            posts.forEach { post ->
                Log.d("FirebaseRepository", "Post ${post.id}: visibility=${post.visibility}")
            }



            Log.d("DCM", "Documents found = ${snapshot.documents.size}")

            if (currentUser != null) {
                val userLikes = likesCollection
                    .whereEqualTo("userId", currentUser.uid)
                    .get()
                    .await()
                    .documents
                    .map { it.getString("postId") ?: "" }
                    .toSet()

                // Apply visibility filtering if the profile is not of the current user
                if (currentUser.uid != userId) {
                    Log.d("FirebaseRepository", "Viewing another user's profile: $userId")

                    // Check if current user is friends with the profile user
                    val friendships = getFriendUserIds(currentUser.uid)
                    val isFriend = friendships.contains(userId)

                    Log.d("FirebaseRepository", "Is friends with profile user: $isFriend")

                    // Filter posts based on visibility and friendship status
                    val filteredPosts = posts.filter { post ->
                        val canSee = when (post.visibility) {
                            PostVisibility.PUBLIC -> true // Anyone can see public posts
                            PostVisibility.FRIENDS_ONLY -> isFriend // Only friends can see friends-only posts
                            PostVisibility.PRIVATE -> false // No one else can see private posts
                        }

                        Log.d(
                            "FirebaseRepository",
                            "Post ${post.id}: visibility=${post.visibility}, canSee=$canSee"
                        )
                        canSee
                    }

                    Log.d("FirebaseRepository", "Filtered posts count: ${filteredPosts.size}")

                    return@withContext filteredPosts.map { post ->
                        post.copy(isLikedByCurrentUser = userLikes.contains(post.id))
                    }
                } else {
                    // Return all posts for the current user's own profile
                    Log.d(
                        "FirebaseRepository",
                        "Viewing own profile, returning all posts: ${posts.size}"
                    )
                    return@withContext posts.map { post ->
                        post.copy(isLikedByCurrentUser = userLikes.contains(post.id))
                    }
                }
            } else {
                // Not logged in, only show public posts
                Log.d("FirebaseRepository", "Not logged in, returning only public posts")
                return@withContext posts.filter { it.visibility == PostVisibility.PUBLIC }
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error getting user posts: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    suspend fun getUserPosts(): List<Post> = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

        try {
            Log.d("FirebaseRepository", "Getting posts for user: ${currentUser.uid}")
            val postsQuery = postsCollection
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)

            Log.d("FirebaseRepository", "Query: posts collection with userId=${currentUser.uid}")

            val snapshot = postsQuery.get().await()
            Log.d("FirebaseRepository", "Found ${snapshot.size()} posts")

            val posts = snapshot.toObjects(Post::class.java)

            val userLikes = likesCollection
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()
                .documents
                .map { it.getString("postId") ?: "" }
                .toSet()

            return@withContext posts.map { post ->
                post.copy(isLikedByCurrentUser = userLikes.contains(post.id))
            }
        } catch (e: Exception) {
            val posts = postsCollection
                .get()
                .await()
                .toObjects(Post::class.java)
                .filter { it.userId == currentUser.uid }
                .sortedByDescending { it.createdAt }

            return@withContext posts
        }
    }

    suspend fun createPost(text: String, imageUri: Uri?): Post = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

        val userDisplayName = currentUser.displayName ?: "Unknown User"
        val userProfile = getCurrentUser()
        val userProfilePicture = userProfile?.photoUrl

        var imageUrl: String? = null
        if (imageUri != null) {
            try {
                imageUrl = uploadImage(imageUri, "posts")
            } catch (e: Exception) {
                Log.e("FirebaseRepository", "Failed to upload image: ${e.message}")
            }
        }

        val postId = postsCollection.document().id
        val post = Post(
            id = postId,
            userId = currentUser.uid,
            userDisplayName = userDisplayName,
            userProfilePicture = userProfilePicture,
            text = text,
            imageUrl = imageUrl,
            createdAt = Timestamp.now()
        )

        postsCollection.document(postId).set(post).await()
        post
    }

    suspend fun createPostWithImageUrl(text: String, imageUrl: String?): Post =
        withContext(Dispatchers.IO) {
            val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

            val userDisplayName = currentUser.displayName ?: "Unknown User"
            val userProfile = getCurrentUser()
            val userProfilePicture = userProfile?.photoUrl

            val postId = postsCollection.document().id
            val post = Post(
                id = postId,
                userId = currentUser.uid,
                userDisplayName = userDisplayName,
                userProfilePicture = userProfilePicture,
                text = text,
                imageUrl = imageUrl,
                createdAt = Timestamp.now()
            )

            postsCollection.document(postId).set(post).await()
            post
        }

    suspend fun createPostWithMediaUrl(
        text: String,
        mediaUrl: String?,
        isVideo: Boolean = false,
        visibility: PostVisibility = PostVisibility.PUBLIC
    ): Post =
        withContext(Dispatchers.IO) {
            val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

            val userDisplayName = currentUser.displayName ?: "Unknown User"
            val userProfile = getCurrentUser()
            val userProfilePicture = userProfile?.photoUrl

            val postId = postsCollection.document().id
            val post = if (isVideo) {
                Post(
                    id = postId,
                    userId = currentUser.uid,
                    userDisplayName = userDisplayName,
                    userProfilePicture = userProfilePicture,
                    text = text,
                    videoUrl = mediaUrl,
                    hasVideo = true,
                    createdAt = Timestamp.now(),
                    isPrivate = visibility == PostVisibility.PRIVATE,
                    visibility = visibility
                )
            } else {
                Post(
                    id = postId,
                    userId = currentUser.uid,
                    userDisplayName = userDisplayName,
                    userProfilePicture = userProfilePicture,
                    text = text,
                    imageUrl = mediaUrl,
                    createdAt = Timestamp.now(),
                    isPrivate = visibility == PostVisibility.PRIVATE,
                    visibility = visibility
                )
            }

            postsCollection.document(postId).set(post).await()
            post
        }

    suspend fun getPosts(): List<Post> = withContext(Dispatchers.IO) {
        postsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()
            .toObjects(Post::class.java)
    }

    suspend fun getPostsWithLikeStatus(): List<Post> = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext emptyList<Post>()

        try {
            val posts = postsCollection
                .get()
                .await()
                .toObjects(Post::class.java)
                .sortedByDescending { it.createdAt }

            val userLikes = likesCollection
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()
                .documents
                .map { it.getString("postId") ?: "" }
                .toSet()

            val friendships = getFriendUserIds(currentUser.uid)

            val userSharedPosts = sharedPostsCollection
                .whereEqualTo("sharedByUserId", currentUser.uid)
                .get()
                .await()
                .documents
                .mapNotNull { it.getString("originalPostId") }
                .toSet()

            // Get posts archived by current user
            val userArchivedPosts = archivedPostsCollection
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()
                .documents
                .mapNotNull { it.getString("postId") }
                .toSet()

            posts.forEach { post ->
                if (post.imageUrl != null) {
                    Log.d("FirebaseRepository", "Post ID: ${post.id}, Image URL: ${post.imageUrl}")
                    try {
                        val storageRef = storage.getReferenceFromUrl(post.imageUrl!!)
                        Log.d("FirebaseRepository", "Storage path: ${storageRef.path}")
                    } catch (e: Exception) {
                        Log.e("FirebaseRepository", "Invalid image URL: ${post.imageUrl}", e)
                    }
                }
            }

            // Apply visibility filters
            val filteredPosts = posts.filter { post ->
                when (post.visibility) {
                    PostVisibility.PUBLIC -> true // Everyone can see public posts
                    PostVisibility.FRIENDS_ONLY -> {
                        // Only friends of the post author or the author can see these posts
                        post.userId == currentUser.uid || friendships.contains(post.userId)
                    }
                    PostVisibility.PRIVATE -> {
                        // Only the author can see their own private posts
                        post.userId == currentUser.uid
                    }
                }
            }

            return@withContext filteredPosts.map { post ->
                post.copy(
                    isLikedByCurrentUser = userLikes.contains(post.id),
                    isSharedByCurrentUser = userSharedPosts.contains(post.id),
                    isArchivedByCurrentUser = userArchivedPosts.contains(post.id)
                )
            }
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }

    // Helper method to get friend user IDs
    private suspend fun getFriendUserIds(userId: String): Set<String> =
        withContext(Dispatchers.IO) {
            try {
                val senderFriendships = friendshipsCollection
                    .whereEqualTo("senderId", userId)
                    .whereEqualTo("status", FriendshipStatus.ACCEPTED.name)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.getString("receiverId") }
                    .toSet()

            val receiverFriendships = friendshipsCollection
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("status", FriendshipStatus.ACCEPTED.name)
                .get()
                .await()
                .documents
                .mapNotNull { it.getString("senderId") }
                .toSet()

            return@withContext senderFriendships + receiverFriendships
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error getting friend user IDs", e)
            return@withContext emptySet<String>()
        }
    }

    suspend fun getFriendsPostsWithLikeStatus(): List<Post> = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext emptyList<Post>()

        try {
            val friendships = getFriendUserIds(currentUser.uid)

            if (friendships.isEmpty()) {
                Log.d("FirebaseRepository", "User has no friends, returning empty list")
                return@withContext emptyList()
            }

            Log.d("FirebaseRepository", "Found ${friendships.size} friends")

            friendships.forEach { friendId ->
                Log.d("FirebaseRepository", "Friend ID: $friendId")
            }

            val allPosts = postsCollection
                .get()
                .await()
                .toObjects(Post::class.java)

            Log.d("FirebaseRepository", "Found ${allPosts.size} total posts")

            // Apply proper visibility filters
            val visibleFriendPosts = allPosts.filter { post ->
                friendships.contains(post.userId) && // Only include posts from friends
                        when (post.visibility) {
                            PostVisibility.PUBLIC -> true // Friends can see public posts
                            PostVisibility.FRIENDS_ONLY -> true // Friends can see friends-only posts
                            PostVisibility.PRIVATE -> false // No one can see private posts except the author
                        }
            }

            val sharedPosts = getSharedPostsForFriends(friendships)
            Log.d("FirebaseRepository", "Found ${sharedPosts.size} shared posts")

            val currentUserSharedPosts = getPostsSharedByCurrentUser()
            Log.d("FirebaseRepository", "Found ${currentUserSharedPosts.size} posts shared by current user")

            val combinedPosts =
                (visibleFriendPosts + sharedPosts + currentUserSharedPosts).sortedByDescending { it.createdAt }

            Log.d("FirebaseRepository", "Filtered to ${combinedPosts.size} friend posts")

            val userLikes = likesCollection
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()
                .documents
                .mapNotNull { it.getString("postId") }
                .toSet()

            val userSharedPosts = sharedPostsCollection
                .whereEqualTo("sharedByUserId", currentUser.uid)
                .get()
                .await()
                .documents
                .mapNotNull { it.getString("originalPostId") }
                .toSet()

            // Get posts archived by current user
            val userArchivedPosts = archivedPostsCollection
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()
                .documents
                .mapNotNull { it.getString("postId") }
                .toSet()

            return@withContext combinedPosts.map { post ->
                post.copy(
                    isLikedByCurrentUser = userLikes.contains(post.id),
                    isSharedByCurrentUser = userSharedPosts.contains(post.id),
                    isArchivedByCurrentUser = userArchivedPosts.contains(post.id)
                )
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error getting friends' posts", e)
            return@withContext emptyList()
        }
    }

    private suspend fun getPostsSharedByCurrentUser(): List<Post> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: return@withContext emptyList<Post>()

            val sharedPostDocs = sharedPostsCollection
                .whereEqualTo("sharedByUserId", currentUser.uid)
                .get()
                .await()
                .documents

            if (sharedPostDocs.isEmpty()) {
                return@withContext emptyList<Post>()
            }

            Log.d("FirebaseRepository", "Found ${sharedPostDocs.size} posts shared by current user")

            val userSharedPosts = sharedPostDocs
                .mapNotNull { it.getString("originalPostId") }
                .toSet()

            val result = mutableListOf<Post>()

            for (sharedPostDoc in sharedPostDocs) {
                val sharedPost = sharedPostDoc.toObject(SharedPost::class.java) ?: continue

                Log.d(
                    "FirebaseRepository",
                    "Processing post shared by current user: ${sharedPost.id}, original post: ${sharedPost.originalPostId}, isPrivate: ${sharedPost.isPrivate}"
                )

                val originalPostId = sharedPost.originalPostId

                val originalPost = getPostById(originalPostId) ?: continue

                val combinedPost = originalPost.copy(
                    isSharedPost = true,
                    sharedByUserId = sharedPost.sharedByUserId,
                    sharedByUserName = sharedPost.sharedByUserName,
                    isSharedByCurrentUser = true,
                    isPrivate = sharedPost.isPrivate
                )

                result.add(combinedPost)
            }

            return@withContext result
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error getting posts shared by current user", e)
            return@withContext emptyList<Post>()
        }
    }

    suspend fun sharePost(postId: String, isPrivate: Boolean = false): Boolean =
        withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

        try {
            val post = getPostById(postId) ?: throw IllegalStateException("Post not found")

            // Check if trying to share a private post with friends (non-private sharing)
            if (post.visibility == PostVisibility.PRIVATE && !isPrivate) {
                Log.e("FirebaseRepository", "Cannot share a private post with friends")
                return@withContext false
            }

            val existingShareQuery = sharedPostsCollection
                .whereEqualTo("sharedByUserId", currentUser.uid)
                .whereEqualTo("originalPostId", postId)
                .get()
                .await()

            if (!existingShareQuery.isEmpty) {
                Log.d("FirebaseRepository", "Found existing share, removing it")

                val shareDoc = existingShareQuery.documents.first()
                sharedPostsCollection.document(shareDoc.id).delete().await()

                postsCollection.document(postId).update("shareCount", FieldValue.increment(-1))
                    .await()

                return@withContext false
            }

            val user = getCurrentUser() ?: throw IllegalStateException("Failed to get user profile")

            val sharedPostId = sharedPostsCollection.document().id
            val sharedPost = SharedPost(
                id = sharedPostId,
                originalPostId = postId,
                sharedByUserId = currentUser.uid,
                sharedByUserName = user.displayName,
                sharedByUserProfilePicture = user.photoUrl,
                createdAt = Timestamp.now(),
                isPrivate = isPrivate
            )

            Log.d(
                "FirebaseRepository",
                "Creating shared post: $sharedPostId, original: $postId, sharedBy: ${user.displayName}, isPrivate: $isPrivate"
            )

            sharedPostsCollection.document(sharedPostId).set(sharedPost).await()

            postsCollection.document(postId).update("shareCount", FieldValue.increment(1)).await()

            return@withContext true
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error sharing post", e)
            return@withContext false
        }
    }

    private suspend fun getSharedPostsForFriends(friendIds: Set<String>): List<Post> =
        withContext(Dispatchers.IO) {
            try {
                val currentUser = auth.currentUser ?: return@withContext emptyList<Post>()

                val sharedPostDocs = sharedPostsCollection
                    .get()
                    .await()
                    .documents
                    .filter { doc ->
                        val sharedByUserId = doc.getString("sharedByUserId")
                        val isPrivate = doc.getBoolean("isPrivate") ?: false
                        friendIds.contains(sharedByUserId) && !isPrivate
                    }

                if (sharedPostDocs.isEmpty()) {
                    return@withContext emptyList<Post>()
                }

                Log.d("FirebaseRepository", "Found ${sharedPostDocs.size} shared post documents")

                val userSharedPosts = sharedPostsCollection
                    .whereEqualTo("sharedByUserId", currentUser.uid)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.getString("originalPostId") }
                    .toSet()

                Log.d("FirebaseRepository", "Current user has shared ${userSharedPosts.size} posts: $userSharedPosts")

                val result = mutableListOf<Post>()

                for (sharedPostDoc in sharedPostDocs) {
                    val sharedPost = sharedPostDoc.toObject(SharedPost::class.java) ?: continue

                    Log.d("FirebaseRepository", "Processing shared post: ${sharedPost.id}, original post: ${sharedPost.originalPostId}")

                    val originalPostId = sharedPost.originalPostId

                    val originalPost = getPostById(originalPostId) ?: continue

                    val combinedPost = originalPost.copy(
                        isSharedPost = true,
                        sharedByUserId = sharedPost.sharedByUserId,
                        sharedByUserName = sharedPost.sharedByUserName,
                        isSharedByCurrentUser = userSharedPosts.contains(originalPostId)
                    )

                    Log.d("FirebaseRepository", "Added shared post with sharer: ${sharedPost.sharedByUserName}, isSharedByCurrentUser=${userSharedPosts.contains(originalPostId)}")

                    result.add(combinedPost)
                }

                return@withContext result
            } catch (e: Exception) {
                Log.e("FirebaseRepository", "Error getting shared posts", e)
                return@withContext emptyList<Post>()
            }
        }

    suspend fun searchPosts(query: String): List<Post> = withContext(Dispatchers.IO) {
        if (query.length < 2) return@withContext emptyList()

        val currentUser = auth.currentUser ?: return@withContext emptyList()

        if (query.isNotEmpty()) {
            try {
                saveSearchQuery(query)
            } catch (e: Exception) {
                Log.e("FirebaseRepository", "Error saving search query: ${e.message}", e)
            }
        }

        try {
            val allPosts = postsCollection
                .get()
                .await()
                .toObjects(Post::class.java)
                .sortedByDescending { it.createdAt }

            val userLikes = likesCollection
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()
                .documents
                .mapNotNull { it.getString("postId") }
                .toSet()

            val friendships = getFriendUserIds(currentUser.uid)

            // Apply proper visibility filters
            val filteredPosts = allPosts.filter { post ->
                post.text.contains(query, ignoreCase = true) &&
                        when (post.visibility) {
                            PostVisibility.PUBLIC -> true // Anyone can search and find public posts
                            PostVisibility.FRIENDS_ONLY -> {
                                // Only friends or the author can find friends-only posts
                                post.userId == currentUser.uid || friendships.contains(post.userId)
                            }

                            PostVisibility.PRIVATE -> {
                                // Only the author can find their private posts
                                post.userId == currentUser.uid
                            }
                        }
            }

            return@withContext filteredPosts.map { post ->
                post.copy(isLikedByCurrentUser = userLikes.contains(post.id))
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error searching posts: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    suspend fun addComment(postId: String, text: String, imageUri: Uri?): Comment = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

        val userDisplayName = currentUser.displayName ?: "Unknown User"

        var imageUrl: String? = null
        if (imageUri != null) {
            try {
                imageUrl = uploadImage(imageUri, "comments")
            } catch (e: Exception) {
                Log.e("FirebaseRepository", "Failed to upload comment image: ${e.message}")
            }
        }

        val commentId = commentsCollection.document().id
        val comment = Comment(
            id = commentId,
            postId = postId,
            userId = currentUser.uid,
            userDisplayName = userDisplayName,
            text = text,
            imageUrl = imageUrl,
            createdAt = Timestamp.now()
        )

        commentsCollection.document(commentId).set(comment).await()

        postsCollection.document(postId).update("commentCount", FieldValue.increment(1)).await()

        val post = getPostById(postId) ?: return@withContext comment
        val user = getCurrentUser() ?: return@withContext comment
        if (post.userId != user.id) {
            notificationRepository.createCommentNotification(post, commentId, user)
        } else {
            Log.d("FirebaseRepository", "Skipping self-notification for comment")
        }

        comment
    }

    suspend fun addCommentWithImageUrl(postId: String, text: String, imageUrl: String?): Comment =
        withContext(Dispatchers.IO) {
            val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

            val userDisplayName = currentUser.displayName ?: "Unknown User"

            val commentId = commentsCollection.document().id
            val comment = Comment(
                id = commentId,
                postId = postId,
                userId = currentUser.uid,
                userDisplayName = userDisplayName,
                text = text,
                imageUrl = imageUrl,
                createdAt = Timestamp.now()
            )

            commentsCollection.document(commentId).set(comment).await()

            postsCollection.document(postId).update("commentCount", FieldValue.increment(1)).await()

            val post = getPostById(postId) ?: return@withContext comment
            val user = getCurrentUser() ?: return@withContext comment
            if (post.userId != user.id) {
                notificationRepository.createCommentNotification(post, commentId, user)
            } else {
                Log.d("FirebaseRepository", "Skipping self-notification for comment")
            }

            comment
        }

    suspend fun addCommentWithMediaUrl(
        postId: String,
        text: String,
        mediaUrl: String?,
        isVideo: Boolean = false
    ): Comment =
        withContext(Dispatchers.IO) {
            val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

            val userDisplayName = currentUser.displayName ?: "Unknown User"

            val commentId = commentsCollection.document().id
            val comment = if (isVideo) {
                Comment(
                    id = commentId,
                    postId = postId,
                    userId = currentUser.uid,
                    userDisplayName = userDisplayName,
                    text = text,
                    videoUrl = mediaUrl,
                    hasVideo = true,
                    createdAt = Timestamp.now()
                )
            } else {
                Comment(
                    id = commentId,
                    postId = postId,
                    userId = currentUser.uid,
                    userDisplayName = userDisplayName,
                    text = text,
                    imageUrl = mediaUrl,
                    createdAt = Timestamp.now()
                )
            }

            commentsCollection.document(commentId).set(comment).await()

            postsCollection.document(postId).update("commentCount", FieldValue.increment(1)).await()

            val post = getPostById(postId) ?: return@withContext comment
            val user = getCurrentUser() ?: return@withContext comment
            if (post.userId != user.id) {
                notificationRepository.createCommentNotification(post, commentId, user)
            } else {
                Log.d("FirebaseRepository", "Skipping self-notification for comment")
            }

            comment
        }

    suspend fun getCommentsForPost(postId: String): List<Comment> = withContext(Dispatchers.IO) {
        try {
            commentsCollection
                .whereEqualTo("postId", postId)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .await()
                .toObjects(Comment::class.java)
        } catch (e: Exception) {
            commentsCollection
                .get()
                .await()
                .toObjects(Comment::class.java)
                .filter { it.postId == postId }
                .sortedBy { it.createdAt }
        }
    }

    suspend fun getPostById(postId: String): Post? = withContext(Dispatchers.IO) {
        val postDoc = postsCollection.document(postId).get().await()
        val post = postDoc.toObject(Post::class.java)

        if (post != null) {
            Log.d("FirebaseRepository", "Retrieved post: id=${post.id}, userId=${post.userId}, text=${post.text}")

            val currentUser = auth.currentUser
            if (currentUser != null) {
                // Check if the current user should be able to see this post based on visibility
                val canSeePost = when (post.visibility) {
                    PostVisibility.PUBLIC -> true // Anyone can see public posts
                    PostVisibility.FRIENDS_ONLY -> {
                        // Only friends of the post author or the author can see these posts
                        post.userId == currentUser.uid ||
                                getFriendUserIds(currentUser.uid).contains(post.userId)
                    }

                    PostVisibility.PRIVATE -> {
                        // Only the author can see their own private posts
                        post.userId == currentUser.uid
                    }
                }

                if (!canSeePost) {
                    Log.d(
                        "FirebaseRepository",
                        "User ${currentUser.uid} cannot access post $postId due to visibility restrictions"
                    )
                    return@withContext null
                }

                val sharedPostQuery = sharedPostsCollection
                    .whereEqualTo("sharedByUserId", currentUser.uid)
                    .whereEqualTo("originalPostId", postId)
                    .get()
                    .await()

                val isSharedByUser = !sharedPostQuery.isEmpty

                Log.d("FirebaseRepository", "Post $postId isSharedByCurrentUser=$isSharedByUser")

                // Check if post is archived by current user
                val archivedPostQuery = archivedPostsCollection
                    .whereEqualTo("userId", currentUser.uid)
                    .whereEqualTo("postId", postId)
                    .get()
                    .await()

                val isArchivedByUser = !archivedPostQuery.isEmpty

                Log.d(
                    "FirebaseRepository",
                    "Post $postId isArchivedByCurrentUser=$isArchivedByUser"
                )

                return@withContext post.copy(
                    isSharedByCurrentUser = isSharedByUser,
                    isArchivedByCurrentUser = isArchivedByUser
                )
            }
        } else {
            Log.e("FirebaseRepository", "Post was null for id: $postId")
        }

        return@withContext post
    }

    suspend fun toggleLike(postId: String): Boolean = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

        val likeQuery = likesCollection
            .whereEqualTo("postId", postId)
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .await()

        if (likeQuery.isEmpty) {
            val likeId = likesCollection.document().id
            val likeData = hashMapOf(
                "id" to likeId,
                "postId" to postId,
                "userId" to currentUser.uid,
                "createdAt" to Timestamp.now()
            )

            likesCollection.document(likeId).set(likeData).await()
            postsCollection.document(postId).update("likeCount", FieldValue.increment(1)).await()

            val post = getPostById(postId) ?: return@withContext true
            val user = getCurrentUser() ?: return@withContext true
            if (post.userId != user.id) {
                notificationRepository.createLikeNotification(post, user)
            } else {
                Log.d("FirebaseRepository", "Skipping self-notification for like")
            }

            true
        } else {
            val likeDoc = likeQuery.documents.first()
            likesCollection.document(likeDoc.id).delete().await()
            postsCollection.document(postId).update("likeCount", FieldValue.increment(-1)).await()
            false
        }
    }

    suspend fun toggleCommentLike(commentId: String): Boolean = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

        val likeQuery = commentLikesCollection
            .whereEqualTo("commentId", commentId)
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .await()

        if (likeQuery.isEmpty) {
            val likeId = commentLikesCollection.document().id
            val likeData = hashMapOf(
                "id" to likeId,
                "commentId" to commentId,
                "userId" to currentUser.uid,
                "userDisplayName" to (currentUser.displayName ?: "Unknown User"),
                "createdAt" to Timestamp.now()
            )

            commentLikesCollection.document(likeId).set(likeData).await()
            commentsCollection.document(commentId).update("likeCount", FieldValue.increment(1)).await()
            true
        } else {
            val likeDoc = likeQuery.documents.first()
            commentLikesCollection.document(likeDoc.id).delete().await()
            commentsCollection.document(commentId).update("likeCount", FieldValue.increment(-1)).await()
            false
        }
    }

    suspend fun addReply(parentCommentId: String, text: String, imageUri: Uri?): Comment = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")
        val userDisplayName = currentUser.displayName ?: "Unknown User"

        val parentComment = commentsCollection.document(parentCommentId).get().await()
            .toObject(Comment::class.java) ?: throw IllegalStateException("Parent comment not found")

        var imageUrl: String? = null
        if (imageUri != null) {
            try {
                imageUrl = uploadImage(imageUri, "comments")
            } catch (e: Exception) {
                Log.e("FirebaseRepository", "Failed to upload reply image: ${e.message}")
            }
        }

        val commentId = commentsCollection.document().id
        val reply = Comment(
            id = commentId,
            postId = parentComment.postId,
            userId = currentUser.uid,
            userDisplayName = userDisplayName,
            text = text,
            imageUrl = imageUrl,
            createdAt = Timestamp.now(),
            parentCommentId = parentCommentId
        )

        commentsCollection.document(commentId).set(reply).await()

        commentsCollection.document(parentCommentId).update("replyCount", FieldValue.increment(1)).await()

        reply
    }

    suspend fun addReplyWithImageUrl(
        parentCommentId: String,
        text: String,
        imageUrl: String?
    ): Comment = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")
        val userDisplayName = currentUser.displayName ?: "Unknown User"

        val parentComment = commentsCollection.document(parentCommentId).get().await()
            .toObject(Comment::class.java)
            ?: throw IllegalStateException("Parent comment not found")

        val commentId = commentsCollection.document().id
        val reply = Comment(
            id = commentId,
            postId = parentComment.postId,
            userId = currentUser.uid,
            userDisplayName = userDisplayName,
            text = text,
            imageUrl = imageUrl,
            createdAt = Timestamp.now(),
            parentCommentId = parentCommentId
        )

        commentsCollection.document(commentId).set(reply).await()

        commentsCollection.document(parentCommentId).update("replyCount", FieldValue.increment(1)).await()

        reply
    }

    suspend fun addReplyWithMediaUrl(
        parentCommentId: String,
        text: String,
        mediaUrl: String?,
        isVideo: Boolean = false
    ): Comment = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")
        val userDisplayName = currentUser.displayName ?: "Unknown User"

        val parentComment = commentsCollection.document(parentCommentId).get().await()
            .toObject(Comment::class.java)
            ?: throw IllegalStateException("Parent comment not found")

        val commentId = commentsCollection.document().id
        val reply = if (isVideo) {
            Comment(
                id = commentId,
                postId = parentComment.postId,
                userId = currentUser.uid,
                userDisplayName = userDisplayName,
                text = text,
                videoUrl = mediaUrl,
                hasVideo = true,
                createdAt = Timestamp.now(),
                parentCommentId = parentCommentId
            )
        } else {
            Comment(
                id = commentId,
                postId = parentComment.postId,
                userId = currentUser.uid,
                userDisplayName = userDisplayName,
                text = text,
                imageUrl = mediaUrl,
                createdAt = Timestamp.now(),
                parentCommentId = parentCommentId
            )
        }

        commentsCollection.document(commentId).set(reply).await()

        commentsCollection.document(parentCommentId).update("replyCount", FieldValue.increment(1)).await()

        reply
    }

    suspend fun getRepliesForComment(commentId: String): List<Comment> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser

            val replies = commentsCollection
                .whereEqualTo("parentCommentId", commentId)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .await()
                .toObjects(Comment::class.java)

            if (currentUser != null) {
                val userLikes = commentLikesCollection
                    .whereEqualTo("userId", currentUser.uid)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.getString("commentId") }
                    .toSet()

                return@withContext replies.map { reply ->
                    reply.copy(isLikedByCurrentUser = userLikes.contains(reply.id))
                }
            } else {
                return@withContext replies
            }
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }

    private suspend fun uploadImage(imageUri: Uri, folder: String): String = withContext(Dispatchers.IO) {
        try {
            Log.d("FirebaseRepository", "Using local URI as temporary solution: $imageUri")
            return@withContext imageUri.toString()
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error processing local URI", e)
            throw Exception("Failed to process image: ${e.message}")
        }
    }

    suspend fun getPostsSharedByUser(userId: String? = null): List<Post> =
        withContext(Dispatchers.IO) {
            try {
                val currentUser = auth.currentUser
                val targetUserId = userId ?: currentUser?.uid ?: return@withContext emptyList()

                Log.d("FirebaseRepository", "Getting posts shared by user: $targetUserId")

                val sharedPostDocs = sharedPostsCollection
                    .whereEqualTo("sharedByUserId", targetUserId)
                    .get()
                    .await()
                    .documents

                if (sharedPostDocs.isEmpty()) {
                    Log.d("FirebaseRepository", "No shared posts found for user: $targetUserId")
                    return@withContext emptyList()
                }

                Log.d("FirebaseRepository", "Found ${sharedPostDocs.size} shared post documents")

                val result = mutableListOf<Post>()
                val friendships =
                    if (currentUser != null) getFriendUserIds(currentUser.uid) else emptySet()

                for (sharedPostDoc in sharedPostDocs) {
                    val sharedPost = sharedPostDoc.toObject(SharedPost::class.java) ?: continue
                    val originalPostId = sharedPost.originalPostId

                    val originalPost = getPostById(originalPostId) ?: continue

                    // Apply visibility filtering based on who's viewing
                    val canSeePost = when {
                        // If viewing own shared posts, show all
                        currentUser?.uid == targetUserId -> true
                        // If shared post is marked private, only the sharer can see it
                        sharedPost.isPrivate -> false
                        // For original post visibility:
                        originalPost.visibility == PostVisibility.PUBLIC -> true
                        originalPost.visibility == PostVisibility.FRIENDS_ONLY ->
                            currentUser?.uid == originalPost.userId || friendships.contains(
                                originalPost.userId
                            )

                        originalPost.visibility == PostVisibility.PRIVATE ->
                            currentUser?.uid == originalPost.userId

                        else -> false
                    }

                    if (canSeePost) {
                        val combinedPost = originalPost.copy(
                            isSharedPost = true,
                            sharedByUserId = sharedPost.sharedByUserId,
                            sharedByUserName = sharedPost.sharedByUserName,
                            isSharedByCurrentUser = currentUser?.uid == sharedPost.sharedByUserId
                        )

                        result.add(combinedPost)
                    }
                }

                Log.d("FirebaseRepository", "Returning ${result.size} visible shared posts")
                return@withContext result
            } catch (e: Exception) {
                Log.e("FirebaseRepository", "Error getting posts shared by user: ${e.message}", e)
                return@withContext emptyList()
            }
        }

    suspend fun getNotificationsForCurrentUser(): Flow<List<Notification>> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser
                ?: return@withContext kotlinx.coroutines.flow.flow { emit(emptyList<Notification>()) }
            Log.d("FirebaseRepository", "Getting notifications for user: ${currentUser.uid}")
            return@withContext notificationRepository.getNotificationsForUser(currentUser.uid)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error getting notifications for current user", e)
            return@withContext kotlinx.coroutines.flow.flow { emit(emptyList<Notification>()) }
        }
    }

    suspend fun markNotificationAsRead(notificationId: String) = withContext(Dispatchers.IO) {
        notificationRepository.markNotificationAsRead(notificationId)
    }

    suspend fun markAllNotificationsAsRead() = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")
        notificationRepository.markAllNotificationsAsRead(currentUser.uid)
    }

    suspend fun saveSearchQuery(query: String) = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext

        try {
            Log.d("FirebaseRepository", "Saving search query: '$query' for user: ${currentUser.uid}")

            val searchHistoryId = searchHistoryCollection.document().id
            val searchHistory = hashMapOf(
                "id" to searchHistoryId,
                "userId" to currentUser.uid,
                "query" to query,
                "timestamp" to Timestamp.now()
            )

            searchHistoryCollection.document(searchHistoryId).set(searchHistory).await()
            Log.d("FirebaseRepository", "Search query saved successfully with ID: $searchHistoryId")
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error saving search query: ${e.message}", e)
        }
    }

    suspend fun getSearchHistory(limit: Int = 10): List<SearchHistory> =
        withContext(Dispatchers.IO) {
            val currentUser = auth.currentUser ?: return@withContext emptyList()

            try {
                Log.d("FirebaseRepository", "Getting search history for user: ${currentUser.uid}")

                val query = searchHistoryCollection
                    .whereEqualTo("userId", currentUser.uid)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(limit.toLong())

                Log.d("FirebaseRepository", "Executing query: ${query.toString()}")

                val snapshot = query.get().await()

                Log.d("FirebaseRepository", "Query returned ${snapshot.size()} documents")

                val result = snapshot.toObjects(SearchHistory::class.java)

                Log.d("FirebaseRepository", "Converted to ${result.size} SearchHistory objects")
                if (result.isNotEmpty()) {
                    Log.d("FirebaseRepository", "First item: query='${result.first().query}', id='${result.first().id}'")
                }

                return@withContext result
            } catch (e: Exception) {
                Log.e("FirebaseRepository", "Error getting search history: ${e.message}", e)
                return@withContext emptyList()
            }
        }

    suspend fun deleteSearchHistoryItem(historyId: String) = withContext(Dispatchers.IO) {
        try {
            searchHistoryCollection.document(historyId).delete().await()
            Log.d("FirebaseRepository", "Search history item deleted: $historyId")
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error deleting search history item: ${e.message}", e)
        }
    }

    suspend fun clearSearchHistory() = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext

        try {
            val batch = firestore.batch()
            val historyItems = searchHistoryCollection
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()

            for (item in historyItems) {
                batch.delete(item.reference)
            }

            batch.commit().await()
            Log.d("FirebaseRepository", "Search history cleared for user: ${currentUser.uid}")
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error clearing search history: ${e.message}", e)
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
            } else {
                Log.d("FirebaseRepository", "No self-notifications found")
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error checking self-notifications: ${e.message}")
        }
    }

    suspend fun debugFriendships() = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext

        try {
            Log.d("FirebaseRepository", "Current user ID: ${currentUser.uid}")

            val allFriendships = friendshipsCollection.get().await()
            Log.d("FirebaseRepository", "Total friendships in collection: ${allFriendships.size()}")

            val senderFriendships = friendshipsCollection
                .whereEqualTo("senderId", currentUser.uid)
                .get()
                .await()
            Log.d("FirebaseRepository", "Friendships where user is sender: ${senderFriendships.size()}")

            val receiverFriendships = friendshipsCollection
                .whereEqualTo("receiverId", currentUser.uid)
                .get()
                .await()
            Log.d("FirebaseRepository", "Friendships where user is receiver: ${receiverFriendships.size()}")

            val acceptedSenderFriendships = friendshipsCollection
                .whereEqualTo("senderId", currentUser.uid)
                .whereEqualTo("status", "ACCEPTED")
                .get()
                .await()
            Log.d("FirebaseRepository", "ACCEPTED friendships where user is sender: ${acceptedSenderFriendships.size()}")

            val acceptedReceiverFriendships = friendshipsCollection
                .whereEqualTo("receiverId", currentUser.uid)
                .whereEqualTo("status", "ACCEPTED")
                .get()
                .await()
            Log.d("FirebaseRepository", "ACCEPTED friendships where user is receiver: ${acceptedReceiverFriendships.size()}")

            val friendIds = mutableSetOf<String>()

            acceptedSenderFriendships.forEach { doc ->
                val receiverId = doc.getString("receiverId")
                if (receiverId != null) friendIds.add(receiverId)
                Log.d("FirebaseRepository", "Friend (receiver): $receiverId")
            }

            acceptedReceiverFriendships.forEach { doc ->
                val senderId = doc.getString("senderId")
                if (senderId != null) friendIds.add(senderId)
                Log.d("FirebaseRepository", "Friend (sender): $senderId")
            }

            Log.d("FirebaseRepository", "Total unique friends found: ${friendIds.size}")
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error debugging friendships: ${e.message}", e)
        }
    }

    suspend fun deletePost(postId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

            val post = getPostById(postId)

            if (post == null) {
                Log.e("FirebaseRepository", "Post not found: $postId")
                return@withContext false
            }

            if (post.userId != currentUser.uid) {
                Log.e("FirebaseRepository", "Not authorized to delete post: $postId")
                return@withContext false
            }

            postsCollection.document(postId).delete().await()

            val likesQuery = likesCollection.whereEqualTo("postId", postId).get().await()
            val batch = firestore.batch()
            likesQuery.documents.forEach { doc ->
                batch.delete(doc.reference)
            }

            val commentsQuery = commentsCollection.whereEqualTo("postId", postId).get().await()
            val commentIds = commentsQuery.documents.mapNotNull { it.id }

            commentsQuery.documents.forEach { doc ->
                batch.delete(doc.reference)
            }

            if (commentIds.isNotEmpty()) {
                val commentLikesQuery =
                    commentLikesCollection.whereIn("commentId", commentIds).get().await()
                commentLikesQuery.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
            }

            val sharesQuery =
                sharedPostsCollection.whereEqualTo("originalPostId", postId).get().await()
            sharesQuery.documents.forEach { doc ->
                batch.delete(doc.reference)
            }

            batch.commit().await()

            Log.d("FirebaseRepository", "Successfully deleted post: $postId")
            return@withContext true

        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error deleting post: ${e.message}", e)
            return@withContext false
        }
    }

    private val hiddenPostsCollection = firestore.collection("hiddenPosts")
    private val archivedPostsCollection = firestore.collection("archivedPosts")

    suspend fun hidePost(postId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

            val post = getPostById(postId)
            if (post == null) {
                Log.e("FirebaseRepository", "Post not found: $postId")
                return@withContext false
            }

            val hiddenPostId = hiddenPostsCollection.document().id
            val hiddenPost = hashMapOf(
                "id" to hiddenPostId,
                "userId" to currentUser.uid,
                "postId" to postId,
                "hiddenAt" to Timestamp.now()
            )

            hiddenPostsCollection.document(hiddenPostId).set(hiddenPost).await()

            Log.d("FirebaseRepository", "Successfully hid post: $postId for user: ${currentUser.uid}")
            return@withContext true

        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error hiding post: ${e.message}", e)
            return@withContext false
        }
    }

    suspend fun isPostHidden(postId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: return@withContext false

            val hiddenPostQuery = hiddenPostsCollection
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("postId", postId)
                .get()
                .await()

            return@withContext !hiddenPostQuery.isEmpty

        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error checking if post is hidden: ${e.message}", e)
            return@withContext false
        }
    }

    suspend fun getHiddenPosts(): List<Post> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: return@withContext emptyList<Post>()

            val hiddenPostsQuery = hiddenPostsCollection
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()

            val hiddenPostIds = hiddenPostsQuery.documents.mapNotNull { it.getString("postId") }

            if (hiddenPostIds.isEmpty()) {
                return@withContext emptyList()
            }

            // Retrieve the actual post data for each hidden post ID
            val posts = mutableListOf<Post>()
            for (postId in hiddenPostIds) {
                try {
                    val postDoc = postsCollection.document(postId).get().await()
                    val post = postDoc.toObject(Post::class.java)
                    if (post != null) {
                        posts.add(post)
                    }
                } catch (e: Exception) {
                    Log.e(
                        "FirebaseRepository",
                        "Error getting hidden post $postId: ${e.message}",
                        e
                    )
                }
            }

            return@withContext posts
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error getting hidden posts: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    suspend fun unhidePost(postId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

            val hiddenPostQuery = hiddenPostsCollection
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("postId", postId)
                .get()
                .await()

            if (hiddenPostQuery.isEmpty) {
                Log.d(
                    "FirebaseRepository",
                    "Post $postId is not hidden for user ${currentUser.uid}"
                )
                return@withContext false
            }

            // Delete the hidden post record
            val batch = firestore.batch()
            for (doc in hiddenPostQuery.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()

            Log.d(
                "FirebaseRepository",
                "Successfully unhid post: $postId for user: ${currentUser.uid}"
            )
            return@withContext true

        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error unhiding post: ${e.message}", e)
            return@withContext false
        }
    }

    suspend fun archivePost(postId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

            val post = getPostById(postId)
            if (post == null) {
                Log.e("FirebaseRepository", "Post not found: $postId")
                return@withContext false
            }

            // Any user can archive any post

            val archivedPostId = archivedPostsCollection.document().id
            val archivedPost = hashMapOf(
                "id" to archivedPostId,
                "userId" to currentUser.uid,
                "postId" to postId,
                "archivedAt" to Timestamp.now()
            )

            archivedPostsCollection.document(archivedPostId).set(archivedPost).await()

            Log.d(
                "FirebaseRepository",
                "Successfully archived post: $postId for user: ${currentUser.uid}"
            )
            return@withContext true

        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error archiving post: ${e.message}", e)
            return@withContext false
        }
    }

    suspend fun isPostArchived(postId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: return@withContext false

            val archivedPostQuery = archivedPostsCollection
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("postId", postId)
                .get()
                .await()

            return@withContext !archivedPostQuery.isEmpty

        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error checking if post is archived: ${e.message}", e)
            return@withContext false
        }
    }

    suspend fun getArchivedPosts(): List<Post> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: return@withContext emptyList<Post>()

            val archivedPostsQuery = archivedPostsCollection
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()

            val archivedPostIds = archivedPostsQuery.documents.mapNotNull { it.getString("postId") }

            if (archivedPostIds.isEmpty()) {
                return@withContext emptyList()
            }

            // Retrieve the actual post data for each archived post ID
            val posts = mutableListOf<Post>()
            for (postId in archivedPostIds) {
                try {
                    val postDoc = postsCollection.document(postId).get().await()
                    val post = postDoc.toObject(Post::class.java)
                    if (post != null) {
                        // Mark post as archived by current user
                        posts.add(post.copy(isArchivedByCurrentUser = true))
                    }
                } catch (e: Exception) {
                    Log.e(
                        "FirebaseRepository",
                        "Error getting archived post $postId: ${e.message}",
                        e
                    )
                }
            }

            return@withContext posts
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error getting archived posts: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    suspend fun unarchivePost(postId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

            val archivedPostQuery = archivedPostsCollection
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("postId", postId)
                .get()
                .await()

            if (archivedPostQuery.isEmpty) {
                Log.d(
                    "FirebaseRepository",
                    "Post $postId is not archived for user ${currentUser.uid}"
                )
                return@withContext false
            }

            // Delete the archived post record
            val batch = firestore.batch()
            for (doc in archivedPostQuery.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()

            Log.d(
                "FirebaseRepository",
                "Successfully unarchived post: $postId for user: ${currentUser.uid}"
            )
            return@withContext true

        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error unarchiving post: ${e.message}", e)
            return@withContext false
        }
    }

    suspend fun getFilteredPostsWithLikeStatus(): List<Post> = withContext(Dispatchers.IO) {
        val posts = getPostsWithLikeStatus()
        val currentUser = auth.currentUser ?: return@withContext posts

        try {
            val hiddenPostsQuery = hiddenPostsCollection
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()

            val hiddenPostIds =
                hiddenPostsQuery.documents.mapNotNull { it.getString("postId") }.toSet()

            return@withContext posts.filter { !hiddenPostIds.contains(it.id) }

        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error filtering hidden posts: ${e.message}", e)
            return@withContext posts
        }
    }

    suspend fun updatePost(
        postId: String,
        newText: String,
        newVisibility: PostVisibility
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

            // Get the post to verify ownership
            val post = getPostById(postId) ?: throw IllegalStateException("Post not found")

            // Only the post owner can update it
            if (post.userId != currentUser.uid) {
                Log.e("FirebaseRepository", "Not authorized to update post: $postId")
                return@withContext false
            }

            // Update the post
            val updateData = mapOf(
                "text" to newText,
                "visibility" to newVisibility,
                "isPrivate" to (newVisibility == PostVisibility.PRIVATE)
            )

            postsCollection.document(postId).update(updateData).await()

            Log.d("FirebaseRepository", "Successfully updated post: $postId")
            return@withContext true

        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error updating post: ${e.message}", e)
            return@withContext false
        }
    }

    suspend fun getFriendshipStatus(
        currentUserId: String,
        targetUserId: String
    ): FriendshipStatus? = withContext(Dispatchers.IO) {
        try {
            // Check if there's a friendship where current user is sender
            val senderQuery = friendshipsCollection
                .whereEqualTo("senderId", currentUserId)
                .whereEqualTo("receiverId", targetUserId)
                .get()
                .await()

            if (!senderQuery.isEmpty) {
                val status = senderQuery.documents.first().getString("status")
                return@withContext status?.let { FriendshipStatus.valueOf(it) }
            }

            // Check if there's a friendship where current user is receiver
            val receiverQuery = friendshipsCollection
                .whereEqualTo("senderId", targetUserId)
                .whereEqualTo("receiverId", currentUserId)
                .get()
                .await()

            if (!receiverQuery.isEmpty) {
                val status = receiverQuery.documents.first().getString("status")
                return@withContext status?.let { FriendshipStatus.valueOf(it) }
            }

            // No friendship exists
            return@withContext null
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error checking friendship status: ${e.message}", e)
            return@withContext null
        }
    }

    // Returns a Pair of (FriendshipStatus?, isRequestSender)
    suspend fun getFriendshipStatusWithRole(
        currentUserId: String,
        targetUserId: String
    ): Pair<FriendshipStatus?, Boolean> = withContext(Dispatchers.IO) {
        try {
            // Check if there's a friendship where current user is sender
            val senderQuery = friendshipsCollection
                .whereEqualTo("senderId", currentUserId)
                .whereEqualTo("receiverId", targetUserId)
                .get()
                .await()

            if (!senderQuery.isEmpty) {
                val status = senderQuery.documents.first().getString("status")
                return@withContext Pair(status?.let { FriendshipStatus.valueOf(it) }, true)
            }

            // Check if there's a friendship where current user is receiver
            val receiverQuery = friendshipsCollection
                .whereEqualTo("senderId", targetUserId)
                .whereEqualTo("receiverId", currentUserId)
                .get()
                .await()

            if (!receiverQuery.isEmpty) {
                val status = receiverQuery.documents.first().getString("status")
                return@withContext Pair(status?.let { FriendshipStatus.valueOf(it) }, false)
            }

            // No friendship exists
            return@withContext Pair(null, false)
        } catch (e: Exception) {
            Log.e(
                "FirebaseRepository",
                "Error checking friendship status with role: ${e.message}",
                e
            )
            return@withContext Pair(null, false)
        }
    }

    suspend fun sendFriendRequest(targetUserId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")
            val currentUserProfile =
                getCurrentUser() ?: throw IllegalStateException("Current user profile not found")
            val targetUserProfile =
                getUserById(targetUserId) ?: throw IllegalStateException("Target user not found")

            // Check if friendship already exists
            val existingStatus = getFriendshipStatus(currentUser.uid, targetUserId)
            if (existingStatus != null) {
                // Friendship already exists in some form
                Log.d(
                    "FirebaseRepository",
                    "Friendship already exists with status: $existingStatus"
                )
                return@withContext false
            }

            // Create new friendship request
            val friendshipId = friendshipsCollection.document().id
            val friendship = Friendship(
                id = friendshipId,
                senderId = currentUser.uid,
                senderName = currentUserProfile.displayName,
                senderPhotoUrl = currentUserProfile.photoUrl,
                receiverId = targetUserId,
                receiverName = targetUserProfile.displayName,
                receiverPhotoUrl = targetUserProfile.photoUrl,
                status = FriendshipStatus.PENDING,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )

            friendshipsCollection.document(friendshipId).set(friendship).await()

            Log.d("FirebaseRepository", "Friend request sent: ${currentUser.uid} -> $targetUserId")
            return@withContext true
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error sending friend request: ${e.message}", e)
            return@withContext false
        }
    }

    suspend fun removeFriend(targetUserId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")

            // Find friendship where current user is sender
            val senderQuery = friendshipsCollection
                .whereEqualTo("senderId", currentUser.uid)
                .whereEqualTo("receiverId", targetUserId)
                .get()
                .await()

            if (!senderQuery.isEmpty) {
                val friendshipDoc = senderQuery.documents.first()
                friendshipsCollection.document(friendshipDoc.id).delete().await()

                // Update friend counts
                updateFriendCount(currentUser.uid, -1)
                updateFriendCount(targetUserId, -1)

                Log.d(
                    "FirebaseRepository",
                    "Removed friendship (as sender): ${currentUser.uid} -> $targetUserId"
                )
                return@withContext true
            }

            // Find friendship where current user is receiver
            val receiverQuery = friendshipsCollection
                .whereEqualTo("senderId", targetUserId)
                .whereEqualTo("receiverId", currentUser.uid)
                .get()
                .await()

            if (!receiverQuery.isEmpty) {
                val friendshipDoc = receiverQuery.documents.first()
                friendshipsCollection.document(friendshipDoc.id).delete().await()

                // Update friend counts
                updateFriendCount(currentUser.uid, -1)
                updateFriendCount(targetUserId, -1)

                Log.d(
                    "FirebaseRepository",
                    "Removed friendship (as receiver): $targetUserId -> ${currentUser.uid}"
                )
                return@withContext true
            }

            Log.d(
                "FirebaseRepository",
                "No friendship found to remove between ${currentUser.uid} and $targetUserId"
            )
            return@withContext false
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error removing friend: ${e.message}", e)
            return@withContext false
        }
    }

    private suspend fun updateFriendCount(userId: String, increment: Long) =
        withContext(Dispatchers.IO) {
            try {
                usersCollection.document(userId)
                    .update("friendCount", FieldValue.increment(increment)).await()
            } catch (e: Exception) {
                Log.e(
                    "FirebaseRepository",
                    "Error updating friend count for user $userId: ${e.message}",
                    e
                )
            }
        }
}