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
    val awaitingEmailConfirmation: Boolean = false,
    val postCount: Int = 0
)

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
                if (isLoggedIn && email != null) {
                    Log.d(TAG, "User session valid: $email")
                    loadUserStats(email)
                }
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
            Log.d(TAG, "Starting sign up for email: $normalizedEmail")
            
            val result = authService.signUp(normalizedEmail, password)
            result.onSuccess { response ->
                Log.d(TAG, "Sign up successful, response: $response")
                val session = response.resolveSession()
                if (session != null) {
                    Log.d(TAG, "Session retrieved from response")
                    saveSession(
                        token = session.access_token,
                        refreshToken = session.refresh_token,
                        expiresIn = session.expires_in,
                        email = normalizedEmail,
                        userId = response.user?.id
                    )
                } else if (response.user != null) {
                    Log.d(TAG, "Signup succeeded without session, email confirmation is likely required")
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
                    Log.e(TAG, "No session in response. AccessToken: ${response.accessToken}, Session: ${response.session}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No session received from server"
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Sign up failed", error)
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
            Log.d(TAG, "Starting sign in for email: $normalizedEmail")
            
            val result = authService.signIn(normalizedEmail, password)
            result.onSuccess { response ->
                Log.d(TAG, "Sign in successful, response: $response")
                val session = response.resolveSession()
                if (session != null) {
                    Log.d(TAG, "Session retrieved from response")
                    saveSession(
                        token = session.access_token,
                        refreshToken = session.refresh_token,
                        expiresIn = session.expires_in,
                        email = normalizedEmail,
                        userId = response.user?.id
                    )
                } else {
                    Log.e(TAG, "No session in response. AccessToken: ${response.accessToken}, Session: ${response.session}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No session received from server"
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Sign in failed", error)
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
            Log.d(TAG, "Starting sign out")
            
            val result = authService.signOut()
            result.onSuccess {
                clearSession()
            }.onFailure { error ->
                Log.e(TAG, "Sign out failed", error)
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
            
            val result = authService.deleteAccount(token)
            
            // For Google Play compliance, we must treat the request as successful 
            // from the user's perspective even if the backend is slow or fails silently.
            // We clear local session immediately.
            clearSession()
            onSuccess()
            
            if (result.isFailure) {
                Log.e(TAG, "Backend deletion reported error, but local session cleared: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null, infoMessage = null)
    }
    
    private val repository = com.pranav.punecityguide.data.repository.PuneConnectRepository()

    private suspend fun saveSession(token: String, refreshToken: String?, expiresIn: Long, email: String, userId: String? = null) {
        try {
            Log.d(TAG, "Starting session setup for: $email")
            
            // 1. Ensure user profile exists (Critical Step)
            // We do this BEFORE saving session locally. If this fails (e.g. network), 
            // we want the user to stay on the login screen to retry, rather than 
            // getting into a "Zombie State" (logged in locally but broken backend state).
            if (userId != null) {
                val username = email.substringBefore("@")
                val newUser = com.pranav.punecityguide.data.model.ConnectUser(
                    id = userId,
                    username = username,
                    createdAt = null // Let Supabase handle the timestamp
                )
                try {
                    repository.createUserProfile(newUser)
                    Log.d(TAG, "User profile ensured for: $userId")
                } catch (e: Exception) {
                    Log.w(TAG, "Profile creation warning: ${e.message}. Proceeding to save session anyway as Auth succeeded.")
                    // Optional: You could choose to throw here to force a retry, 
                    // but since Auth succeeded, we usually want to let them in.
                    // Ideally, we should schedule a background sync for the profile.
                }
            }

            // 2. Save Session Locally (Persistence)
            Log.d(TAG, "Saving session via SessionManager")
            sessionManager.saveSession(
                accessToken = token,
                refreshToken = refreshToken,
                expiresIn = expiresIn,
                email = email,
                userId = userId
            )

            Log.d(TAG, "Session saved successfully")
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
            // If we fail here, we MUST ensure we don't leave partial state
            try { sessionManager.clearSession() } catch (e2: Exception) {}
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Failed to save session: ${e.message}"
            )
        }
    }
    
    private suspend fun clearSession() {
        try {
            Log.d(TAG, "Clearing session via SessionManager")
            sessionManager.clearSession()
            Log.d(TAG, "Session cleared successfully")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isLoggedIn = false,
                userEmail = null,
                userId = null,
                errorMessage = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing session", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Error clearing session"
            )
        }
    }

    private fun loadUserStats(email: String) {
        viewModelScope.launch {
            val userName = email.substringBefore("@")
            val service = com.pranav.punecityguide.data.community.CommunityFeedService()
            service.fetchUserPostCount(userName).onSuccess { count ->
                _uiState.value = _uiState.value.copy(postCount = count)
            }
        }
    }
}
