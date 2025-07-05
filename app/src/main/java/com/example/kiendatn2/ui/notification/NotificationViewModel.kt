package com.example.kiendatn2.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kiendatn2.data.Notification
import com.example.kiendatn2.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.util.Log
import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.Date
import com.google.firebase.auth.FirebaseAuth

enum class NotificationDateFilter {
    RECENT, // Last 5 days
    ALL,    // All notifications
    WEEK,   // Last week
    MONTH   // Last month
}

class NotificationViewModel : ViewModel() {
    private val repository = FirebaseRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(NotificationUIState())
    val uiState: StateFlow<NotificationUIState> = _uiState

    private var allNotifications: List<Notification> = emptyList()
    private var currentFilter = NotificationDateFilter.RECENT

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        if (firebaseAuth.currentUser != null) {
            // User is signed in, reload notifications
            loadNotifications()
        } else {
            // User is signed out, clear notifications
            allNotifications = emptyList()
            _uiState.value = _uiState.value.copy(
                notificationsState = NotificationState.Success(emptyList()),
                currentFilter = currentFilter
            )
        }
    }

    init {
        loadNotifications()
        viewModelScope.launch {
            try {
                repository.checkSelfNotifications()
            } catch (e: Exception) {
            }
        }

        // Register auth state listener
        auth.addAuthStateListener(authStateListener)
    }

    override fun onCleared() {
        // Remove auth state listener when ViewModel is destroyed
        auth.removeAuthStateListener(authStateListener)
        super.onCleared()
    }

    fun setDateFilter(filter: NotificationDateFilter) {
        if (filter != currentFilter) {
            currentFilter = filter
            applyFilter()
        }
    }

    private fun applyFilter() {
        val currentState = _uiState.value.notificationsState
        if (currentState is NotificationState.Success) {
            val filtered = filterNotifications(allNotifications, currentFilter)
            _uiState.value = _uiState.value.copy(
                notificationsState = NotificationState.Success(filtered),
                currentFilter = currentFilter
            )
        }
    }

    private fun filterNotifications(
        notifications: List<Notification>,
        filter: NotificationDateFilter
    ): List<Notification> {
        val calendar = Calendar.getInstance()
        val currentTime = calendar.timeInMillis

        return when (filter) {
            NotificationDateFilter.RECENT -> {
                // Last 5 days
                calendar.add(Calendar.DAY_OF_YEAR, -5)
                val fiveDaysAgo = calendar.time
                notifications.filter { it.timestamp.toDate().after(fiveDaysAgo) }
            }

            NotificationDateFilter.WEEK -> {
                // Last week
                calendar.add(Calendar.WEEK_OF_YEAR, -1)
                val oneWeekAgo = calendar.time
                notifications.filter { it.timestamp.toDate().after(oneWeekAgo) }
            }

            NotificationDateFilter.MONTH -> {
                // Last month
                calendar.add(Calendar.MONTH, -1)
                val oneMonthAgo = calendar.time
                notifications.filter { it.timestamp.toDate().after(oneMonthAgo) }
            }

            NotificationDateFilter.ALL -> {
                // All notifications
                notifications
            }
        }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(notificationsState = NotificationState.Loading)
            try {
                repository.getNotificationsForCurrentUser()
                    .catch { e ->
                        Log.e("NotificationViewModel", "Error in notifications flow", e)
                        _uiState.value = _uiState.value.copy(
                            notificationsState = NotificationState.Error(
                                e.message ?: "Failed to load notifications"
                            )
                        )
                    }
                    .collectLatest { notifications ->
                        Log.d(
                            "NotificationViewModel",
                            "Collected ${notifications.size} notifications"
                        )
                        allNotifications = notifications
                        val filtered = filterNotifications(notifications, currentFilter)
                        _uiState.value = _uiState.value.copy(
                            notificationsState = NotificationState.Success(filtered),
                            currentFilter = currentFilter
                        )
                    }
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "Exception getting notifications", e)
                _uiState.value = _uiState.value.copy(
                    notificationsState = NotificationState.Error(
                        e.message ?: "Failed to load notifications"
                    )
                )
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                repository.markNotificationAsRead(notificationId)

                val currentState = _uiState.value.notificationsState
                if (currentState is NotificationState.Success) {
                    val updatedAllNotifications = allNotifications.map { notification ->
                        if (notification.id == notificationId) {
                            notification.copy(isRead = true)
                        } else {
                            notification
                        }
                    }
                    allNotifications = updatedAllNotifications

                    val updatedFilteredNotifications =
                        filterNotifications(updatedAllNotifications, currentFilter)
                    _uiState.value = _uiState.value.copy(
                        notificationsState = NotificationState.Success(updatedFilteredNotifications)
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    notificationsState = NotificationState.Error(
                        e.message ?: "Failed to mark notification as read"
                    )
                )
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                repository.markAllNotificationsAsRead()

                val updatedAllNotifications = allNotifications.map { notification ->
                    notification.copy(isRead = true)
                }
                allNotifications = updatedAllNotifications

                val updatedFilteredNotifications =
                    filterNotifications(updatedAllNotifications, currentFilter)
                _uiState.value = _uiState.value.copy(
                    notificationsState = NotificationState.Success(updatedFilteredNotifications)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    notificationsState = NotificationState.Error(
                        e.message ?: "Failed to mark all notifications as read"
                    )
                )
            }
        }
    }
}

sealed class NotificationState {
    object Loading : NotificationState()
    data class Success(val notifications: List<Notification>) : NotificationState()
    data class Error(val message: String) : NotificationState()
}