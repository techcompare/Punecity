package com.pranav.punecityguide.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pranav.punecityguide.data.model.ConnectPost
import com.pranav.punecityguide.data.model.ConnectUser
import com.pranav.punecityguide.data.model.Attraction
import com.pranav.punecityguide.data.repository.PuneConnectRepository
import com.pranav.punecityguide.data.repository.AttractionRepository
import com.pranav.punecityguide.data.service.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ConnectProfileUiState(
    val isLoading: Boolean = false,
    val user: ConnectUser? = null,
    val userPosts: List<ConnectPost> = emptyList(),
    val favorites: List<Attraction> = emptyList(),
    val points: Int = 0,
    val passportLevel: Int = 1,
    val discoveryStreak: Int = 0,
    val error: String? = null
)

class ConnectProfileViewModel(
    private val communityRepository: PuneConnectRepository,
    private val attractionRepository: AttractionRepository
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
            val profileResult = communityRepository.getUserProfile(userId)
            profileResult.onSuccess { user ->
                 _uiState.value = _uiState.value.copy(user = user)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = "Profile load failed: ${e.message}")
            }

            // 2. Fetch User Posts to calculate points
            val postsResult = communityRepository.getPostsByUserId(userId)
            postsResult.onSuccess { posts ->
                val communityPoints = (posts.size * 10) + (posts.sumOf { it.upvotes } * 2)
                
                // 3. Fetch Local Exploration Stats (V6 Version 6 Passport)
                val favorites = attractionRepository.getFavoriteAttractions().first()
                val viewedCount = attractionRepository.getRecentlyViewedCount().first()
                
                // V6 Gamification logic
                val passportPoints = (favorites.size * 25) + (viewedCount * 5)
                val totalPoints = communityPoints + passportPoints
                val level = (totalPoints / 100).coerceAtLeast(1)
                
                _uiState.value = _uiState.value.copy(
                    userPosts = posts,
                    favorites = favorites.take(5), // Show top 5 recent favorites
                    points = totalPoints,
                    passportLevel = level,
                    discoveryStreak = (viewedCount / 2).coerceAtMost(7), // Simple mock streak or use real logic
                    isLoading = false
                )
            }.onFailure { e ->
                 _uiState.value = _uiState.value.copy(
                     isLoading = false,
                     error = "Failed to load posts: ${e.message}"
                 )
            }
        }
    }

    companion object {
        fun factory(
            communityRepository: PuneConnectRepository,
            attractionRepository: AttractionRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ConnectProfileViewModel(communityRepository, attractionRepository) as T
                }
            }
        }
    }
}
