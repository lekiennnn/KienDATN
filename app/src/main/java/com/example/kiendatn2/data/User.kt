package com.example.kiendatn2.data

import com.google.firebase.Timestamp

data class User(
    val id: String = "",
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val bio: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val friendCount: Int = 0,
    @field:Transient
    val isFollowedByCurrentUser: Boolean = false,
    @field:Transient
    val isLoading: Boolean = false
)