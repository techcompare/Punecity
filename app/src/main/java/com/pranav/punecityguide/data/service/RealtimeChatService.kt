package com.pranav.punecityguide.data.service

import com.pranav.punecityguide.data.model.Attraction
import com.pranav.punecityguide.ui.viewmodel.ChatMessage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import com.pranav.punecityguide.data.service.NetworkResilience
import android.util.Log

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
            return Result.failure(IllegalStateException("CLAUDE_API_KEY is missing"))
        }

        if (apiKey.startsWith("sk-or-v1-")) {
            return getChatReplyViaOpenRouter(history, userInput, attractionsContext)
        }

        return NetworkResilience.withRetry("anthropic_chat", maxRetries = 2) {
            // Fix: Ensure we don't send an OpenRouter model ID to Anthropic
            val actualModel = resolveAnthropicModel(model)
            
            val request = ClaudeChatRequest(
                model = actualModel,
                system = buildSystemPrompt(attractionsContext),
                messages = buildMessages(history, userInput, attractionsContext),
                temperature = 0.7,
                maxTokens = 1282
            )

            val response: ClaudeChatResponse = httpClient.post("https://api.anthropic.com/v1/messages") {
                header("Authorization", "Bearer $apiKey")
                header("x-api-key", apiKey)
                header("anthropic-version", "2023-06-01")
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()

            val content = response.content
                .firstOrNull { it.type == "text" }
                ?.text
                ?.trim()
                .orEmpty()

            if (content.isBlank()) {
                throw IllegalStateException("Empty response from Pune AI service.")
            }
            content
        }.onFailure { e ->
            Log.e("RealtimeChat", "Claude error: ${e.message}")
        }.recoverCatching { e ->
            throw Exception(NetworkResilience.classifyError(e))
        }
    }

    private fun resolveAnthropicModel(currentModel: String): String {
        return currentModel
    }

    private suspend fun getChatReplyViaOpenRouter(
        history: List<ChatMessage>,
        userInput: String,
        attractionsContext: List<Attraction>
    ): Result<String> {
        return NetworkResilience.withRetry("openrouter_chat", maxRetries = 2) {
            val systemPrompt = buildSystemPrompt(attractionsContext)

            val request = OpenRouterChatRequest(
                model = resolveOpenRouterModel(model),
                messages = buildOpenRouterMessages(history, userInput, systemPrompt),
                temperature = 0.7,
                maxTokens = 1282,
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

            val fullContent = java.lang.StringBuilder()
            body.lines().forEach { line ->
                if (line.trim().startsWith("data: ")) {
                    val data = line.trim().removePrefix("data: ").trim()
                    if (data == "[DONE]") return@forEach
                    try {
                        val root = json.parseToJsonElement(data) as? JsonObject
                        val delta = (root?.get("choices") as? JsonArray)?.firstOrNull()?.let { it as? JsonObject }?.get("delta") as? JsonObject
                        val contentPart = delta?.get("content")?.let { if (it is JsonPrimitive) it.content else null }
                        if (contentPart != null) {
                            fullContent.append(contentPart)
                        }

                        val usage = root?.get("usage") as? JsonObject
                        val reasoningTokens = usage?.get("reasoning_tokens") as? JsonPrimitive
                            ?: usage?.get("reasoningTokens") as? JsonPrimitive
                        if (reasoningTokens != null) {
                            Log.d("RealtimeChat", "Reasoning tokens: ${reasoningTokens.content}")
                        }
                    } catch (e: Exception) {
                        // gracefully ignore parse failures on partial SSE data chunks
                    }
                }
            }

            val contentResult = fullContent.toString().trim()
            if (contentResult.isBlank()) {
                throw IllegalStateException("AI returned an empty response.")
            }
            contentResult
        }.onFailure { e ->
            Log.e("RealtimeChat", "OpenRouter error: ${e.message}")
        }.recoverCatching { e ->
            throw Exception(NetworkResilience.classifyError(e))
        }
    }

    private fun buildOpenRouterMessages(
        history: List<ChatMessage>,
        userInput: String,
        systemPrompt: String
    ): List<OpenRouterMessage> {
        val prior = history
            .dropWhile { !it.isUser } // Skip leading assistant messages
            .takeLast(4)
            .map { msg ->
                OpenRouterMessage(
                    role = if (msg.isUser) "user" else "assistant",
                    content = msg.text
                )
            }

        val hasLatestInHistory = history.lastOrNull()?.let { it.isUser && it.text.trim() == userInput.trim() } == true
        val latest = if (hasLatestInHistory) {
            emptyList()
        } else {
            listOf(OpenRouterMessage(role = "user", content = userInput))
        }

        return listOf(OpenRouterMessage(role = "system", content = systemPrompt)) + prior + latest
    }

    /*
    private fun resolveOpenRouterModel(currentModel: String): String { ... }
    */

    private fun parseOpenRouterContent(body: String): String? {
        return runCatching {
            val root = json.parseToJsonElement(body) as? JsonObject ?: return null
            val choices = root["choices"] as? JsonArray ?: return null
            val message = choices.firstOrNull()
                ?.let { it as? JsonObject }
                ?.get("message") as? JsonObject
                ?: return null

            val content = message["content"] ?: return null
            when (content) {
                is JsonPrimitive -> content.content
                is JsonArray -> content.joinToString("\n") { part ->
                    val obj = part as? JsonObject
                    val textPrimitive = obj?.get("text") as? JsonPrimitive
                    textPrimitive?.content.orEmpty()
                }.trim().ifBlank { null }
                else -> null
            }
        }.getOrNull()
    }

    private fun parseOpenRouterError(body: String): String {
        return runCatching {
            val root = json.parseToJsonElement(body) as? JsonObject ?: return body
            val error = root["error"] as? JsonObject
            val msg = (error?.get("message") as? JsonPrimitive)?.content
            msg ?: body
        }.getOrDefault(body)
    }

    /*
    private fun buildMessages(...) { ... }
    */

    private fun buildSystemPrompt(attractions: List<Attraction>): String = buildString {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val dayOfWeek = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
        val isWeekend = dayOfWeek == java.util.Calendar.SATURDAY || dayOfWeek == java.util.Calendar.SUNDAY
        val timeContext = when {
            hour < 7 -> "It's early morning. Suggest breakfast spots, morning walks, and peaceful places."
            hour < 12 -> "It's morning. Great time for sightseeing, heritage walks, and brunch."
            hour in 12..14 -> "It's lunch time. Prioritize food recommendations and nearby restaurants."
            hour < 18 -> "It's afternoon. Suggest cafes, museums, indoor activities, or evening plans."
            hour < 21 -> "It's evening. Suggest dinner spots, viewpoints, evening strolls, and nightlife."
            else -> "It's late night. Only suggest places that are open late — restaurants, lounges, 24hr spots."
        }
        val dayContext = if (isWeekend) "It's the weekend — suggest day trips, treks, family outings." else "It's a weekday — suggest quick visits, work-friendly cafes, lunch spots."

        append(
            """You are a versatile and warm AI Guide. While you have deep, local expertise about Pune (India), you can assist with ANY general query, plan, or recommendation worldwide.
Your tone is helpful, witty, and practical. When discussing Pune, you occasionally use local phrases, but for everything else, you are a globally-aware assistant.

RESPONSE RULES:
- Always be concise (2–4 short paragraphs max).
- If recommending places, **bold the name** and mention a key fact.
- For itineraries, use numbered steps with time estimates.
- Always be factual and never invent information.
- Use the verified Pune context below if the user asks about Pune specifically."""
        )
        if (attractions.isNotEmpty()) {
            append("\n\nVERIFIED PUNE ATTRACTIONS (use only these names):\n")
            attractions
                .sortedByDescending { it.rating }
                .take(30)
                .forEach { a ->
                    val fee = a.entryFee.take(20).ifBlank { "Free" }
                    val hours = a.openingHours.take(25).ifBlank { "Open daily" }
                    append("• ${a.name} | ${a.category} | ⭐${a.rating} | ${fee} | ${hours}\n")
                }
        }
    }

    private fun buildAttractionContext(attractions: List<Attraction>): String {
        if (attractions.isEmpty()) return ""
        return attractions
            .sortedByDescending { it.rating }
            .take(8)
            .joinToString("\n") { a ->
                val fee = a.entryFee.ifBlank { "N/A" }.take(15)
                "${a.name}(${a.category},R:${a.rating},F:$fee)"
            }
    }
}
/*
@Serializable
data class ClaudeChatRequest(...)
@Serializable
data class ClaudeMessage(...)
@Serializable
data class ClaudeTextContent(...)
@Serializable
data class ClaudeChatResponse(...)
*/

@Serializable
data class OpenRouterChatRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
    val temperature: Double = 0.7,
    @SerialName("max_tokens") val maxTokens: Int = 256,
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
