package com.pranav.punecityguide.data.service

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

/**
 * Production-grade session management with DataStore persistence.
 *
 * Features:
 * - Persistent token storage via Jetpack DataStore
 * - Proactive token refresh before expiry (5-minute buffer window)
 * - Thread-safe via Kotlin Mutex (non-blocking for coroutines)
 * - Automatic refresh token rotation support
 * - Session expiry validation with configurable buffer
 */
class TokenSessionManager(private val context: Context) {
    
    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_EXPIRES_AT = longPreferencesKey("expires_at")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_SESSION_ID = stringPreferencesKey("session_id") // For audit correlation
        private const val TAG = "TokenSessionManager"
        private const val REFRESH_BUFFER_MS = 5 * 60 * 1000L // Refresh 5 minutes before expiry
    }

    private val refreshMutex = Mutex()
    private var lastRefreshAttemptMs = 0L
    private var cachedAccessToken: String? = null
    private var cachedExpiresAt: Long = 0L

    val accessToken: Flow<String?> = context.dataStore.data.map { it[KEY_ACCESS_TOKEN] }
    val userEmail: Flow<String?> = context.dataStore.data.map { it[KEY_USER_EMAIL] }
    val userIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_ID] ?: prefs[KEY_ACCESS_TOKEN]?.let { token ->
            try {
                val parts = token.split(".")
                if (parts.size == 3) {
                    val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
                    org.json.JSONObject(payload).optString("sub", null)
                } else null
            } catch (e: Exception) { null }
        }
    }

    /**
     * Save a new auth session with all tokens.
     * Generates a unique session ID for audit trail correlation.
     */
    suspend fun saveSession(accessToken: String, refreshToken: String?, expiresIn: Long, email: String?, userId: String? = null) {
        val expiresAt = System.currentTimeMillis() + (expiresIn * 1000)
        val sessionId = java.util.UUID.randomUUID().toString().take(8)
        
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            refreshToken?.let { prefs[KEY_REFRESH_TOKEN] = it }
            prefs[KEY_EXPIRES_AT] = expiresAt
            email?.let { prefs[KEY_USER_EMAIL] = it }
            userId?.let { prefs[KEY_USER_ID] = it }
            prefs[KEY_SESSION_ID] = sessionId
        }

        // Update local cache
        cachedAccessToken = accessToken
        cachedExpiresAt = expiresAt
        com.pranav.punecityguide.data.service.SupabaseClient.invalidateAuthCache()

        Log.d(TAG, "Session saved [session=$sessionId, email=$email, expiresIn=${expiresIn}s]")
    }

    /**
     * Securely clear all session data on sign-out.
     */
    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
        cachedAccessToken = null
        cachedExpiresAt = 0L
        com.pranav.punecityguide.data.service.SupabaseClient.invalidateAuthCache()
        Log.d(TAG, "Session cleared")
    }

    /**
     * Get an access token, proactively refreshing if it's about to expire.
     *
     * Returns null if no session exists.
     * Uses Mutex to prevent concurrent refresh storms.
     */
    suspend fun getAccessToken(): String? {
        val now = System.currentTimeMillis()
        
        // 1. Fast path: use cache if valid with buffer
        cachedAccessToken?.let { token ->
            if (now < (cachedExpiresAt - REFRESH_BUFFER_MS)) {
                return token
            }
        }

        // 2. Lock to prevent simultaneous refreshes
        return refreshMutex.withLock {
            // Check cache again after acquiring lock
            cachedAccessToken?.let { token ->
                if (now < (cachedExpiresAt - REFRESH_BUFFER_MS)) {
                    return@withLock token
                }
            }

            // Sync with DataStore
            val prefs = context.dataStore.data.firstOrNull()
            val token = prefs?.get(KEY_ACCESS_TOKEN)
            val expiry = prefs?.get(KEY_EXPIRES_AT) ?: 0L
            val refreshToken = prefs?.get(KEY_REFRESH_TOKEN)

            if (token == null) return@withLock null

            // 3. If near or past expiry, perform refresh
            if (now >= (expiry - REFRESH_BUFFER_MS) && !refreshToken.isNullOrBlank()) {
                Log.d(TAG, "Token expiring soon. Attempting sync refresh...")
                try {
                    val authService = AuthService()
                    val result = authService.refreshSession(refreshToken)
                    result.onSuccess { response ->
                        val session = response.resolveSession()
                        if (session != null) {
                            saveSession(
                                accessToken = session.access_token,
                                refreshToken = session.refresh_token,
                                expiresIn = session.expires_in,
                                email = prefs[KEY_USER_EMAIL]
                            )
                            Log.i(TAG, "Refresh session successful")
                            return@withLock session.access_token
                        }
                    }.onFailure { e ->
                        Log.e(TAG, "Refresh session failed: ${e.message}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Refresh exception", e)
                }
            }

            // Return current token as fallback, or new token if set by saveSession
            cachedAccessToken = token
            cachedExpiresAt = expiry
            token
        }
    }

    suspend fun getUserId(): String? = userIdFlow.firstOrNull()

    suspend fun getRefreshToken(): String? = context.dataStore.data.map { it[KEY_REFRESH_TOKEN] }.firstOrNull()

    /**
     * Check if the current session is valid (token exists and is not expired).
     */
    suspend fun isSessionValid(): Boolean {
        val expiry = context.dataStore.data.map { it[KEY_EXPIRES_AT] }.firstOrNull() ?: 0L
        val token = context.dataStore.data.map { it[KEY_ACCESS_TOKEN] }.firstOrNull()
        // Buffer of 60 seconds for network latency
        return !token.isNullOrEmpty() && System.currentTimeMillis() < (expiry - 60_000)
    }

    /** Returns the session ID for audit log correlation. */
    suspend fun getSessionId(): String? {
        return context.dataStore.data.map { it[KEY_SESSION_ID] }.firstOrNull()
    }

    private fun isTokenExpiringSoon(expiresAt: Long): Boolean {
        return System.currentTimeMillis() >= (expiresAt - REFRESH_BUFFER_MS)
    }
}
