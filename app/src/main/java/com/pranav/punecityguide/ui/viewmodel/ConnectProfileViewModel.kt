package com.pranav.punecityguide.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pranav.punecityguide.data.model.ConnectPost
import com.pranav.punecityguide.data.model.ConnectUser
import com.pranav.punecityguide.data.repository.PuneConnectRepository
import com.pranav.punecityguide.data.service.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConnectProfileUiState(
    val isLoading: Boolean = false,
    val user: ConnectUser? = null,
    val userPosts: List<ConnectPost> = emptyList(),
    val points: Int = 0,
    val error: String? = null
)

class ConnectProfileViewModel(
    private val repository: PuneConnectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectProfileUiState())
    val uiState: StateFlow<ConnectProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            val userId = SupabaseClient.getSessionManager().getUserId()
            if (userId == null) {
                _uiState.value = _uiState.value.copy(error = "User not logged in")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // 1. Fetch User Profile
            val profileResult = repository.getUserProfile(userId)
            profileResult.onSuccess { user ->
                 _uiState.value = _uiState.value.copy(user = user)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = "Profile load failed: ${e.message}")
            }

            // 2. Fetch User Posts to calculate points
            val postsResult = repository.getPostsByUserId(userId)
            postsResult.onSuccess { posts ->
                val points = (posts.size * 10) + (posts.sumOf { it.upvotes } * 2)
                _uiState.value = _uiState.value.copy(
                    userPosts = posts,
                    points = points,
                    isLoading = false
                )
            }.onFailure { e ->
                 // Even if posts fail, we might want to show user profile if loaded
                 _uiState.value = _uiState.value.copy(
                     isLoading = false,
                     error = "Failed to load posts: ${e.message}"
                 )
            }
        }
    }

    companion object {
        fun factory(repository: PuneConnectRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ConnectProfileViewModel(repository) as T
                }
            }
        }
    }
}
