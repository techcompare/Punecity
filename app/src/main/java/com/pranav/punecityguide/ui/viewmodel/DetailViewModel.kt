package com.pranav.punecityguide.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pranav.punecityguide.data.model.Attraction
import com.pranav.punecityguide.data.model.RecentlyViewed
import com.pranav.punecityguide.data.repository.AttractionRepository
import com.pranav.punecityguide.data.repository.RecentlyViewedRepository
import com.pranav.punecityguide.data.repository.SyncAuditRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailUiState(
    val isLoading: Boolean = true,
    val attraction: Attraction? = null,
    val error: String? = null
)

class DetailViewModel(
    private val repository: AttractionRepository,
    private val recentlyViewedRepository: RecentlyViewedRepository,
    private val attractionId: Int,
    private val auditRepository: SyncAuditRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()
    
    init {
        loadAttraction()
    }
    
    private fun loadAttraction() {
        viewModelScope.launch {
            try {
                repository.observeAttractionById(attractionId).collect { attraction ->
                    _uiState.value = _uiState.value.copy(
                        attraction = attraction,
                        isLoading = false
                    )
                    if (attraction != null) {
                        launch {
                            auditRepository.log("DETAIL_VIEW", "User viewing ${attraction.name} (ID: $attractionId)")
                            recentlyViewedRepository.upsert(
                                RecentlyViewed(
                                    attractionId = attractionId,
                                    viewedAtEpochMillis = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Error loading attraction",
                    isLoading = false
                )
            }
        }
    }

    fun toggleFavorite() {
        val attraction = _uiState.value.attraction ?: return
        viewModelScope.launch {
            repository.updateFavoriteStatus(attraction.id, !attraction.isFavorite)
        }
    }
    
    companion object {
        fun factory(
            repository: AttractionRepository,
            recentlyViewedRepository: RecentlyViewedRepository,
            attractionId: Int,
            auditRepository: SyncAuditRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DetailViewModel(repository, recentlyViewedRepository, attractionId, auditRepository) as T
                }
            }
        }
    }
}
