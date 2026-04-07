package com.pranav.punecityguide.data.repository

import android.util.Log
import com.pranav.punecityguide.AppConfig
import com.pranav.punecityguide.data.model.UserProfile
import com.pranav.punecityguide.data.service.SupabaseClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages user profile data in Supabase.
 *
 * Handles:
 * - Profile CRUD (linked to Supabase auth.users)
 * - Display name, avatar, preferred currency, travel style
 * - Profile retrieval with graceful fallback for missing profiles
 */
class UserProfileRepository {

    private val client: HttpClient get() = SupabaseClient.getHttpClient()
    private val baseUrl = AppConfig.Supabase.SUPABASE_URL
    private val TAG = "UserProfileRepo"

    /**
     * Fetch the user's profile. Returns null if no profile exists yet.
     */
    suspend fun getProfile(userId: String): Result<UserProfile?> = withContext(Dispatchers.IO) {
        try {
            val response = client.get("$baseUrl/rest/v1/profiles") {
                parameter("id", "eq.$userId")
                parameter("select", "*")
            }
            if (response.status.isSuccess()) {
                val list: List<UserProfile> = response.body()
                Result.success(list.firstOrNull())
            } else {
                Log.w(TAG, "Profile fetch failed: ${response.status}")
                Result.success(null) // Treat as "no profile yet"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Profile fetch error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Create or update user profile via upsert.
     */
    suspend fun upsertProfile(profile: UserProfile): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val response = client.post("$baseUrl/rest/v1/profiles") {
                contentType(ContentType.Application.Json)
                header("Prefer", "return=representation,resolution=merge-duplicates")
                setBody(profile)
            }
            if (response.status.isSuccess()) {
                val created: List<UserProfile> = response.body()
                Log.d(TAG, "Profile upserted for ${profile.id}")
                Result.success(created.first())
            } else {
                val body = response.bodyAsText()
                Log.e(TAG, "Profile upsert failed: ${response.status} - $body")
                Result.failure(Exception("Failed to save profile"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Profile upsert error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Update only specific profile fields.
     */
    suspend fun updateDisplayName(userId: String, displayName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = client.patch("$baseUrl/rest/v1/profiles") {
                parameter("id", "eq.$userId")
                contentType(ContentType.Application.Json)
                setBody(mapOf("display_name" to displayName))
            }
            if (response.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("Failed to update display name"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update preferred currency for the user.
     */
    suspend fun updatePreferredCurrency(userId: String, currency: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = client.patch("$baseUrl/rest/v1/profiles") {
                parameter("id", "eq.$userId")
                contentType(ContentType.Application.Json)
                setBody(mapOf("preferred_currency" to currency))
            }
            if (response.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("Failed to update currency"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete user profile (for GDPR / account deletion compliance).
     */
    suspend fun deleteProfile(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = client.delete("$baseUrl/rest/v1/profiles") {
                parameter("id", "eq.$userId")
            }
            if (response.status.isSuccess()) {
                Log.d(TAG, "Profile deleted for $userId")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
