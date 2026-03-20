package com.pranav.punecityguide.data.community

import android.util.Log
import com.pranav.punecityguide.AppConfig
import com.pranav.punecityguide.data.service.SupabaseClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import com.pranav.punecityguide.data.service.NetworkResilience

@kotlinx.serialization.Serializable
data class CommunityPost(
    val id: String,
    val userName: String,
    val userAvatarUrl: String = "",
    val placeImageUrl: String = "",
    val description: String,
    val locationTag: String = "",
    val createdAt: String = "",
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val likeCount: Int = 0
)

data class CommunityFeedResult(
    val posts: List<CommunityPost>,
    val tableUsed: String
)

class CommunityFeedService(
    private val httpClient: io.ktor.client.HttpClient = SupabaseClient.getHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    private val baseUrl = AppConfig.Supabase.COMMUNITY_SUPABASE_URL.ifBlank { AppConfig.Supabase.SUPABASE_URL }
    private val anonKey = AppConfig.Supabase.SUPABASE_ANON_KEY
    private val tableName = AppConfig.Supabase.TABLE_POSTS
    
    private val TAG = "CommunityService"
    private var discoveredColumns: Set<String>? = null

    suspend fun fetchPosts(limit: Int = 50): Result<CommunityFeedResult> {
        val candidateTables = listOf(tableName, "posts", "messages", "community")
        
        for (candidate in candidateTables) {
            try {
                val response = httpClient.get("$baseUrl/rest/v1/$candidate") {
                    header("apikey", anonKey)
                    header("Authorization", "Bearer $anonKey")
                    parameter("select", "*")
                    parameter("order", "created_at.desc.nullslast")
                    parameter("limit", limit)
                }

                if (response.status.isSuccess()) {
                    val body = response.bodyAsText()
                    val root = json.parseToJsonElement(body) as? JsonArray ?: JsonArray(emptyList())
                    
                    if (root.isNotEmpty()) {
                        val firstObj = root[0] as? JsonObject
                        discoveredColumns = firstObj?.keys
                    }

                    val posts = root.mapNotNull { it.toCommunityPostOrNull() }
                    lastSuccessfulTable = candidate
                    return Result.success(CommunityFeedResult(posts = posts, tableUsed = candidate))
                }
            } catch (_: Exception) {}
        }
        return Result.failure(Exception("Pune Buzz is currently quiet. Be the first to post!"))
    }

    suspend fun insertPost(description: String, location: String, userName: String, userId: String? = null): Result<Unit> {
        val targetTable = lastSuccessfulTable ?: tableName
        
        // Strategy: Build a robust payload that covers most common PostgREST schemas
        val payload = mutableMapOf<String, String>()
        
        // Map Description/Content
        if (discoveredColumns?.contains("body") == true) payload["body"] = description
        if (discoveredColumns?.contains("content") == true) payload["content"] = description
        if (discoveredColumns?.contains("description") == true) payload["description"] = description
        if (discoveredColumns?.contains("message") == true) payload["message"] = description
        
        if (payload.isEmpty()) payload["body"] = description // Default fallback to user's schema

        // Map Username
        if (discoveredColumns?.contains("username") == true) payload["username"] = userName
        if (discoveredColumns?.contains("user_name") == true) payload["user_name"] = userName
        if (discoveredColumns?.contains("name") == true) payload["name"] = userName
        
        // Removed default fallback to 'user_name' if not discovered, 
        // as the audit shows it doesn't exist in the current schema.

        // Map Location
        if (discoveredColumns?.contains("location") == true) payload["location"] = location
        if (discoveredColumns?.contains("location_tag") == true) payload["location_tag"] = location
        if (discoveredColumns?.contains("community_id") == true) payload["community_id"] = location // Pune-specific ID
        
        // Map User ID
        if (userId != null) {
            payload["user_id"] = userId
        }
        
        val sessionManager = SupabaseClient.getSessionManager()
        val token = sessionManager.getAccessToken() ?: anonKey

        return NetworkResilience.withRetry(tag = "community_insert") {
            val response = httpClient.post("$baseUrl/rest/v1/$targetTable") {
                header("apikey", anonKey)
                header("Authorization", "Bearer $token")
                header("Prefer", "return=minimal")
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            if (!response.status.isSuccess()) {
                val error = response.bodyAsText()
                Log.e(TAG, "Insert failed ($targetTable): $error")
                
                // FINAL RESORT: Try with bare-bones 'body' or 'content' which matches standard templates
                if (response.status.value == 400 || error.contains("column")) {
                     val fallbackPayload = mutableMapOf("body" to description, "title" to "Pune Buzz")
                     if (userId != null) fallbackPayload["user_id"] = userId
                     
                     val fallbackResponse = httpClient.post("$baseUrl/rest/v1/$targetTable") {
                        header("apikey", anonKey)
                        header("Authorization", "Bearer $token")
                        contentType(ContentType.Application.Json)
                        setBody(fallbackPayload)
                    }
                    if (!fallbackResponse.status.isSuccess()) {
                         throw Exception(fallbackResponse.bodyAsText())
                    }
                } else {
                    throw Exception(error)
                }
            }
        }
    }

    suspend fun reportPost(postId: String, reason: String, userName: String): Result<Unit> {
        return try {
            Log.d(TAG, "Reporting post $postId for: $reason")
            val payload = mapOf(
                "post_id" to postId,
                "reason" to reason,
                "reported_by" to userName,
                "timestamp" to System.currentTimeMillis().toString()
            )
            // Attempt to sync report to Supabase (table content_reports)
            val response = httpClient.post("$baseUrl/rest/v1/content_reports") {
                header("apikey", anonKey)
                header("Authorization", "Bearer $anonKey")
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            // Even if table doesn't exist yet, we consider local success for UX
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Report sync failed (Table might not exist): ${e.message}")
            Result.success(Unit)
        }
    }

    suspend fun fetchUserPostCount(userName: String): Result<Int> {
        val targetTable = lastSuccessfulTable ?: tableName
        return try {
            val response = httpClient.get("$baseUrl/rest/v1/$targetTable") {
                header("apikey", anonKey)
                header("Authorization", "Bearer $anonKey")
                parameter("select", "count")
                // Filter by username using multiple possible column names
                parameter("or", "(username.eq.$userName,user_name.eq.$userName,name.eq.$userName)")
            }
            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                val count = Regex("\"count\":\\s*(\\d+)").find(body)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                Result.success(count)
            } else {
                Result.success(0)
            }
        } catch (e: Exception) {
            Result.success(0)
        }
    }

    private var lastSuccessfulTable: String? = null

    private fun JsonElement.toCommunityPostOrNull(): CommunityPost? {
        val obj = this as? JsonObject ?: return null

        val bodyText = obj.firstString("body", "description", "content", "message", "text") ?: return null
        val titleText = obj.firstString("title", "subject")
        val description = if (titleText != null && titleText != "Pune Buzz") "[$titleText] $bodyText" else bodyText

        val id = obj.firstString("id", "post_id", "message_id") ?: description.hashCode().toString()
        val userName = obj.firstString("user_name", "username", "author_name", "name") ?: "Pune User"
        val avatarUrl = obj.firstString("user_avatar_url", "avatar_url", "profile_image_url") ?: ""
        val placeImageUrl = obj.firstString("place_image_url", "image_url", "photo_url") ?: ""
        val locationTag = obj.firstString("location_tag", "location", "place") ?: "Pune"
        val createdAt = obj.firstString("created_at", "inserted_at") ?: ""
        val likeCount = obj.firstInt("like_count", "likes", "upvotes")

        return CommunityPost(
            id = id,
            userName = userName,
            userAvatarUrl = avatarUrl,
            placeImageUrl = placeImageUrl,
            description = description,
            locationTag = locationTag,
            createdAt = createdAt,
            likeCount = likeCount
        )
    }

    private fun JsonObject.firstString(vararg keys: String): String? {
        return keys.firstNotNullOfOrNull { key ->
            val primitive = this[key] as? JsonPrimitive
            primitive?.content?.takeIf { it.isNotBlank() && it != "null" }
        }
    }

    private fun JsonObject.firstInt(vararg keys: String): Int {
        return keys.firstNotNullOfOrNull { key ->
            val primitive = this[key] as? JsonPrimitive
            primitive?.content?.toIntOrNull()
        } ?: 0
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        val targetTable = lastSuccessfulTable ?: tableName
        val token = SupabaseClient.getSessionManager().getAccessToken() ?: anonKey
        return try {
            val response = httpClient.delete("$baseUrl/rest/v1/$targetTable?id=eq.$postId") {
                header("apikey", anonKey)
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("Failed to delete"))
        } catch (e: Exception) {
            Log.w(TAG, "Delete failed: ${e.message}")
            Result.success(Unit) // Optimistic for UX
        }
    }
}