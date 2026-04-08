package com.pranav.punecityguide.data

import com.pranav.punecityguide.BuildConfig
import com.pranav.punecityguide.model.CommunityPost
import io.ktor.client.request.header
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.delete
import io.ktor.client.request.patch
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.isSuccess
import io.ktor.client.call.body
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

object CommunityRepository {
    suspend fun getRecentPosts(limit: Int = 20): Result<List<CommunityPost>> {
        if (!SupabaseRest.isConfigured) return Result.success(emptyList())

        return try {
            val response = SupabaseRest.client.get("${SupabaseRest.projectUrl}/rest/v1/${BuildConfig.COMMUNITY_TABLE}") {
                SupabaseRest.applyAuth(this)
                parameter("select", "*")
                parameter("order", "created_at.desc")
                parameter("limit", limit)
            }

            if (!response.status.isSuccess()) {
                return Result.failure(IllegalStateException("Community request failed: ${response.status}"))
            }

            val rows: List<JsonObject> = response.body()
            Result.success(rows.mapNotNull { row -> row.toCommunityPostOrNull() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPost(author: String, content: String, userToken: String? = null): Result<Unit> {
        if (!SupabaseRest.isConfigured) {
            return Result.failure(IllegalStateException("Service not configured"))
        }

        return try {
            val response = SupabaseRest.client.post("${SupabaseRest.projectUrl}/rest/v1/${BuildConfig.COMMUNITY_TABLE}") {
                SupabaseRest.applyAuth(this, userToken)
                header("Content-Type", "application/json")
                header("Prefer", "return=minimal")
                setBody(
                    buildJsonObject {
                        put("author_name", author)
                        put("content", content)
                    }
                )
            }

            if (!response.status.isSuccess()) {
                return Result.failure(IllegalStateException("Post failed: ${response.status}"))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updatePost(postId: String, newContent: String, userToken: String? = null): Result<Unit> {
        if (!SupabaseRest.isConfigured) {
            return Result.failure(IllegalStateException("Service not configured"))
        }

        return try {
            val response = SupabaseRest.client.patch("${SupabaseRest.projectUrl}/rest/v1/${BuildConfig.COMMUNITY_TABLE}") {
                SupabaseRest.applyAuth(this, userToken)
                header("Content-Type", "application/json")
                header("Prefer", "return=minimal")
                parameter("id", "eq.$postId")
                setBody(
                    buildJsonObject {
                        put("content", newContent)
                        put("updated_at", "now()")
                    }
                )
            }

            if (!response.status.isSuccess()) {
                return Result.failure(IllegalStateException("Update failed: ${response.status}"))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deletePost(postId: String, userToken: String? = null): Result<Unit> {
        if (!SupabaseRest.isConfigured) {
            return Result.failure(IllegalStateException("Service not configured"))
        }

        return try {
            val response = SupabaseRest.client.delete("${SupabaseRest.projectUrl}/rest/v1/${BuildConfig.COMMUNITY_TABLE}") {
                SupabaseRest.applyAuth(this, userToken)
                parameter("id", "eq.$postId")
            }

            if (!response.status.isSuccess()) {
                return Result.failure(IllegalStateException("Delete failed: ${response.status}"))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun JsonObject.toCommunityPostOrNull(): CommunityPost? {
    val id = idAsString() ?: return null
    val content = firstString("content", "body", "description", "text", "title") ?: return null
    val author = firstString("author", "author_name", "user_name", "username", "userId") ?: "Punekar"
    val createdAt = firstString("created_at", "createdAt", "timestamp")

    return CommunityPost(
        id = id,
        author = author,
        content = content,
        createdAt = createdAt,
    )
}

private fun JsonObject.idAsString(): String? {
    val primitive = this["id"] as? JsonPrimitive ?: return null
    if (primitive.isString) return primitive.contentOrNull
    primitive.intOrNull?.let { return it.toString() }
    primitive.longOrNull?.let { return it.toString() }
    primitive.doubleOrNull?.let { d -> return d.toLong().toString() }
    return primitive.contentOrNull
}

private fun JsonObject.stringOf(key: String): String? {
    val primitive = this[key] as? JsonPrimitive ?: return null
    return primitive.contentOrNull
}

private fun JsonObject.firstString(vararg keys: String): String? {
    return keys.firstNotNullOfOrNull { key -> stringOf(key) }
}
