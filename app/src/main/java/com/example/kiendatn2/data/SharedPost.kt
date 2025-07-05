package com.example.kiendatn2.data

import com.google.firebase.Timestamp

data class SharedPost(
    val id: String = "",
    val originalPostId: String = "",
    val sharedByUserId: String = "",
    val sharedByUserName: String = "",
    val sharedByUserProfilePicture: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val isPrivate: Boolean = false
)