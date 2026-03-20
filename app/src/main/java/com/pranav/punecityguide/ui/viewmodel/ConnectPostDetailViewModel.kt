package com.pranav.punecityguide.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pranav.punecityguide.data.model.ConnectComment
import com.pranav.punecityguide.data.model.ConnectPost
import com.pranav.punecityguide.data.repository.PuneConnectRepository
import com.pranav.punecityguide.data.service.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ConnectDetailUiState(
    val isLoading: Boolean = false,
    val post: ConnectPost? = null,
    val comments: List<ConnectComment> = emptyList(),
    val error: String? = null,
    val isSavingComment: Boolean = false
)

class ConnectPostDetailViewModel(
    private val repository: PuneConnectRepository,
    private val postId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectDetailUiState())
    val uiState: StateFlow<ConnectDetailUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // Fetch post
            val postResult = repository.getPostById(postId)
            postResult.onSuccess { post ->
                _uiState.value = _uiState.value.copy(post = post)
                
                // Fetch comments
                val commentsResult = repository.getComments(postId)
                commentsResult.onSuccess { comments ->
                    _uiState.value = _uiState.value.copy(isLoading = false, comments = comments)
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Comments failed: ${e.message}")
                }
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Post failed: ${e.message}")
            }
        }
    }

    fun addComment(content: String, onSuccess: () -> Unit) {
        if (content.isBlank()) return
        
        viewModelScope.launch {
            val userId = SupabaseClient.getSessionManager().getUserId() ?: run {
                _uiState.value = _uiState.value.copy(error = "User not logged in", isSavingComment = false)
                return@launch
            }
            _uiState.value = _uiState.value.copy(isSavingComment = true)
            
            val newComment = ConnectComment(
                id = UUID.randomUUID().toString(),
                postId = postId,
                userId = userId,
                text = content,
                createdAt = null // Let Supabase handle the timestamp
            )
            
            val result = repository.addComment(newComment)
            result.onSuccess { created ->
                _uiState.update { currentState ->
                    currentState.copy(
                        isSavingComment = false,
                        comments = currentState.comments + created
                    )
                }
                onSuccess()
            }.onFailure { e ->
                _uiState.update { currentState ->
                    currentState.copy(
                        isSavingComment = false,
                        error = "Failed to add comment: ${e.message}"
                    )
                }
            }
        }
    }

    companion object {
        fun factory(repository: PuneConnectRepository, postId: String): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ConnectPostDetailViewModel(repository, postId) as T
                }
            }
        }
    }
}
