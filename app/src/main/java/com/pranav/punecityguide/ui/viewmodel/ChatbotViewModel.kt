package com.pranav.punecityguide.ui.viewmodel

import com.pranav.punecityguide.BuildConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pranav.punecityguide.data.service.RealtimeChatService
import com.pranav.punecityguide.data.service.SupabaseClient
import com.pranav.punecityguide.data.service.AiTokenQuotaService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: Long,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatbotUiState(
    val isLoadingContext: Boolean = false,
    val isConversationsLoading: Boolean = false,
    val currentConversationId: String? = null,
    val conversations: List<com.pranav.punecityguide.data.model.AiConversation> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val quickPrompts: List<String> = listOf(
        "What's the cheapest travel destination in Asia?",
        "Help me create a 10-day Japan budget",
        "Compare living costs: Bangkok vs Bali",
        "Give me 5 money-saving tips for Europe travel"
    ),
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    val remainingTokens: Int = 999,
    val tokenLimitReached: Boolean = false
)

class ChatbotViewModel(
    private val auditRepository: com.pranav.punecityguide.data.repository.SyncAuditRepository,
    private val tokenQuotaService: AiTokenQuotaService,
    private val aiChatRepository: com.pranav.punecityguide.data.repository.AiChatRepository,
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

    init {
        checkTokenQuota()
        loadConversations()
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

    private fun loadConversations() {
        _uiState.update { it.copy(isConversationsLoading = true) }
        viewModelScope.launch {
            aiChatRepository.getConversations(userId).onSuccess { list ->
                _uiState.update { it.copy(conversations = list, isConversationsLoading = false) }
            }.onFailure {
                _uiState.update { it.copy(isConversationsLoading = false) }
            }
        }
    }

    fun selectConversation(id: String) {
        _uiState.update { it.copy(currentConversationId = id, messages = emptyList(), isSending = true) }
        viewModelScope.launch {
            aiChatRepository.getMessages(id).onSuccess { messages ->
                val domainMessages = messages.map {
                    ChatMessage(
                        id = it.id.hashCode().toLong(),
                        text = it.content,
                        isUser = it.role == "user"
                    )
                }
                _uiState.update { it.copy(messages = domainMessages, isSending = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isSending = false, errorMessage = "Failed to load history: ${e.message}") }
            }
        }
    }

    fun sendMessage(input: String, onSuccess: () -> Unit = {}) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return
        if (_uiState.value.isSending) return

        val userMsgId = System.currentTimeMillis()
        val userMessage = ChatMessage(
            id = userMsgId,
            text = trimmed,
            isUser = true
        )

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage,
            isSending = true,
            errorMessage = null
        )

        viewModelScope.launch {
            var convId = _uiState.value.currentConversationId
            if (convId == null) {
                val title = if (trimmed.length > 30) trimmed.take(27) + "..." else trimmed
                val result = aiChatRepository.createConversation(userId, title)
                result.onSuccess { 
                    convId = it.id
                    _uiState.update { s -> s.copy(currentConversationId = it.id, conversations = listOf(it) + s.conversations) }
                }.onFailure { e ->
                    _uiState.update { it.copy(isSending = false, errorMessage = "Failed to start conversation: ${e.message}") }
                    return@launch
                }
            }

            aiChatRepository.saveMessage(convId!!, "user", trimmed)
            auditRepository.log("AI_QUERY_START", "Input: $trimmed", "Model: ${BuildConfig.CLAUDE_MODEL}")
            
            val remoteResult = realtimeChatService.getChatReply(
                history = _uiState.value.messages,
                userInput = trimmed
            )

            if (remoteResult.isFailure) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isSending = false,
                        errorMessage = "I'm having trouble connecting to CostPilot AI. Please check your internet or try again."
                    )
                }
                auditRepository.log("AI_QUERY_FAILURE", "Remote failed: ${remoteResult.exceptionOrNull()?.message}")
                return@launch
            }

            val botText = remoteResult.getOrThrow()
            aiChatRepository.saveMessage(convId!!, "assistant", botText)

            val botMessage = ChatMessage(
                id = System.currentTimeMillis() + 1,
                text = botText,
                isUser = false
            )

            _uiState.update { currentState ->
                currentState.copy(
                    messages = currentState.messages + botMessage,
                    isSending = false
                )
            }
            onSuccess()
            auditRepository.log("AI_QUERY_SUCCESS", "Response length: ${botText.length}")
        }
    }

    companion object {
        fun factory(
            auditRepository: com.pranav.punecityguide.data.repository.SyncAuditRepository,
            tokenQuotaService: AiTokenQuotaService,
            aiChatRepository: com.pranav.punecityguide.data.repository.AiChatRepository,
            userId: String
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChatbotViewModel(auditRepository, tokenQuotaService, aiChatRepository, userId) as T
                }
            }
        }
    }
}
