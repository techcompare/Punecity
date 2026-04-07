package com.pranav.punecityguide.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranav.punecityguide.data.model.*
import com.pranav.punecityguide.data.repository.CityCostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ComparatorUiState(
    val allCities: List<CityCost> = emptyList(),
    val searchQuery1: String = "",
    val searchQuery2: String = "",
    val filteredCities1: List<CityCost> = emptyList(),
    val filteredCities2: List<CityCost> = emptyList(),
    val selectedCity1: CityCost? = null,
    val selectedCity2: CityCost? = null,
    val comparison: CityComparison? = null,
    val isSearching1: Boolean = false,
    val isSearching2: Boolean = false,
    val isInitialLoading: Boolean = true,
    // Trip budget
    val tripDays: String = "7",
    val travelStyle: TravelStyle = TravelStyle.MID_RANGE,
    val groupSize: Int = 1,
    val tripBudget: TripBudget? = null,
    val selectedTripCity: CityCost? = null,
    val showTripSheet: Boolean = false,
    val errorMessage: String? = null
)

class ComparatorViewModel : ViewModel() {
    private val repository = com.pranav.punecityguide.data.service.ServiceLocator.cityCostRepository
    private val _uiState = MutableStateFlow(ComparatorUiState())
    val uiState: StateFlow<ComparatorUiState> = _uiState.asStateFlow()

    init {
        loadAllCities()
    }

    private fun loadAllCities() {
        viewModelScope.launch {
            repository.getAllCities().onSuccess { result ->
                _uiState.update { it.copy(allCities = result, isInitialLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isInitialLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun onSearchCity1(query: String) {
        _uiState.update { it.copy(searchQuery1 = query, isSearching1 = query.isNotEmpty()) }
        if (query.length >= 2) {
            viewModelScope.launch {
                repository.searchCities(query).onSuccess { res ->
                    _uiState.update { state ->
                        if (state.searchQuery1 == query) state.copy(filteredCities1 = res) else state
                    }
                }
            }
        } else {
            _uiState.update { it.copy(filteredCities1 = emptyList()) }
        }
    }

    fun onSearchCity2(query: String) {
        _uiState.update { it.copy(searchQuery2 = query, isSearching2 = query.isNotEmpty()) }
        if (query.length >= 2) {
            viewModelScope.launch {
                repository.searchCities(query).onSuccess { res ->
                    _uiState.update { state ->
                        if (state.searchQuery2 == query) state.copy(filteredCities2 = res) else state
                    }
                }
            }
        } else {
            _uiState.update { it.copy(filteredCities2 = emptyList()) }
        }
    }

    fun selectCity1(city: CityCost) {
        _uiState.update {
            val hasBoth = it.selectedCity2 != null
            if (hasBoth) {
                viewModelScope.launch { com.pranav.punecityguide.data.service.ServiceLocator.preferenceManager.completeMission(com.pranav.punecityguide.data.service.MissionType.COMPARE) }
            }
            it.copy(
                selectedCity1 = city,
                searchQuery1 = "${city.cityName}, ${city.country}",
                isSearching1 = false,
                filteredCities1 = emptyList(),
                comparison = if (it.selectedCity2 != null) repository.compareCities(city, it.selectedCity2) else null
            )
        }
    }

    fun selectCity2(city: CityCost) {
        _uiState.update {
            val hasBoth = it.selectedCity1 != null
            if (hasBoth) {
                viewModelScope.launch { com.pranav.punecityguide.data.service.ServiceLocator.preferenceManager.completeMission(com.pranav.punecityguide.data.service.MissionType.COMPARE) }
            }
            it.copy(
                selectedCity2 = city,
                searchQuery2 = "${city.cityName}, ${city.country}",
                isSearching2 = false,
                filteredCities2 = emptyList(),
                comparison = if (it.selectedCity1 != null) repository.compareCities(it.selectedCity1, city) else null
            )
        }
    }

    fun swapCities() {
        _uiState.update {
            val newComparison = if (it.selectedCity2 != null && it.selectedCity1 != null) {
                repository.compareCities(it.selectedCity2, it.selectedCity1)
            } else null
            it.copy(
                selectedCity1 = it.selectedCity2,
                selectedCity2 = it.selectedCity1,
                searchQuery1 = it.searchQuery2,
                searchQuery2 = it.searchQuery1,
                comparison = newComparison
            )
        }
    }

    // ── Trip Budget ──
    fun showTripBudget(city: CityCost) {
        _uiState.update { it.copy(selectedTripCity = city, showTripSheet = true) }
        generateBudget()
    }

    fun hideTripSheet() {
        _uiState.update { it.copy(showTripSheet = false) }
    }

    fun onTripDaysChange(days: String) {
        _uiState.update { it.copy(tripDays = days) }
        generateBudget()
    }

    fun onTravelStyleChange(style: TravelStyle) {
        _uiState.update { it.copy(travelStyle = style) }
        generateBudget()
    }

    fun onGroupSizeChange(size: Int) {
        _uiState.update { it.copy(groupSize = size.coerceIn(1, 10)) }
        generateBudget()
    }

    private fun generateBudget() {
        val city = _uiState.value.selectedTripCity ?: return
        val days = _uiState.value.tripDays.toIntOrNull() ?: return
        if (days <= 0) return
        val budget = repository.generateTripBudget(
            city = city,
            days = days,
            style = _uiState.value.travelStyle,
            groupSize = _uiState.value.groupSize
        )
        _uiState.update { it.copy(tripBudget = budget) }
    }

    fun clearSearch1() {
        _uiState.update {
            it.copy(searchQuery1 = "", selectedCity1 = null, filteredCities1 = emptyList(), isSearching1 = false, comparison = null)
        }
    }

    fun clearSearch2() {
        _uiState.update {
            it.copy(searchQuery2 = "", selectedCity2 = null, filteredCities2 = emptyList(), isSearching2 = false, comparison = null)
        }
    }

    fun getPopularComparisons(): List<Pair<CityCost, CityCost>> {
        val cities = _uiState.value.allCities
        val pairs = listOf(
            "Bangkok" to "Bali",
            "Tokyo" to "Seoul",
            "London" to "Paris",
            "New York" to "San Francisco"
        )
        return pairs.mapNotNull { (a, b) ->
            val c1 = cities.find { it.cityName == a }
            val c2 = cities.find { it.cityName == b }
            if (c1 != null && c2 != null) c1 to c2 else null
        }
    }
}
