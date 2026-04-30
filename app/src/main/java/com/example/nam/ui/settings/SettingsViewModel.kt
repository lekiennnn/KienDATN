package com.example.nam.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SettingsUIState(
    val isDarkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val passwordChangeState: PasswordChangeState = PasswordChangeState.Initial
)

sealed class PasswordChangeState {
    object Initial : PasswordChangeState()
    object Loading : PasswordChangeState()
    object Success : PasswordChangeState()
    data class Error(val message: String) : PasswordChangeState()
}

class SettingsViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val _uiState = MutableStateFlow(SettingsUIState())
    val uiState: StateFlow<SettingsUIState> = _uiState.asStateFlow()

    fun toggleDarkMode() {
        _uiState.update { it.copy(isDarkMode = !it.isDarkMode) }
    }

    fun toggleNotifications() {
        _uiState.update { it.copy(notificationsEnabled = !it.notificationsEnabled) }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(passwordChangeState = PasswordChangeState.Loading) }

                val user = auth.currentUser ?: throw Exception("User not logged in")
                val email = user.email ?: throw Exception("User email not found")

                // Re-authenticate the user first
                val credential = EmailAuthProvider.getCredential(email, currentPassword)
                user.reauthenticate(credential).await()

                // Change the password
                user.updatePassword(newPassword).await()

                _uiState.update { it.copy(passwordChangeState = PasswordChangeState.Success) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        passwordChangeState = PasswordChangeState.Error(
                            e.message ?: "Failed to change password"
                        )
                    )
                }
            }
        }
    }

    fun resetPasswordChangeState() {
        _uiState.update { it.copy(passwordChangeState = PasswordChangeState.Initial) }
    }
}