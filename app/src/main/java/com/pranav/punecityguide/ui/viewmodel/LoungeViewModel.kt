package com.pranav.punecityguide.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pranav.punecityguide.data.model.GlobalChatMessage
import com.pranav.punecityguide.data.repository.PuneConnectRepository
import com.pranav.punecityguide.data.service.SupabaseClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoungeUiState(
    val messages: List<GlobalChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null
)

class LoungeViewModel(private val repository: PuneConnectRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoungeUiState())
    val uiState = _uiState.asStateFlow()

    private var isRefreshingLoopRunning = false

    init {
        startLoungePolling()
    }

    private fun startLoungePolling() {
        if (isRefreshingLoopRunning) return
        isRefreshingLoopRunning = true
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            while (true) {
                fetchMessages()
                _uiState.update { it.copy(isLoading = false) }
                delay(8000) // Poll every 8 seconds for real-time feel
            }
        }
    }

    suspend fun fetchMessages() {
        val result = repository.getLoungeMessages()
        result.onSuccess { msgs ->
            _uiState.update { it.copy(messages = msgs, error = null) }
        }.onFailure { e ->
            // Silent fail for polling, only show error if initial fetch fails
            if (_uiState.value.messages.isEmpty()) {
                _uiState.update { it.copy(error = "Lounge disconnected: ${e.message}") }
            }
        }
    }

    fun sendMessage(text: String, userName: String) {
        if (text.isBlank()) return
        
        viewModelScope.launch {
            val userId = SupabaseClient.getSessionManager().getUserId() ?: "anon"
            
            _uiState.update { it.copy(isSending = true) }
            
            val result = repository.sendLoungeMessage(userId, userName, text)
            result.onSuccess { newMsg ->
                _uiState.update { it.copy(
                    messages = it.messages + newMsg,
                    isSending = false,
                    error = null
                ) }
            }.onFailure { e ->
                _uiState.update { it.copy(isSending = false, error = "Failed to send: ${e.message}") }
            }
        }
    }

    companion object {
        fun factory(repository: PuneConnectRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LoungeViewModel(repository) as T
                }
            }
        }
    }
}
