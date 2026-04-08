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
                // Show demo posts with helpful message
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        posts = getDemoPosts(),
                        error = "Viewing demo posts. Configure Supabase to enable real community features."
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
                        "No internet connection. Showing demo posts."
                    error.message?.contains("timeout", ignoreCase = true) == true ->
                        "Connection timed out. Showing demo posts."
                    else -> "Unable to load posts. Showing demo posts."
                }
                // Show demo posts as fallback
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        posts = getDemoPosts(),
                        error = userMessage,
                    )
                }
            }
        }
    }
    
    private fun getDemoPosts(): List<CommunityPost> {
        return listOf(
            CommunityPost(
                id = "demo1",
                author = "Pune Explorer",
                content = "Just discovered an amazing cafe in Koregaon Park! The vibe is incredible and the coffee is top-notch. Highly recommend! ☕",
                createdAt = "2 hours ago",
                likes = 24,
                isLiked = false,
                isSaved = false
            ),
            CommunityPost(
                id = "demo2",
                author = "Heritage Lover",
                content = "Visited Shaniwar Wada today. The history and architecture are breathtaking. A must-visit for anyone in Pune! 🏰",
                createdAt = "5 hours ago",
                location = "Shaniwar Wada",
                likes = 42,
                isLiked = false,
                isSaved = false
            ),
            CommunityPost(
                id = "demo3",
                author = "Foodie Punekar",
                content = "The street food at JM Road is unbeatable! Had the best vada pav and cutting chai. Nothing beats authentic Pune flavors! 🌮",
                createdAt = "1 day ago",
                location = "JM Road",
                likes = 67,
                isLiked = false,
                isSaved = false
            ),
            CommunityPost(
                id = "demo4",
                author = "Weekend Wanderer",
                content = "Sunrise trek to Sinhagad Fort was absolutely worth it! The view from the top is stunning. Perfect weekend activity! 🌄",
                createdAt = "2 days ago",
                location = "Sinhagad Fort",
                likes = 89,
                isLiked = false,
                isSaved = false
            ),
            CommunityPost(
                id = "demo5",
                author = "Art Enthusiast",
                content = "Spent the evening at Dagdusheth Ganpati Temple. The devotion and energy here are truly special. A spiritual experience! 🙏",
                createdAt = "3 days ago",
                location = "Dagdusheth Temple",
                likes = 56,
                isLiked = false,
                isSaved = false
            )
        )
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
