package com.pranav.punecityguide.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pranav.punecityguide.data.model.ConnectPost
import com.pranav.punecityguide.data.repository.PuneConnectRepository
import com.pranav.punecityguide.data.service.RemoteAiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConnectHomeUiState(
    val isLoading: Boolean = false,
    val posts: List<ConnectPost> = emptyList(),
    val savedPostIds: Set<String> = emptySet(),
    val error: String? = null,
    val selectedArea: String? = null,
    val selectedSort: String = "latest",
    val dailyAiSpark: String = "Loading your daily Pune insight...",
    val availableAreas: List<String> = listOf("Baner", "Wakad", "Hinjewadi", "Kothrud", "Koregaon Park", "Viman Nagar")
)

class ConnectHomeViewModel(
    private val repository: PuneConnectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectHomeUiState())
    val uiState: StateFlow<ConnectHomeUiState> = _uiState.asStateFlow()

    private var currentOffset = 0
    private val PAGE_SIZE = 20
    private var isEndReached = false

    init {
        loadPosts()
        loadAiSpark()
        loadUserData()
        loadAreas() // NEW — Dynamic areas
        startPolling()
    }

    private fun loadAreas() {
        viewModelScope.launch {
            repository.getDistinctAreas().onSuccess { areas ->
                if (areas.isNotEmpty()) {
                    _uiState.update { it.copy(availableAreas = areas) }
                }
            }
        }
    }

    private fun loadUserData() {
        viewModelScope.launch {
            val userId = repository.getCurrentUserId() ?: return@launch
            
            // Load bookmarks
            repository.getSavedPostIds(userId).onSuccess { ids ->
                _uiState.update { it.copy(savedPostIds = ids.toSet()) }
            }
            
            // Note: In a full app, we'd also load userVotes here to show blue/red tinted icons
        }
    }

    private fun loadAiSpark() {
        viewModelScope.launch {
            val res = RemoteAiService.getChatResponse("Give me a one-sentence witty Pune tip or fact for today.")
            res.onSuccess { tip ->
                _uiState.value = _uiState.value.copy(dailyAiSpark = tip)
            }
            // Failure case leaves default message or could set a fallback
        }
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(30_000) // Poll every 30s
                // Only auto-refresh if we're at the top and not already loading
                if (!_uiState.value.isLoading) {
                    // This logic is usually triggered by UI scroll state (isAtTop)
                    // We'll expose a 'canAutoRefresh' property if needed, but for now 30s is safe
                }
            }
        }
    }
    
    fun toggleSave(postId: String) {
        val isSaved = _uiState.value.savedPostIds.contains(postId)
        val newSavedIds = if (isSaved) {
            _uiState.value.savedPostIds - postId
        } else {
            _uiState.value.savedPostIds + postId
        }
        
        _uiState.value = _uiState.value.copy(savedPostIds = newSavedIds)
        
        viewModelScope.launch {
            repository.toggleSavePost(postId, isSaved)
        }
    }

    fun refresh() {
        currentOffset = 0
        isEndReached = false
        loadPosts(isRefresh = true)
    }

    fun loadMore() {
        if (uiState.value.isLoading || isEndReached) return
        loadPosts(isRefresh = false)
    }

    private fun loadPosts(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }
            
            val offset = if (isRefresh) 0 else currentOffset
            
            val result = repository.getPosts(
                sortBy = _uiState.value.selectedSort,
                area = _uiState.value.selectedArea,
                limit = PAGE_SIZE,
                offset = offset
            )
            
            result.onSuccess { newPosts ->
                if (newPosts.size < PAGE_SIZE) {
                    isEndReached = true
                }
                
                val updatedList = if (isRefresh) {
                    newPosts
                } else {
                    _uiState.value.posts + newPosts
                }
                
                currentOffset = offset + newPosts.size
                _uiState.value = _uiState.value.copy(isLoading = false, posts = updatedList)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun setAreaFilter(area: String?) {
        _uiState.value = _uiState.value.copy(selectedArea = area)
        refresh()
    }

    fun setSortMode(sort: String) {
        _uiState.value = _uiState.value.copy(selectedSort = sort)
        refresh()
    }

    fun votePost(post: ConnectPost, voteType: Int) {
        // Simple optimistic score update
        val updatedPosts = _uiState.value.posts.map {
            if (it.id == post.id) {
                val scoreChange = voteType // Assuming score = up - down, this is simplified
                it.copy(upvotes = if(voteType > 0) it.upvotes + 1 else it.upvotes, 
                        downvotes = if(voteType < 0) it.downvotes + 1 else it.downvotes)
            } else it
        }
        _uiState.value = _uiState.value.copy(posts = updatedPosts)

        viewModelScope.launch {
            repository.votePost(post.id, voteType)
        }
    }

    companion object {
        fun factory(repository: PuneConnectRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ConnectHomeViewModel(repository) as T
                }
            }
        }
    }
}
