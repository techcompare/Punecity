package com.pranav.punecityguide.data.service

import com.pranav.punecityguide.data.model.Attraction
import com.pranav.punecityguide.ui.viewmodel.ChatMessage
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import android.util.Log

/**
 * ⚡ Real-Time AI Service (V6 Philosophy)
 * 
 * A lean, high-performance assistant that uses OpenRouter for global-scale AI.
 * It is fully localized to Pune using Verified Attraction Context.
 */
class RealtimeChatService(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val model: String
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getChatReply(
        history: List<ChatMessage>,
        userInput: String,
        attractionsContext: List<Attraction> = emptyList()
    ): Result<String> {
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("CLAUDE_API_KEY is missing from environment"))
        }

        return NetworkResilience.withRetry("openrouter_chat", maxRetries = 2) {
            val systemPrompt = buildSystemPrompt(attractionsContext)

            val request = OpenRouterChatRequest(
                model = resolveOpenRouterModel(model),
                messages = buildOpenRouterMessages(history, userInput, systemPrompt),
                temperature = 0.7,
                maxTokens = 1200,
                stream = true
            )

            val response = httpClient.post("https://openrouter.ai/api/v1/chat/completions") {
                header("Authorization", "Bearer $apiKey")
                header("HTTP-Referer", "https://punecityguide.local")
                header("X-Title", "Pune City Guide")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val body = response.bodyAsText()
            if (!response.status.isSuccess()) {
                val error = parseOpenRouterError(body)
                throw IllegalStateException("AI engine error: $error")
            }

            // --- SSE Streaming Parser (Instant Feel) ---
            val fullContent = StringBuilder()
            body.lines().forEach { line ->
                if (line.trim().startsWith("data: ")) {
                    val data = line.trim().removePrefix("data: ").trim()
                    if (data == "[DONE]") return@forEach
                    try {
                        val root = json.parseToJsonElement(data) as? JsonObject
                        val choices = (root?.get("choices") as? JsonArray)
                        val delta = choices?.firstOrNull()?.let { it as? JsonObject }?.get("delta") as? JsonObject
                        val contentPart = delta?.get("content")?.let { if (it is JsonPrimitive) it.content else null }
                        if (contentPart != null) {
                            fullContent.append(contentPart)
                        }
                    } catch (e: Exception) {
                        // Suppress parse failures on partial SSE data chunks
                    }
                }
            }

            val finalResult = fullContent.toString().trim()
            if (finalResult.isBlank()) {
                throw IllegalStateException("AI returned an empty response. Verify API settings.")
            }
            finalResult
        }.onFailure { e ->
            Log.e("RealtimeChat", "Chat failure: ${e.message}")
        }.recoverCatching { e ->
            throw Exception(NetworkResilience.classifyError(e))
        }
    }

    private fun resolveOpenRouterModel(currentModel: String): String {
        if (currentModel.contains("/")) return currentModel
        if (currentModel.isEmpty()) return "anthropic/claude-3.5-sonnet"
        // Standardize IDs for OpenRouter
        return when {
            currentModel.contains("claude") -> "anthropic/${currentModel.replace("3-5", "3.5")}"
            else -> "anthropic/claude-3.5-sonnet"
        }
    }

    private fun buildOpenRouterMessages(
        history: List<ChatMessage>,
        userInput: String,
        systemPrompt: String
    ): List<OpenRouterMessage> {
        val prior = history
            .dropWhile { !it.isUser }
            .takeLast(6)
            .map { msg ->
                OpenRouterMessage(
                    role = if (msg.isUser) "user" else "assistant",
                    content = msg.text
                )
            }

        val hasLatestInHistory = history.lastOrNull()?.let { it.isUser && it.text.trim() == userInput.trim() } == true
        val latest = if (hasLatestInHistory) emptyList() else listOf(OpenRouterMessage(role = "user", content = userInput))

        return listOf(OpenRouterMessage(role = "system", content = systemPrompt)) + prior + latest
    }

    private fun parseOpenRouterError(body: String): String {
        return runCatching {
            val root = json.parseToJsonElement(body) as? JsonObject ?: return body
            val error = root["error"] as? JsonObject
            val msg = (error?.get("message") as? JsonPrimitive)?.content
            msg ?: body
        }.getOrDefault(body)
    }

    private fun buildSystemPrompt(attractions: List<Attraction>): String = buildString {
        append(
            """You are a versatile AI City Guide. Deeply knowledgeable about Pune (India) but helpful for ANY worldwide query.
TONE: Helpful, witty, and premium.
RULES:
- Concise (2-4 paragraphs max).
- Bold **Landmark Names**.
- Factual and accurate.
- Use the verified Pune context provided if applicable."""
        )
        if (attractions.isNotEmpty()) {
            append("\n\nVERIFIED PUNE ATTRACTIONS:\n")
            attractions.take(30).forEach { a ->
                append("• ${a.name} | ${a.category} | ⭐${a.rating}\n")
            }
        }
    }
}

// --- Lean Data Models ---

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

@Serializable
data class OpenRouterChatResponse(
    val choices: List<OpenRouterChoice> = emptyList()
)

@Serializable
data class OpenRouterChoice(
    val message: OpenRouterMessageContent = OpenRouterMessageContent()
)

@Serializable
data class OpenRouterMessageContent(
    val content: String = ""
)
