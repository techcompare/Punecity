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

/**
 * Stateless remote AI service for quick one-shot queries.
 *
 * For conversational AI with history, use RealtimeChatService instead.
 * This service is best for:
 * - Quick budget estimates
 * - Currency conversion explanations
 * - Travel tip generation
 */
object RemoteAiService {
    private val json = Json { ignoreUnknownKeys = true }
    
    suspend fun getChatResponse(userMessage: String, chatHistory: List<AiMessage> = emptyList()): Result<String> {
        val httpClient = SupabaseClient.getHttpClient()
        val messages = mutableListOf<AiMessage>()
        messages.add(AiMessage("system", """
            You are 'CostPilot AI', a world-class travel budget intelligence assistant.
            You specialize in:
            - Global cost-of-living comparisons
            - Travel budget planning and optimization
            - Currency exchange insights
            - Money-saving tips for international travelers
            Provide accurate, data-driven, and actionable responses in a premium, professional tone.
        """.trimIndent()))
        messages.addAll(chatHistory)
        messages.add(AiMessage("user", userMessage))

        return try {
            val response: OpenRouterResponse = httpClient.post("https://openrouter.ai/api/v1/chat/completions") {
                header("Authorization", "Bearer ${BuildConfig.CLAUDE_API_KEY}")
                header("HTTP-Referer", "https://costpilot.app")
                header("X-Title", "CostPilot Travel Intelligence")
                contentType(ContentType.Application.Json)
                setBody(OpenRouterRequest(model = BuildConfig.CLAUDE_MODEL, messages = messages))
            }.body()

            if (response.error != null) {
                Result.failure(Exception(response.error.message))
            } else {
                val reply = response.choices?.firstOrNull()?.message?.content 
                    ?: "I wasn't able to generate a response. Please try again."
                Result.success(reply)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
