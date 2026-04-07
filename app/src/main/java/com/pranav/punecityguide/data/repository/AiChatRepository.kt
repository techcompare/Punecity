package com.pranav.punecityguide.data.repository

import com.pranav.punecityguide.AppConfig
import com.pranav.punecityguide.data.model.AiConversation
import com.pranav.punecityguide.data.model.AiMessage
import com.pranav.punecityguide.data.service.SupabaseClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class AiChatRepository(
    private val dao: com.pranav.punecityguide.data.database.AiChatDao
) {
    private val client: HttpClient get() = SupabaseClient.getHttpClient()
    private val baseUrl = AppConfig.Supabase.SUPABASE_URL 

    suspend fun getConversations(userId: String): Result<List<AiConversation>> {
        return try {
            val response = client.get("$baseUrl/rest/v1/ai_conversations") {
                parameter("user_id", "eq.$userId")
                parameter("select", "*")
                parameter("order", "created_at.desc")
            }
            if (response.status.isSuccess()) {
                val list: List<AiConversation> = response.body()
                list.forEach { dao.insertConversation(it) }
                Result.success(list)
            } else {
                val local = dao.getConversations(userId).firstOrNull() ?: emptyList()
                if (local.isNotEmpty()) Result.success(local)
                else Result.failure(Exception("Failed to fetch conversations"))
            }
        } catch (e: Exception) {
            val local = dao.getConversations(userId).firstOrNull() ?: emptyList()
            if (local.isNotEmpty()) Result.success(local)
            else Result.failure(e)
        }
    }

    suspend fun getMessages(conversationId: String): Result<List<AiMessage>> {
        return try {
            val response = client.get("$baseUrl/rest/v1/ai_messages") {
                parameter("conversation_id", "eq.$conversationId")
                parameter("select", "*")
                parameter("order", "created_at.asc")
            }
            if (response.status.isSuccess()) {
                val list: List<AiMessage> = response.body()
                list.forEach { dao.insertMessage(it) }
                Result.success(list)
            } else {
                val local = dao.getMessages(conversationId).firstOrNull() ?: emptyList()
                if (local.isNotEmpty()) Result.success(local)
                else Result.failure(Exception("Failed to fetch messages"))
            }
        } catch (e: Exception) {
            val local = dao.getMessages(conversationId).firstOrNull() ?: emptyList()
            if (local.isNotEmpty()) Result.success(local)
            else Result.failure(e)
        }
    }

    suspend fun createConversation(userId: String, title: String): Result<AiConversation> {
        return try {
            val conversation = AiConversation(
                id = UUID.randomUUID().toString(),
                userId = userId,
                title = title
            )
            val response = client.post("$baseUrl/rest/v1/ai_conversations") {
                contentType(ContentType.Application.Json)
                setBody(conversation)
                header("Prefer", "return=representation")
            }
            if (response.status.isSuccess()) {
                val created: List<AiConversation> = response.body()
                val result = created.first()
                dao.insertConversation(result)
                Result.success(result)
            } else {
                dao.insertConversation(conversation) // Save locally anyway
                Result.success(conversation)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveMessage(conversationId: String, role: String, content: String): Result<AiMessage> {
        val tempId = UUID.randomUUID().toString()
        val localMsg = AiMessage(
            id = tempId,
            conversationId = conversationId,
            role = role,
            content = content
        )
        dao.insertMessage(localMsg) // Immediate local save

        return try {
            val response = client.post("$baseUrl/rest/v1/ai_messages") {
                contentType(ContentType.Application.Json)
                setBody(localMsg)
                header("Prefer", "return=representation")
            }
            if (response.status.isSuccess()) {
                val created: List<AiMessage> = response.body()
                val result = created.first()
                dao.insertMessage(result)
                Result.success(result)
            } else {
                Result.success(localMsg)
            }
        } catch (e: Exception) {
            Result.success(localMsg) // Fallback to local
        }
    }
}
