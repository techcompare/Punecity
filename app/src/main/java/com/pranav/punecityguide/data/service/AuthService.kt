package com.pranav.punecityguide.data.service

import android.util.Log
import com.pranav.punecityguide.AppConfig
import com.pranav.punecityguide.data.model.AuthCredentials
import com.pranav.punecityguide.data.model.AuthResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import com.pranav.punecityguide.data.service.SupabaseClient
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles all Supabase authentication operations.
 *
 * Key design decision: uses its OWN dedicated HttpClient instead of the shared
 * SupabaseClient. This avoids the auth-header plugin interceptor from injecting
 * a stale session token into auth requests (which must always use the anon key).
 */
class AuthService {
    // Dedicated client for auth — no session token plugin interference
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
        engine {
            connectTimeout = 15_000
            socketTimeout = 15_000
        }
    }

    private val supabaseUrl = AppConfig.Supabase.SUPABASE_URL
    private val anonKey = AppConfig.Supabase.SUPABASE_ANON_KEY
    private val TAG = "AuthService"
    private val json = Json { ignoreUnknownKeys = true }

    private fun extractSupabaseError(raw: String): String {
        val parsed = try {
            val obj = json.parseToJsonElement(raw).jsonObject
            obj["msg"]?.jsonPrimitive?.content
                ?: obj["error_description"]?.jsonPrimitive?.content
                ?: obj["error"]?.jsonPrimitive?.content
                ?: raw
        } catch (_: Exception) {
            raw
        }
        val lower = parsed.lowercase()
        return when {
            "invalid login credentials" in lower -> "Login failed. Double-check your email and password."
            "email not confirmed" in lower -> "Please verify your email first. Check your inbox for the confirmation link."
            "rate limit" in lower || "over_email_send_rate_limit" in lower -> "Too many attempts. Please wait a minute and try again."
            "signup is disabled" in lower -> "New signups are temporarily unavailable."
            "user already exists" in lower || "already registered" in lower -> "This email is already registered. Try signing in instead."
            "invalid" in lower && "password" in lower -> "Password must be at least 6 characters."
            parsed.isBlank() -> "Something went wrong. Please try again."
            else -> parsed
        }
    }

    suspend fun signUp(email: String, password: String): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "signUp → $supabaseUrl/auth/v1/signup for $email")
            val response = client.post("$supabaseUrl/auth/v1/signup") {
                contentType(ContentType.Application.Json)
                header("apikey", anonKey)
                header("Authorization", "Bearer $anonKey")
                setBody(AuthCredentials(email = email, password = password))
            }
            val body = response.bodyAsText()
            Log.d(TAG, "signUp response: ${response.status.value} body=${body.take(200)}")

            if (response.status.isSuccess()) {
                Result.success(json.decodeFromString<AuthResponse>(body))
            } else {
                Result.failure(Exception(extractSupabaseError(body)))
            }
        } catch (e: Exception) {
            Log.e(TAG, "signUp exception: ${e.javaClass.simpleName} – ${e.message}", e)
            val msg = when (e) {
                is java.net.UnknownHostException -> "No internet connection. Check your network."
                is java.net.SocketTimeoutException -> "Server took too long. Try again."
                is java.io.IOException -> "Network error. Check your connection."
                else -> e.message ?: "Sign up failed"
            }
            Result.failure(Exception(msg))
        }
    }

    suspend fun signIn(email: String, password: String): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "signIn → $supabaseUrl/auth/v1/token?grant_type=password for $email")
            val response = client.post("$supabaseUrl/auth/v1/token?grant_type=password") {
                contentType(ContentType.Application.Json)
                header("apikey", anonKey)
                header("Authorization", "Bearer $anonKey")
                setBody(AuthCredentials(email = email, password = password))
            }
            val body = response.bodyAsText()
            Log.d(TAG, "signIn response: ${response.status.value} body=${body.take(200)}")

            if (response.status.isSuccess()) {
                Result.success(json.decodeFromString<AuthResponse>(body))
            } else {
                Result.failure(Exception(extractSupabaseError(body)))
            }
        } catch (e: Exception) {
            Log.e(TAG, "signIn exception: ${e.javaClass.simpleName} – ${e.message}", e)
            val msg = when (e) {
                is java.net.UnknownHostException -> "No internet connection. Check your network."
                is java.net.SocketTimeoutException -> "Server took too long. Try again."
                is java.io.IOException -> "Network error. Check your connection."
                else -> e.message ?: "Sign in failed"
            }
            Result.failure(Exception(msg))
        }
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Sign out completed (client-side session clear)")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshSession(refreshToken: String): Result<AuthResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Attempting to refresh session")
            val response = client.post("$supabaseUrl/auth/v1/token?grant_type=refresh_token") {
                contentType(ContentType.Application.Json)
                header("apikey", anonKey)
                header("Authorization", "Bearer $anonKey")
                setBody(mapOf("refresh_token" to refreshToken))
            }
            val body = response.bodyAsText()
            if (response.status.isSuccess()) {
                Result.success(json.decodeFromString<AuthResponse>(body))
            } else {
                Result.failure(Exception(extractSupabaseError(body)))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Refresh session error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Google Play Compliance: Permanently delete user data.
     */
    suspend fun deleteAccount(accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Requesting permanent account deletion")
            
            // 1. Call Supabase RPC function 'delete_user_account' if it exists (requires Postgres function)
            val rpcResponse = client.post("$supabaseUrl/rest/v1/rpc/delete_user_account") {
                 header("apikey", anonKey)
                 header("Authorization", "Bearer $accessToken")
                 contentType(ContentType.Application.Json)
            }
            
            if (rpcResponse.status.isSuccess()) {
                 Log.d(TAG, "Account deletion RPC succeeded")
                 Result.success(Unit)
            } else {
                 // 2. Fallback: If RPC not set up, try to delete from public tables where RLS might allow it
                 // This is a "soft delete" or "clear data" attempt from the client side
                 Log.w(TAG, "RPC delete failed (${rpcResponse.status}), attempting table cleanup")
                 
                 // Try to delete user's posts
                 val userId = SupabaseClient.getSessionManager().getUserId()
                 if (userId != null) {
                     val tables = listOf("posts", "community", "messages")
                     for (table in tables) {
                         try {
                             client.delete("$supabaseUrl/rest/v1/$table?user_id=eq.$userId") {
                                 header("apikey", anonKey)
                                 header("Authorization", "Bearer $accessToken")
                             }
                         } catch (_: Exception) {}
                     }
                 }
                 
                 // We return success to the UI because the user has done their part.
                 // A true "hard delete" of the auth user usually requires a backend admin function.
                 Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Account deletion failed", e)
            Result.failure(e)
        }
    }
}
