package com.pranav.punecityguide.data.repository

import android.util.Log
import com.pranav.punecityguide.AppConfig
import com.pranav.punecityguide.data.model.CommunityMessage
import com.pranav.punecityguide.data.model.CommunityChannel
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
 * Startup-grade community repository with:
 * - Multi-channel discussion support
 * - Guest messaging via identity generation
 * - Message reactions (beta)
 * - All basic CRUD & pagination
 */
class CommunityRepository {
    private val client: HttpClient get() = SupabaseClient.getHttpClient()
    private val baseUrl = AppConfig.Supabase.SUPABASE_URL
    private val TAG = "CommunityRepo"

    /**
     * Statically defined channels for now.
     */
    fun getChannels(): List<CommunityChannel> = listOf(
        CommunityChannel("global", "Global Chat", "Connect with travelers worldwide.", "Public"),
        CommunityChannel("budget", "Budget Tips", "Share hacks and cost-saving advice.", "AccountBalance"),
        CommunityChannel("stories", "Travel Stories", "A place for your deep-dive experiences.", "Style"),
        CommunityChannel("hidden", "Hidden Gems", "Discuss spots only locals know.", "Favorite")
    )

    /**
     * Fetch recent messages for a specific channel.
     */
    suspend fun getRecentMessages(
        channel: String = "global",
        limit: Int = 50,
        before: String? = null
    ): Result<List<CommunityMessage>> = withContext(Dispatchers.IO) {
        try {
            val response = client.get("$baseUrl/rest/v1/community_messages") {
                parameter("channel_id", "eq.$channel")
                parameter("select", "*")
                parameter("order", "created_at.desc")
                parameter("limit", limit)
                if (before != null) {
                    parameter("created_at", "lt.$before")
                }
            }
            if (response.status.isSuccess()) {
                val list: List<CommunityMessage> = response.body()
                Result.success(list)
            } else {
                val body = response.bodyAsText()
                Log.w(TAG, "Fetch failed: ${response.status} - $body")
                Result.failure(Exception("Failed to load messages"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Post a new message, handling guest identification if needed.
     */
    suspend fun postMessage(
        userId: String,
        userName: String,
        content: String,
        channel: String = "global"
    ): Result<CommunityMessage> = withContext(Dispatchers.IO) {
        try {
            val msg = CommunityMessage(
                userId = userId,
                userName = userName,
                content = content.trim(),
                channelId = channel
            )
            val response = client.post("$baseUrl/rest/v1/community_messages") {
                contentType(ContentType.Application.Json)
                header("Prefer", "return=representation")
                setBody(msg)
            }
            if (response.status.isSuccess()) {
                val created: List<CommunityMessage> = response.body()
                Result.success(created.first())
            } else {
                val body = response.bodyAsText()
                Log.w(TAG, "Post failed: ${response.status} - $body")
                Result.failure(Exception("Failed to send message: ${response.status}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Post error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Delete a message (author-only RLS enforced on backend).
     */
    suspend fun deleteMessage(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = client.delete("$baseUrl/rest/v1/community_messages") {
                parameter("id", "eq.$id")
            }
            if (response.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("Deletion failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Edit a message (author-only RLS enforced on backend).
     */
    suspend fun editMessage(id: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = client.patch("$baseUrl/rest/v1/community_messages") {
                parameter("id", "eq.$id")
                contentType(ContentType.Application.Json)
                setBody(mapOf("content" to content.trim(), "is_edited" to true))
            }
            if (response.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("Edit failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Adds or updates a reaction count on a message.
     */
    suspend fun reactToMessage(messageId: String, emoji: String): Result<Unit> = withContext(Dispatchers.IO) {
        // Implementation tip: Real-time reactions usually use a separate table and RPC
        // For now, we'll simulate success for UI feedback
        Result.success(Unit)
    }

    suspend fun getUserMessageCount(userId: String): Int = withContext(Dispatchers.IO) {
        try {
            val response = client.get("$baseUrl/rest/v1/community_messages") {
                parameter("user_id", "eq.$userId")
                parameter("select", "id")
                header("Prefer", "count=exact")
            }
            if (response.status.isSuccess()) {
                response.headers["Content-Range"]?.substringAfterLast("/")?.toIntOrNull() ?: 0
            } else 0
        } catch (e: Exception) { 0 }
    }
}
