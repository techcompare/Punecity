package com.pranav.punecityguide.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranav.punecityguide.data.PuneRepository
import com.pranav.punecityguide.model.PuneSpot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoveryUiState(
    val isLoading: Boolean = true,
    val spots: List<PuneSpot> = emptyList(),
    val currentCategory: String = "All",
    val error: String? = null,
)

class DiscoveryViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DiscoveryUiState())
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val currentCat = _uiState.value.currentCategory
            val result = PuneRepository.getFeaturedSpots()
            result.onSuccess { allSpots ->
                val filtered = if (currentCat == "All") allSpots 
                               else allSpots.filter { it.category?.equals(currentCat, ignoreCase = true) == true }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        spots = filtered,
                        error = null,
                    )
                }
            }.onFailure { throwable ->
                // Show error message - no fallback to seeded data
                val userMessage = when {
                    throwable.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
                    throwable.message?.contains("Network", ignoreCase = true) == true ||
                    throwable.message?.contains("UnknownHost", ignoreCase = true) == true -> 
                        "No internet connection. Please connect to the internet to view places."
                    throwable.message?.contains("timeout", ignoreCase = true) == true ->
                        "Connection timed out. Please check your internet and try again."
                    else -> "Failed to load data. Please check your internet connection and try again."
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        spots = emptyList(),
                        error = userMessage,
                    )
                }
            }
        }
    }

    fun setCategory(category: String) {
        if (_uiState.value.currentCategory == category) return
        _uiState.update { it.copy(currentCategory = category) }
        refresh()
    }
}
