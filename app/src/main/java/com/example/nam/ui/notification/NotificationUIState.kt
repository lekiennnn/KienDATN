package com.example.nam.ui.notification

data class NotificationUIState(
    val notificationsState: NotificationState = NotificationState.Loading,
    val currentFilter: NotificationDateFilter = NotificationDateFilter.RECENT
)