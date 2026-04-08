package com.pranav.punecityguide.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranav.punecityguide.data.CommunityRepository
import com.pranav.punecityguide.model.CommunityPost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class CommunityUiState(
    val isLoading: Boolean = true,
    val isPosting: Boolean = false,
    val posts: List<CommunityPost> = emptyList(),
    val error: String? = null,
    val info: String? = null,
)

class CommunityViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()

    init {
        refresh()
        startAutoRefresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, info = null) }
            
            // Check if Supabase is configured
            if (!com.pranav.punecityguide.data.SupabaseRest.isConfigured) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        posts = emptyList(),
                        error = "Community features require backend configuration."
                    )
                }
                return@launch
            }
            
            CommunityRepository.getRecentPosts().onSuccess { posts ->
                _uiState.update { current ->
                    current.copy(isLoading = false, posts = posts, error = null)
                }
            }.onFailure { error ->
                val userMessage = when {
                    error.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
                    error.message?.contains("Network", ignoreCase = true) == true ->
                        "No internet connection. Please check your connection and try again."
                    error.message?.contains("timeout", ignoreCase = true) == true ->
                        "Connection timed out. Please try again."
                    else -> "Unable to load posts. Pull down to refresh."
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        posts = emptyList(),
                        error = userMessage,
                    )
                }
            }
        }
    }

    fun createPost(author: String, content: String, userToken: String? = null) {
        val normalizedAuthor = author.trim().ifBlank { "Pune User" }
        val normalizedContent = content.trim()
        if (normalizedAuthor.length < 2) {
            _uiState.update { it.copy(error = "Author name should be at least 2 characters") }
            return
        }
        if (normalizedContent.length < 8) {
            _uiState.update { it.copy(error = "Post is too short. Add more detail.") }
            return
        }
        if (normalizedContent.length > 500) {
            _uiState.update { it.copy(error = "Post is too long. Keep it under 500 characters.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isPosting = true,
                    error = null,
                    info = null,
                )
            }

            CommunityRepository.createPost(
                author = normalizedAuthor, 
                content = normalizedContent,
                userToken = userToken
            )
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isPosting = false,
                            info = "Posted successfully",
                        )
                    }
                    refresh()
                }
                .onFailure { error ->
                    val userMessage = when {
                        error.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
                        error.message?.contains("Network", ignoreCase = true) == true ->
                            "No internet connection. Please try again."
                        error.message?.contains("timeout", ignoreCase = true) == true ->
                            "Connection timed out. Please try again."
                        error.message?.contains("401", ignoreCase = true) == true ||
                        error.message?.contains("403", ignoreCase = true) == true ->
                            "Please sign in again to post."
                        else -> "Unable to post. Please try again."
                    }
                    _uiState.update { state ->
                        state.copy(
                            isPosting = false,
                            error = userMessage,
                        )
                    }
                }
        }
    }

    fun updatePost(postId: String, newContent: String, userToken: String? = null) {
        val normalizedContent = newContent.trim()
        if (normalizedContent.length < 8) {
            _uiState.update { it.copy(error = "Post is too short. Add more detail.") }
            return
        }
        if (normalizedContent.length > 500) {
            _uiState.update { it.copy(error = "Post is too long. Keep it under 500 characters.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            CommunityRepository.updatePost(postId, normalizedContent, userToken)
                .onSuccess {
                    _uiState.update { it.copy(info = "Post updated successfully") }
                    refresh()
                }
                .onFailure { error ->
                    val userMessage = when {
                        error.message?.contains("Network", ignoreCase = true) == true ->
                            "No internet connection. Please try again."
                        else -> "Unable to update post. Please try again."
                    }
                    _uiState.update { it.copy(isLoading = false, error = userMessage) }
                }
        }
    }

    fun deletePost(postId: String, userToken: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            CommunityRepository.deletePost(postId, userToken)
                .onSuccess {
                    _uiState.update { it.copy(info = "Post deleted successfully") }
                    refresh()
                }
                .onFailure { error ->
                    val userMessage = when {
                        error.message?.contains("Network", ignoreCase = true) == true ->
                            "No internet connection. Please try again."
                        else -> "Unable to delete post. Please try again."
                    }
                    _uiState.update { it.copy(isLoading = false, error = userMessage) }
                }
        }
    }

    fun clearInfo() {
        _uiState.update { it.copy(info = null) }
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (isActive) {
                delay(45_000)
                if (!_uiState.value.isPosting) {
                    CommunityRepository.getRecentPosts().onSuccess { posts ->
                        _uiState.update { state ->
                            state.copy(posts = posts, error = null)
                        }
                    }
                }
            }
        }
    }
}
