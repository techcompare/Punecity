package com.pranav.punecityguide.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pranav.punecityguide.data.SavedRepository
import com.pranav.punecityguide.model.SavedPlace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SavedUiState(
    val isLoading: Boolean = true,
    val places: List<SavedPlace> = emptyList(),
    val error: String? = null,
)

/**
 * Reactive ViewModel for Saved Places.
 * Uses Room Flow to ensure the UI updates instantly when a spot is saved/unsaved.
 */
class SavedViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(SavedUiState())
    val uiState: StateFlow<SavedUiState> = _uiState.asStateFlow()

    init {
        observeSavedPlaces()
    }

    private fun observeSavedPlaces() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val context = getApplication<Application>().applicationContext
            
            SavedRepository.getSavedPlacesFlow(context).collect { places ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        places = places,
                        error = null,
                    )
                }
            }
        }
    }

    fun refresh() {
        // No-op for Flow-based repo, but we can reset loading state for visual feedback
        _uiState.update { it.copy(isLoading = true) }
    }

    fun removePlace(place: SavedPlace) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            SavedRepository.removeSavedPlace(context, place.id)
        }
    }
}
