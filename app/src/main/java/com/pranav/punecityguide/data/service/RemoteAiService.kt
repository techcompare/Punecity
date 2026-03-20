package com.pranav.punecityguide.data.service

import com.pranav.punecityguide.BuildConfig
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class OpenRouterRequest(
    val model: String,
    val messages: List<AiMessage>
)

@Serializable
data class AiMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenRouterResponse(
    val choices: List<Choice>? = null,
    val error: ErrorDetail? = null
)

@Serializable
data class Choice(
    val message: AiMessage
)

@Serializable
data class ErrorDetail(
    val message: String
)

object RemoteAiService {
    // We'll use a local json serialize since the one from SupabaseClient might not be accessible easily or might have different settings
    private val json = Json { ignoreUnknownKeys = true }
    
    suspend fun getChatResponse(userMessage: String, chatHistory: List<AiMessage> = emptyList()): Result<String> {
        val httpClient = SupabaseClient.getHttpClient()
        val messages = mutableListOf<AiMessage>()
        messages.add(AiMessage("system", "You are 'Pune Connect AI', a savvy local companion for Pune. Use terms like 'Ek Number' and 'Lay Bhari' occasionally. Be helpful and premium."))
        messages.addAll(chatHistory)
        messages.add(AiMessage("user", userMessage))

        return try {
            val response: OpenRouterResponse = httpClient.post("https://openrouter.ai/api/v1/chat/completions") {
                header("Authorization", "Bearer ${BuildConfig.CLAUDE_API_KEY}")
                header("HTTP-Referer", "https://puneconnect.app")
                header("X-Title", "Pune Connect App")
                contentType(ContentType.Application.Json)
                setBody(OpenRouterRequest(model = BuildConfig.CLAUDE_MODEL, messages = messages))
            }.body()

            if (response.error != null) {
                Result.failure(Exception(response.error.message))
            } else {
                val reply = response.choices?.firstOrNull()?.message?.content ?: "Maaf kara! (Sorry), I couldn't think of a reply right now."
                Result.success(reply)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
