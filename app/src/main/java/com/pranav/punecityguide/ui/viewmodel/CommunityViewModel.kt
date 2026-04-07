package com.pranav.punecityguide.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranav.punecityguide.data.model.CommunityMessage
import com.pranav.punecityguide.data.model.CommunityChannel
import com.pranav.punecityguide.data.repository.CommunityRepository
import com.pranav.punecityguide.data.service.ServiceLocator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class CommunityUiState(
    val channels: List<CommunityChannel> = emptyList(),
    val activeChannel: CommunityChannel? = null,
    val messages: List<CommunityMessage> = emptyList(),
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    val currentUserIdentity: UserIdentity? = null
)

data class UserIdentity(
    val id: String,
    val name: String,
    val isGuest: Boolean
)

/**
 * Startup-grade Community ViewModel.
 * Solves "users cant msg" by enabling robust Guest identities.
 * Features: Multi-channel discussions, reliable polling, and optimistic UI.
 */
class CommunityViewModel : ViewModel() {
    private val repository = ServiceLocator.communityRepository
    private val prefs = ServiceLocator.preferenceManager
    
    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()

    init {
        loadChannels()
        loadUserIdentity()
    }

    private fun loadChannels() {
        val channels = repository.getChannels()
        _uiState.update { it.copy(channels = channels, activeChannel = channels.firstOrNull()) }
        startPolling()
    }

    private fun loadUserIdentity() {
        viewModelScope.launch {
            val loggedInUserId = ServiceLocator.tokenSessionManager.getUserId()
            if (loggedInUserId != null) {
                val profile = ServiceLocator.userProfileRepository.getProfile(loggedInUserId).getOrNull()
                _uiState.update { it.copy(
                    currentUserIdentity = UserIdentity(
                        id = loggedInUserId,
                        name = profile?.displayName ?: "User",
                        isGuest = false
                    )
                )}
            } else {
                // Ensure guests can message too! (Fixes "users cant msg")
                _uiState.update { it.copy(
                    currentUserIdentity = UserIdentity(
                        id = prefs.getOrCreateGuestId(),
                        name = prefs.getOrCreateGuestName(),
                        isGuest = true
                    )
                )}
            }
        }
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (isActive) {
                val channelId = _uiState.value.activeChannel?.id ?: "global"
                repository.getRecentMessages(channel = channelId).onSuccess { remoteMsgs ->
                    val reversed = remoteMsgs.reversed()
                    _uiState.update { state ->
                        // Smart Merge: Keep optimistic messages that aren't yet in the remote list
                        val remoteIds = reversed.mapNotNull { it.id }.toSet()
                        val pendingOptimistic = state.messages.filter { it.id?.startsWith("opt_") == true && !remoteIds.contains(it.id) }
                        
                        // If we have remote data, use it as baseline + add pending optimistic ones at the end
                        val merged = (reversed + pendingOptimistic).sortedBy { it.createdAt ?: Long.MAX_VALUE.toString() }

                        state.copy(
                            messages = merged,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }.onFailure { e ->
                    if (_uiState.value.messages.isEmpty()) {
                        _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                    }
                }
                delay(3000) // Poll for new activity
            }
        }
    }

    fun selectChannel(channel: CommunityChannel) {
        if (_uiState.value.activeChannel?.id == channel.id) return
        _uiState.update { it.copy(activeChannel = channel, messages = emptyList(), isLoading = true) }
    }

    fun sendMessage(content: String) {
        val identity = _uiState.value.currentUserIdentity ?: return
        val channelId = _uiState.value.activeChannel?.id ?: "global"
        if (content.isBlank()) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            
            // 1. Post Optimistically with a temporary ID
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.US)
            val nowIso = sdf.format(java.util.Date())
            
            val tempId = "opt_${System.currentTimeMillis()}"
            val tempMsg = CommunityMessage(
                id = tempId,
                userId = identity.id, 
                userName = identity.name, 
                content = content, 
                channelId = channelId,
                createdAt = nowIso
            )
            _uiState.update { it.copy(messages = it.messages + tempMsg) }

            // 2. Actually post to Supabase
            val result = repository.postMessage(identity.id, identity.name, content, channelId)
            
            if (result.isSuccess) {
                ServiceLocator.preferenceManager.completeMission(com.pranav.punecityguide.data.service.MissionType.COMMUNITY)
                // The next poll will replace the optimistic message with the real one using the ID
                _uiState.update { it.copy(isSending = false, errorMessage = null) }
            } else {
                // Remove optimistic message on failure
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages.filter { it.id != tempId },
                        isSending = false, 
                        errorMessage = "Failed to sync. Tap to retry."
                    )
                }
            }
        }
    }

    fun reactToMessage(messageId: String, emoji: String) {
        viewModelScope.launch {
            repository.reactToMessage(messageId, emoji)
            // Ideally update local state count for instant feedback
        }
    }
}
