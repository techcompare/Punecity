package com.pranav.punecityguide.ui.viewmodel

import com.pranav.punecityguide.BuildConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pranav.punecityguide.data.model.Attraction
import com.pranav.punecityguide.data.repository.AttractionRepository
import com.pranav.punecityguide.data.service.LocalAiGuideService
import com.pranav.punecityguide.data.service.RealtimeChatService
import com.pranav.punecityguide.data.service.SupabaseClient
import com.pranav.punecityguide.data.service.AiTokenQuotaService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: Long,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatbotUiState(
    val isLoadingContext: Boolean = true,
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            id = 1L,
            text = "Hi, I am your Pune AI Guide. Ask for plans, budget places, family trips, or food trails.",
            isUser = false
        )
    ),
    val quickPrompts: List<String> = listOf(
        "Plan 4 hours under Rs 800 for family",
        "Show hidden gems with less crowd",
        "Build a monsoon-safe itinerary",
        "Surprise me with a unique Pune trail"
    ),
    val recommendations: List<Attraction> = emptyList(),
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    val remainingTokens: Int = 10,
    val tokenLimitReached: Boolean = false
)

class ChatbotViewModel(
    private val repository: AttractionRepository,
    private val auditRepository: com.pranav.punecityguide.data.repository.SyncAuditRepository,
    private val tokenQuotaService: AiTokenQuotaService,
    private val userId: String
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatbotUiState())
    val uiState: StateFlow<ChatbotUiState> = _uiState.asStateFlow()
    private val realtimeChatService by lazy {
        RealtimeChatService(
            httpClient = SupabaseClient.getHttpClient(),
            apiKey = BuildConfig.CLAUDE_API_KEY,
            model = BuildConfig.CLAUDE_MODEL
        )
    }

    private var cachedAttractions: List<Attraction> = emptyList()
    private var cachedCategories: List<String> = emptyList()

    init {
        loadContext()
        checkTokenQuota()
    }

    private fun checkTokenQuota() {
        viewModelScope.launch {
            val remaining = tokenQuotaService.getRemainingTokens(userId)
            _uiState.value = _uiState.value.copy(
                remainingTokens = remaining,
                tokenLimitReached = remaining <= 0
            )
        }
    }

    private fun loadContext() {
        viewModelScope.launch {
            val attractions = repository.getTopAttractions(50).first()
            val categories = repository.getAllCategories().first()
            cachedAttractions = attractions
            cachedCategories = categories
            _uiState.value = _uiState.value.copy(isLoadingContext = false)
        }
    }

    fun sendMessage(input: String, onSuccess: () -> Unit = {}) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return
        if (_uiState.value.isSending) return
        // Token limit unrestricted

        val userMessage = ChatMessage(
            id = System.currentTimeMillis(),
            text = trimmed,
            isUser = true
        )

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage,
            isSending = true,
            errorMessage = null
        )

        viewModelScope.launch {
            // No token consumption
            val remaining = 999 
            
            auditRepository.log("AI_QUERY_START", "Input: $trimmed", "Model: ${BuildConfig.CLAUDE_MODEL}")
            val localResponse = LocalAiGuideService.buildResponse(
                query = trimmed,
                attractions = cachedAttractions,
                categories = cachedCategories
            )

            val remoteResult = realtimeChatService.getChatReply(
                history = _uiState.value.messages,
                userInput = trimmed,
                attractionsContext = cachedAttractions
            )

            val botText = remoteResult.getOrElse {
                localResponse.reply
            }
            val recommended = if (remoteResult.isSuccess) {
                extractMentionedAttractions(botText, cachedAttractions)
                    .ifEmpty { LocalAiGuideService.recommendAttractions(trimmed, cachedAttractions, limit = 5) }
            } else {
                localResponse.recommendations.ifEmpty {
                    LocalAiGuideService.recommendAttractions(trimmed, cachedAttractions, limit = 5)
                }
            }

            val botMessage = ChatMessage(
                id = System.currentTimeMillis() + 1,
                text = botText,
                isUser = false
            )

            _uiState.update { currentState ->
                currentState.copy(
                    messages = currentState.messages + botMessage,
                    recommendations = recommended,
                    isSending = false,
                    remainingTokens = remaining,
                    tokenLimitReached = remaining <= 0,
                    errorMessage = remoteResult.exceptionOrNull()?.message
                )
            }
            onSuccess()

            if (remoteResult.isSuccess) {
                auditRepository.log("AI_QUERY_SUCCESS", "Response length: ${botText.length}")
            } else {
                auditRepository.log("AI_QUERY_FAILURE", "Error: ${remoteResult.exceptionOrNull()?.message}")
            }
        }
    }

    /** Scans the AI reply text for names of known attractions and returns them in mention order. */
    private fun extractMentionedAttractions(text: String, attractions: List<Attraction>): List<Attraction> {
        val lower = text.lowercase()
        return attractions
            .filter { lower.contains(it.name.lowercase()) }
            .sortedByDescending { lower.indexOf(it.name.lowercase()) == -1 }
            .take(5)
    }

    companion object {
        fun factory(
            repository: AttractionRepository,
            auditRepository: com.pranav.punecityguide.data.repository.SyncAuditRepository,
            tokenQuotaService: AiTokenQuotaService,
            userId: String
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChatbotViewModel(repository, auditRepository, tokenQuotaService, userId) as T
                }
            }
        }
    }
}
