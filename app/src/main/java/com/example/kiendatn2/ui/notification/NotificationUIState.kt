package com.example.kiendatn2.ui.notification

data class NotificationUIState(
    val notificationsState: NotificationState = NotificationState.Loading,
    val currentFilter: NotificationDateFilter = NotificationDateFilter.RECENT
)