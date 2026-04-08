package com.pranav.punecityguide.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranav.punecityguide.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppSession(
    val mode: String,
    val displayName: String,
    val email: String?,
    val accessToken: String?,
)

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    val sessionEvent: AppSession? = null,
)

class AuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Email and password are required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, info = null) }
            AuthRepository.signIn(normalizedEmail, password).onSuccess { auth ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        sessionEvent = AppSession(
                            mode = "auth",
                            displayName = auth.displayName,
                            email = auth.email,
                            accessToken = auth.accessToken,
                        ),
                    )
                }
            }.onFailure { error ->
                val userMessage = when {
                    error.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
                    error.message?.contains("Network", ignoreCase = true) == true ->
                        "No internet connection. Please check your network."
                    error.message?.contains("Invalid login credentials", ignoreCase = true) == true ->
                        "Incorrect email or password. Please try again."
                    error.message?.contains("Email not confirmed", ignoreCase = true) == true ->
                        "Please verify your email address first."
                    error.message?.contains("timeout", ignoreCase = true) == true ->
                        "Connection timed out. Please try again."
                    else -> "Unable to sign in. Please try again."
                }
                _uiState.update { it.copy(isLoading = false, error = userMessage) }
            }
        }
    }

    fun signUp(email: String, password: String, confirmPassword: String, fullName: String) {
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Email and password are required") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(error = "Password must be at least 6 characters") }
            return
        }
        if (password != confirmPassword) {
            _uiState.update { it.copy(error = "Passwords do not match") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, info = null) }
            AuthRepository.signUp(normalizedEmail, password, fullName.trim()).onSuccess { auth ->
                val displayName = fullName.trim().ifBlank { auth.displayName }
                if (auth.requiresEmailVerification) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            info = "Sign up successful. Verify your email, then login.",
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            sessionEvent = AppSession(
                                mode = "auth",
                                displayName = displayName,
                                email = auth.email,
                                accessToken = auth.accessToken,
                            ),
                        )
                    }
                }
            }.onFailure { error ->
                val userMessage = when {
                    error.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
                    error.message?.contains("Network", ignoreCase = true) == true ->
                        "No internet connection. Please check your network."
                    error.message?.contains("already registered", ignoreCase = true) == true ||
                    error.message?.contains("User already exists", ignoreCase = true) == true ->
                        "An account with this email already exists. Try signing in."
                    error.message?.contains("timeout", ignoreCase = true) == true ->
                        "Connection timed out. Please try again."
                    error.message?.contains("invalid", ignoreCase = true) == true ->
                        "Please enter a valid email address."
                    else -> "Unable to create account. Please try again."
                }
                _uiState.update { it.copy(isLoading = false, error = userMessage) }
            }
        }
    }

    fun continueAsGuest() {
        _uiState.update {
            it.copy(
                sessionEvent = AppSession(
                    mode = "guest",
                    displayName = "Guest",
                    email = null,
                    accessToken = null,
                ),
                error = null,
                info = null,
            )
        }
    }

    fun consumeSessionEvent() {
        _uiState.update { it.copy(sessionEvent = null) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, info = null) }
    }
}
