package com.pranav.punecityguide.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pranav.punecityguide.data.service.AuthService
import com.pranav.punecityguide.data.service.SupabaseClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val userEmail: String? = null,
    val userId: String? = null,
    val awaitingEmailConfirmation: Boolean = false
) {
    val displayName: String
        get() = if (isLoggedIn) (userEmail?.substringBefore("@") ?: "Explorer") else "Guest Explorer"
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val authService = AuthService()
    private val sessionManager = SupabaseClient.getSessionManager()
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState
    
    private val TAG = "AuthViewModel"
    private var signupCooldownUntilMs: Long = 0L
    
    init {
        checkAuthStatus()
    }
    
    private fun checkAuthStatus() {
        viewModelScope.launch {
            try {
                val token = sessionManager.getAccessToken()
                val email = sessionManager.userEmail.first()
                val uid = sessionManager.getUserId()
                val isValid = sessionManager.isSessionValid()
                val isLoggedIn = !token.isNullOrEmpty() && isValid

                _uiState.value = _uiState.value.copy(
                    isLoggedIn = isLoggedIn,
                    userEmail = email,
                    userId = uid
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error checking auth status", e)
            }
        }
    }
    
    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (now < signupCooldownUntilMs) {
                val secondsLeft = ((signupCooldownUntilMs - now) / 1000L).toInt().coerceAtLeast(1)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Email rate limit exceeded. Try again in ${secondsLeft}s."
                )
                return@launch
            }

            val normalizedEmail = email.trim().lowercase()
            if (normalizedEmail.isEmpty()) {
                _uiState.value = _uiState.value.copy(errorMessage = "Please enter your email")
                return@launch
            }
            if (password.length < 6) {
                _uiState.value = _uiState.value.copy(errorMessage = "Password must be at least 6 characters")
                return@launch
            }
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                infoMessage = null,
                awaitingEmailConfirmation = false
            )
            
            val result = authService.signUp(normalizedEmail, password)
            result.onSuccess { response ->
                val session = response.resolveSession()
                if (session != null) {
                    saveSession(
                        token = session.access_token,
                        refreshToken = session.refresh_token,
                        expiresIn = session.expires_in,
                        email = normalizedEmail,
                        userId = response.user?.id
                    )
                } else if (response.user != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = false,
                        userEmail = normalizedEmail,
                        userId = response.user?.id,
                        errorMessage = null,
                        infoMessage = "Account created. Please verify your email, then sign in.",
                        awaitingEmailConfirmation = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No session received from server"
                    )
                }
            }.onFailure { error ->
                val raw = error.message ?: "Sign up failed"
                if (raw.lowercase().contains("rate limit")) {
                    signupCooldownUntilMs = System.currentTimeMillis() + 60_000L
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = raw
                )
            }
        }
    }
    
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            val normalizedEmail = email.trim().lowercase()
            if (normalizedEmail.isEmpty()) {
                _uiState.value = _uiState.value.copy(errorMessage = "Please enter your email")
                return@launch
            }
            if (password.isEmpty()) {
                _uiState.value = _uiState.value.copy(errorMessage = "Please enter your password")
                return@launch
            }
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                infoMessage = null,
                awaitingEmailConfirmation = false
            )
            
            val result = authService.signIn(normalizedEmail, password)
            result.onSuccess { response ->
                val session = response.resolveSession()
                if (session != null) {
                    saveSession(
                        token = session.access_token,
                        refreshToken = session.refresh_token,
                        expiresIn = session.expires_in,
                        email = normalizedEmail,
                        userId = response.user?.id
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No session received from server"
                    )
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Sign in failed"
                )
            }
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = authService.signOut()
            result.onSuccess {
                clearSession()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Sign out failed"
                )
            }
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val token = sessionManager.getAccessToken() ?: ""
            authService.deleteAccount(token)
            clearSession()
            onSuccess()
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null, infoMessage = null)
    }

    private suspend fun saveSession(token: String, refreshToken: String?, expiresIn: Long, email: String, userId: String? = null) {
        try {
            sessionManager.saveSession(
                accessToken = token,
                refreshToken = refreshToken,
                expiresIn = expiresIn,
                email = email,
                userId = userId
            )

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isLoggedIn = true,
                userEmail = email,
                userId = userId,
                errorMessage = null,
                infoMessage = null,
                awaitingEmailConfirmation = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error saving session", e)
            try { sessionManager.clearSession() } catch (e2: Exception) {}
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Failed to save session: ${e.message}"
            )
        }
    }
    
    private suspend fun clearSession() {
        try {
            sessionManager.clearSession()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isLoggedIn = false,
                userEmail = null,
                userId = null,
                errorMessage = null
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Error clearing session"
            )
        }
    }
}
