package com.pranav.punecityguide.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pranav.punecityguide.data.model.ConnectPost
import com.pranav.punecityguide.data.repository.PuneConnectRepository
import com.pranav.punecityguide.data.service.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConnectSavedUiState(
    val isLoading: Boolean = false,
    val posts: List<ConnectPost> = emptyList(),
    val error: String? = null
)

class ConnectSavedViewModel(
    private val repository: PuneConnectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectSavedUiState())
    val uiState: StateFlow<ConnectSavedUiState> = _uiState.asStateFlow()

    init {
        loadSavedPosts()
    }

    fun loadSavedPosts() {
        viewModelScope.launch {
            val userId = SupabaseClient.getSessionManager().getUserId()
            if (userId == null) {
                _uiState.value = _uiState.value.copy(error = "User not logged in")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val savedResult = repository.getSavedPosts(userId)
            
            savedResult.onSuccess { savedPosts ->
                if (savedPosts.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoading = false, posts = emptyList())
                    return@onSuccess
                }

                val postIds = savedPosts.map { it.postId }
                val postsResult = repository.getPostsByIds(postIds)
                
                postsResult.onSuccess { posts ->
                    _uiState.value = _uiState.value.copy(isLoading = false, posts = posts)
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to load post details: ${e.message}")
                }
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    companion object {
        fun factory(repository: PuneConnectRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ConnectSavedViewModel(repository) as T
                }
            }
        }
    }
}
