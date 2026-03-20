package com.pranav.punecityguide.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pranav.punecityguide.data.model.Attraction
import com.pranav.punecityguide.data.repository.AttractionRepository
import com.pranav.punecityguide.domain.secretspots.SecretSpotsEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class SecretSpotsUiState(
    val isLoading: Boolean = true,
    val items: List<Attraction> = emptyList(),
    val error: String? = null,
)

class SecretSpotsViewModel(
    private val attractionRepository: AttractionRepository,
    private val auditRepository: com.pranav.punecityguide.data.repository.SyncAuditRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SecretSpotsUiState())
    val uiState: StateFlow<SecretSpotsUiState> = _uiState.asStateFlow()

    fun load(favoriteIds: Set<Int> = emptySet(), recentlyViewedIds: Set<Int> = emptySet()) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val all = attractionRepository.getAllAttractions()
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val ranked = SecretSpotsEngine.rank(
                    attractions = all,
                    favoriteIds = favoriteIds,
                    recentlyViewedIds = recentlyViewedIds,
                    nowHour = hour,
                    limit = 24,
                )
                _uiState.value = SecretSpotsUiState(isLoading = false, items = ranked)
                auditRepository.log("RECOMMEND_SECRET", "Engine found ${ranked.size} spots", "Hour: $hour")
            } catch (e: Exception) {
                auditRepository.log("RECOMMEND_FAILURE", "Secret spots error: ${e.message}")
                _uiState.value = SecretSpotsUiState(isLoading = false, error = e.message ?: "Failed to load secret spots")
            }
        }
    }

    companion object {
        fun factory(
            attractionRepository: AttractionRepository,
            auditRepository: com.pranav.punecityguide.data.repository.SyncAuditRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SecretSpotsViewModel(attractionRepository, auditRepository) as T
                }
            }
        }
    }
}

