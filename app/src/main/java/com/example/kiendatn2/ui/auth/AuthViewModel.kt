package com.example.kiendatn2.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kiendatn2.repository.FirebaseRepository
import com.example.kiendatn2.utils.SharedPreferenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val repository = FirebaseRepository()
    private val sharedPreferenceManager = SharedPreferenceManager.getInstance(application)

    private val _uiState = MutableStateFlow(AuthUIState())
    val uiState: StateFlow<AuthUIState> = _uiState.asStateFlow()

    init {
        checkAuthStatus()
    }

    fun checkAuthStatus() {
        if (auth.currentUser == null) {
            _uiState.update { it.copy(authState = AuthState.Unauthenticated) }
            sharedPreferenceManager.setLoggedIn(false)
            sharedPreferenceManager.setUserId(null)
        } else {
            _uiState.update { it.copy(authState = AuthState.Authenticated) }
            sharedPreferenceManager.setLoggedIn(true)
            sharedPreferenceManager.setUserId(auth.currentUser?.uid)
        }
    }

    fun login(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            _uiState.update { it.copy(authState = AuthState.Error("Email or password can't be empty")) }
            return
        }

        _uiState.update { it.copy(authState = AuthState.Loading) }
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _uiState.update { it.copy(authState = AuthState.Authenticated) }
                    sharedPreferenceManager.setLoggedIn(true)
                    sharedPreferenceManager.setUserId(auth.currentUser?.uid)
                } else {
                    _uiState.update {
                        it.copy(
                            authState = AuthState.Error(
                                task.exception?.message ?: "Something is wrong"
                            )
                        )
                    }
                    sharedPreferenceManager.setLoggedIn(false)
                    sharedPreferenceManager.setUserId(null)
                }
            }
    }

    fun signup(email: String, password: String, displayName: String) {
        if (email.isEmpty() || password.isEmpty() || displayName.isEmpty()) {
            _uiState.update {
                it.copy(authState = AuthState.Error("Email, password or display name can't be empty"))
            }
            return
        }

        _uiState.update { it.copy(authState = AuthState.Loading) }
        viewModelScope.launch {
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password)
                    .await()

                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()

                authResult.user?.updateProfile(profileUpdates)?.await()

                // Save user to Firestore (optional but recommended)
                val user = hashMapOf(
                    "uid" to authResult.user?.uid,
                    "email" to email,
                    "displayName" to displayName,
                    "createdAt" to FieldValue.serverTimestamp()
                )

                FirebaseFirestore.getInstance().collection("users")
                    .document(authResult.user?.uid!!)
                    .set(user)
                    .await()

                _uiState.update { it.copy(authState = AuthState.Authenticated) }
                sharedPreferenceManager.setLoggedIn(true)
                sharedPreferenceManager.setUserId(authResult.user?.uid)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        authState = AuthState.Error(
                            e.message ?: "Signup failed"
                        )
                    )
                }
                sharedPreferenceManager.setLoggedIn(false)
                sharedPreferenceManager.setUserId(null)
            }
        }
    }

    fun signout(){
        auth.signOut()
        _uiState.update { it.copy(authState = AuthState.Unauthenticated) }
        sharedPreferenceManager.setLoggedIn(false)
        sharedPreferenceManager.clearUserSession()
    }
}

sealed class AuthState {
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Error(val message: String) : AuthState()
}