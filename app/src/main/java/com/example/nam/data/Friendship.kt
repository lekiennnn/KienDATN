package com.example.nam.data

import com.google.firebase.Timestamp

enum class FriendshipStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}

data class Friendship(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderPhotoUrl: String? = null,
    val receiverId: String = "",
    val receiverName: String = "",
    val receiverPhotoUrl: String? = null,
    val status: FriendshipStatus = FriendshipStatus.PENDING,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)