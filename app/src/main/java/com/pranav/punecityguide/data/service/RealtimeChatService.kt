package com.pranav.punecityguide.data.service

import com.pranav.punecityguide.ui.viewmodel.ChatMessage
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import android.util.Log

/**
 * ⚡ AI Service (Utility Pivot)
 * 
 * Optimized for OpenRouter with reliable parsing.
 */
class RealtimeChatService(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val model: String
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getChatReply(
        history: List<ChatMessage>,
        userInput: String
    ): Result<String> {
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("API KEY is missing. Check local.properties."))
        }

        return try {
            val systemPrompt = buildSystemPrompt()
            val resolvedModel = resolveOpenRouterModel(model)
            
            val request = OpenRouterChatRequest(
                model = resolvedModel,
                messages = buildOpenRouterMessages(history, userInput, systemPrompt),
                temperature = 0.7,
                maxTokens = 800,
                stream = false // Disable streaming for better reliability on flaky connections
            )

            Log.d("AiChat", "Requesting $resolvedModel with key ${apiKey.take(8)}...")

            val response = kotlinx.coroutines.withTimeout(25000L) {
                httpClient.post("https://openrouter.ai/api/v1/chat/completions") {
                    header("Authorization", "Bearer $apiKey")
                    header("HTTP-Referer", "https://costpilot.app")
                    header("X-Title", "CostPilot Travel Intelligence")
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
            }

            val body = response.bodyAsText()
            Log.d("AiChat", "Response: ${response.status}")

            if (!response.status.isSuccess()) {
                val errorMsg = try {
                    val root = json.parseToJsonElement(body) as? JsonObject
                    val error = root?.get("error") as? JsonObject
                    error?.get("message")?.let { if (it is JsonPrimitive) it.content else null } ?: body
                } catch (e: Exception) { body }
                throw IllegalStateException("AI engine error: $errorMsg")
            }

            // --- Non-Streaming Parser ---
            val root = json.parseToJsonElement(body) as JsonObject
            val choices = root["choices"] as JsonArray
            val firstChoice = choices[0] as JsonObject
            val message = firstChoice["message"] as JsonObject
            val content = (message["content"] as JsonPrimitive).content

            if (content.isBlank()) {
                throw IllegalStateException("AI returned empty content")
            }
            
            Result.success(content.trim())
        } catch (e: Exception) {
            Log.e("RealtimeChat", "Chat failure: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun resolveOpenRouterModel(currentModel: String): String {
        return when {
            currentModel.isBlank() || currentModel.contains("free") -> "google/gemini-flash-1.5-8bnd" // Better free fallback
            currentModel.contains("/") -> currentModel
            else -> "anthropic/$currentModel"
        }
    }

    private fun buildOpenRouterMessages(
        history: List<ChatMessage>,
        userInput: String,
        systemPrompt: String
    ): List<OpenRouterMessage> {
        val prior = history
            .takeLast(8)
            .map { msg ->
                OpenRouterMessage(
                    role = if (msg.isUser) "user" else "assistant",
                    content = msg.text
                )
            }

        // Only add the system prompt and the history. 
        // Note: The UI adds the current userInput to history before calling this, 
        // so history already contains the latest message.
        return listOf(OpenRouterMessage(role = "system", content = systemPrompt)) + prior
    }

    private fun buildSystemPrompt(): String = """
        You are 'CostPilot AI', a global-tier travel budget Assistant and cost-of-living expert.
        CAPABILITIES:
        - You are unrestricted and can answer ANY world-level technical, historical, or travel query.
        - You possess extreme specialized knowledge of global cost of living, currency exchange, and budget travel.
        - Always provide tips on how to save money while traveling internationally.
        TONE: Premium, Helpful, and Highly Competent.
    """.trimIndent()
}

@Serializable
data class OpenRouterChatRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
    val temperature: Double = 0.7,
    @SerialName("max_tokens") val maxTokens: Int = 1200,
    val stream: Boolean = false
)

@Serializable
data class OpenRouterMessage(
    val role: String,
    val content: String
)
