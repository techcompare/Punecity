package com.pranav.punecityguide.data.repository

import com.pranav.punecityguide.AppConfig
import com.pranav.punecityguide.data.model.*
import com.pranav.punecityguide.data.service.SupabaseClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import com.pranav.punecityguide.data.service.RemoteAiService
import com.pranav.punecityguide.data.service.AiMessage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PuneConnectRepository {
    private val client: HttpClient
        get() = SupabaseClient.getHttpClient()

    private val baseUrl = AppConfig.Supabase.SUPABASE_URL

    // --- Storage (Image Upload) ---

    suspend fun uploadImage(fileName: String, bytes: ByteArray): Result<String> {
        return try {
            val response = client.post("$baseUrl/storage/v1/object/posts/$fileName") {
                contentType(ContentType.Image.JPEG)
                setBody(bytes)
            }
            if (response.status.isSuccess()) {
                val publicUrl = "$baseUrl/storage/v1/object/public/posts/$fileName"
                Result.success(publicUrl)
            } else {
                Result.failure(Exception("Failed to upload image: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Posts ---

    suspend fun getPosts(
        sortBy: String = "created_at", // "created_at" or "score" (needs computed column or sort logic)
        area: String? = null,
        category: String? = null,
        limit: Int = 20,
        offset: Int = 0
    ): Result<List<ConnectPost>> {
        return try {
            val response = client.get("$baseUrl/rest/v1/${AppConfig.Supabase.TABLE_POSTS}") {
                parameter("select", "*")
                if (area != null) parameter("area", "eq.$area")
                if (category != null) parameter("category", "eq.$category")

                if (sortBy == "trending") {
                    // Assuming a computed column 'score' exists or sorting by upvotes for now
                    // Ideally: parameter("order", "score.desc") if you have a view or computed column
                    parameter("order", "upvotes.desc") 
                } else {
                    parameter("order", "created_at.desc")
                }

                parameter("limit", limit)
                parameter("offset", offset)
            }
            if (response.status.isSuccess()) {
                val posts: List<ConnectPost> = response.body()
                Result.success(posts)
            } else {
                if (response.status.value == 401) {
                    // Auto-recovery: If 401, it might be a stale user token. 
                    // Clear session and retry once with Anon Key.
                    com.pranav.punecityguide.util.Logger.w("PuneConnectRepository: 401 Unauthorized. Attempting to clear stale session and retry.")
                    
                    SupabaseClient.getSessionManager().clearSession()
                    SupabaseClient.invalidateAuthCache()
                    
                    // Retry request (now it will use Anon Key)
                    val retryResponse = client.get("$baseUrl/rest/v1/${AppConfig.Supabase.TABLE_POSTS}") {
                        parameter("select", "*")
                        if (area != null) parameter("area", "eq.$area")
                        if (category != null) parameter("category", "eq.$category")
                        if (sortBy == "trending") parameter("order", "upvotes.desc") 
                        else parameter("order", "created_at.desc")
                        parameter("limit", limit)
                        parameter("offset", offset)
                    }
                    
                    if (retryResponse.status.isSuccess()) {
                         return Result.success(retryResponse.body())
                    }
                    
                    val anonKey = AppConfig.Supabase.SUPABASE_ANON_KEY
                    if (anonKey.startsWith("sb_") || !anonKey.startsWith("ey")) {
                         return Result.failure(Exception("Config Error: Invalid SUPABASE_ANON_KEY. It must be a JWT starting with 'ey...', not '${anonKey.take(5)}...'"))
                    }
                    return Result.failure(Exception("Unauthorized (401): Check RLS policies or API Key."))
                }
                Result.failure(Exception("Failed to fetch posts: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDistinctAreas(): Result<List<String>> {
        return try {
            val response = client.get("$baseUrl/rest/v1/posts") {
                parameter("select", "area")
                parameter("limit", "100")
            }
            if (response.status.isSuccess()) {
                val posts: List<ConnectPost> = response.body()
                val areas = posts.mapNotNull { it.area }.filter { it.isNotBlank() }.distinct().sorted()
                Result.success(areas)
            } else {
                Result.failure(Exception("Failed to fetch areas: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPost(post: ConnectPost): Result<ConnectPost> {
        return try {
            val response = client.post("$baseUrl/rest/v1/${AppConfig.Supabase.TABLE_POSTS}") {
                contentType(ContentType.Application.Json)
                setBody(post) // Send object directly to avoid mixed-type map serialization error [String, Int]
                parameter("prefer", "return=representation")
            }
            if (response.status.isSuccess()) {
                val createdPosts: List<ConnectPost> = response.body()
                val createdPost = createdPosts.first()
                
                // --- Outstanding Backend: AI Auto-Response ---
                // Trigger a background "AI Discussion Starter" comment
                kotlinx.coroutines.GlobalScope.launch {
                    try {
                        val aiPrompt = """
                            A user just posted this on Pune Connect:
                            Title: ${createdPost.title}
                            Area: ${createdPost.area}
                            Description: ${createdPost.description}
                            
                            As the Pune Connect AI, write a short, helpful, and legendary Puneri comment (2 lines max). 
                            If it's a traffic issue, be sympathetic. If it's food, be excited.
                        """.trimIndent()
                        
                        val aiReply = RemoteAiService.getChatResponse(aiPrompt).getOrNull()
                        if (aiReply != null) {
                            val aiComment = ConnectComment(
                                id = java.util.UUID.randomUUID().toString(),
                                postId = createdPost.id,
                                userId = "00000000-0000-0000-0000-000000000000", // Pune AI Assistant System ID
                                text = "🤖 *Pune AI Insight:* $aiReply",
                                createdAt = null // Let Supabase handle now()
                            )
                            addComment(aiComment).onFailure { 
                                com.pranav.punecityguide.util.Logger.e("Failed to post AI auto-comment: ${it.message}")
                            }
                        }
                    } catch (e: Exception) {
                        // Silent fail for AI side-effect
                    }
                }
                
                Result.success(createdPost)
            } else {
                Result.failure(Exception("Failed to create post"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Comments ---

    suspend fun getComments(postId: String): Result<List<ConnectComment>> {
        return try {
            val response = client.get("$baseUrl/rest/v1/comments") {
                parameter("select", "*")
                parameter("post_id", "eq.$postId")
                parameter("order", "created_at.asc")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to fetch comments"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addComment(comment: ConnectComment): Result<ConnectComment> {
         return try {
            val response = client.post("$baseUrl/rest/v1/comments") {
                contentType(ContentType.Application.Json)
                setBody(comment)
                parameter("prefer", "return=representation")
            }
            if (response.status.isSuccess()) {
                val created: List<ConnectComment> = response.body()
                Result.success(created.first())
            } else {
                Result.failure(Exception("Failed to add comment: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Votes ---

    suspend fun votePost(postId: String, voteType: Int): Result<Unit> {
        val userId = SupabaseClient.getSessionManager().getUserId() ?: return Result.failure(Exception("User not logged in"))
        return try {
            val response = client.post("$baseUrl/rest/v1/votes") {
                contentType(ContentType.Application.Json)
                setBody(ConnectVote(id = java.util.UUID.randomUUID().toString(), userId = userId, postId = postId, voteType = voteType))
                header("Prefer", "resolution=merge-duplicates")
            }
            if (response.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("Failed to vote"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Saved Posts ---

    suspend fun toggleSavePost(postId: String, isAlreadySaved: Boolean): Result<Unit> {
        val userId = SupabaseClient.getSessionManager().getUserId() ?: return Result.failure(Exception("User not logged in"))
        return try {
            if (isAlreadySaved) {
                val response = client.delete("$baseUrl/rest/v1/saved_posts") {
                    parameter("user_id", "eq.$userId")
                    parameter("post_id", "eq.$postId")
                }
                if (response.status.isSuccess()) Result.success(Unit)
                else Result.failure(Exception("Failed to unsave"))
            } else {
                val response = client.post("$baseUrl/rest/v1/saved_posts") {
                    contentType(ContentType.Application.Json)
                    setBody(ConnectSavedPost(id = java.util.UUID.randomUUID().toString(), userId = userId, postId = postId))
                }
                if (response.status.isSuccess()) Result.success(Unit)
                else Result.failure(Exception("Failed to save"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unsavePost(userId: String, postId: String): Result<Unit> {
        return try {
            val response = client.delete("$baseUrl/rest/v1/saved_posts") {
                parameter("user_id", "eq.$userId")
                parameter("post_id", "eq.$postId")
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to unsave post"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getCurrentUserId(): String? = SupabaseClient.getSessionManager().getUserId()

    suspend fun getSavedPostIds(userId: String): Result<List<String>> {
        return try {
            val response = client.get("$baseUrl/rest/v1/saved_posts") {
                parameter("select", "post_id")
                parameter("user_id", "eq.$userId")
            }
            if (response.status.isSuccess()) {
                val list: List<ConnectSavedPost> = response.body()
                Result.success(list.map { it.postId })
            } else {
                Result.failure(Exception("Failed to fetch saved post IDs"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSavedPosts(userId: String): Result<List<ConnectSavedPost>> {
        return try {
             val response = client.get("$baseUrl/rest/v1/saved_posts") {
                parameter("select", "*")
                parameter("user_id", "eq.$userId")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to fetch saved posts"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- User Profile ---

    suspend fun getUserProfile(userId: String): Result<ConnectUser> {
        return try {
            val response = client.get("$baseUrl/rest/v1/${AppConfig.Supabase.TABLE_PROFILES}") {
                parameter("select", "*")
                parameter("id", "eq.$userId")
                parameter("limit", "1")
            }
            if (response.status.isSuccess()) {
                val users: List<ConnectUser> = response.body()
                if (users.isNotEmpty()) {
                    Result.success(users.first())
                } else {
                    Result.failure(Exception("User not found"))
                }
            } else {
                Result.failure(Exception("Failed to fetch user profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createUserProfile(user: ConnectUser): Result<ConnectUser> {
        return try {
            val response = client.post("$baseUrl/rest/v1/${AppConfig.Supabase.TABLE_PROFILES}") {
                contentType(ContentType.Application.Json)
                header("Prefer", "resolution=merge-duplicates, return=representation")
                setBody(user)
            }
            if (response.status.isSuccess()) {
                val created: List<ConnectUser> = response.body()
                Result.success(created.first())
            } else {
                Result.failure(Exception("Failed to create user profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPostsByIds(ids: List<String>): Result<List<ConnectPost>> {
        if (ids.isEmpty()) return Result.success(emptyList())
        return try {
            val response = client.get("$baseUrl/rest/v1/${AppConfig.Supabase.TABLE_POSTS}") {
                parameter("select", "*")
                parameter("id", "in.(${ids.joinToString(",")})")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to fetch posts by IDs"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPostsByUserId(userId: String): Result<List<ConnectPost>> {
        return try {
            val response = client.get("$baseUrl/rest/v1/${AppConfig.Supabase.TABLE_POSTS}") {
                parameter("select", "*")
                parameter("user_id", "eq.$userId")
                parameter("order", "created_at.desc")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to fetch user posts"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPostById(id: String): Result<ConnectPost> {
        return try {
            val response = client.get("$baseUrl/rest/v1/${AppConfig.Supabase.TABLE_POSTS}") {
                parameter("select", "*")
                parameter("id", "eq.$id")
                header("Accept", "application/vnd.pgrst.object+json")
            }
            if (response.status.isSuccess()) {
                val post: ConnectPost = response.body()
                Result.success(post)
            } else {
                Result.failure(Exception("Failed to fetch post details"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Global Lounge (Real-time Chat) ---

    suspend fun getLoungeMessages(limit: Int = 50): Result<List<GlobalChatMessage>> {
        return try {
            val response = client.get("$baseUrl/rest/v1/${AppConfig.Supabase.TABLE_LOUNGE}") {
                parameter("select", "*")
                parameter("order", "created_at.desc") // Get newest first
                parameter("limit", limit)
            }
            if (response.status.isSuccess()) {
                val msgs: List<GlobalChatMessage> = response.body()
                Result.success(msgs.reversed()) // Return in chronological order
            } else {
                Result.failure(Exception("Failed to fetch lounge messages: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendLoungeMessage(userId: String, userName: String, text: String): Result<GlobalChatMessage> {
        return try {
            val msg = GlobalChatMessage(
                id = java.util.UUID.randomUUID().toString(),
                userId = userId,
                userName = userName,
                text = text,
                createdAt = null
            )
            val response = client.post("$baseUrl/rest/v1/${AppConfig.Supabase.TABLE_LOUNGE}") {
                contentType(ContentType.Application.Json)
                setBody(msg)
                parameter("prefer", "return=representation")
            }
            if (response.status.isSuccess()) {
                val created: List<GlobalChatMessage> = response.body()
                Result.success(created.first())
            } else {
                Result.failure(Exception("Failed to send message: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
