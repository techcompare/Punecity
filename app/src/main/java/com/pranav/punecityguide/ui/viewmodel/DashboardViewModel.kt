package com.pranav.punecityguide.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranav.punecityguide.data.model.CityCost
import com.pranav.punecityguide.data.model.CommunityMessage
import com.pranav.punecityguide.data.service.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class DashboardUiState(
    val trendingCities: List<CityCost> = emptyList(),
    val spotOfTheDay: CityCost? = null,
    val pulseData: PulseService.PulseData? = null,
    val latestCommunityMessage: CommunityMessage? = null,
    val streak: Int = 0,
    val missions: MissionsState = MissionsState(false, false, false),
    val isLoading: Boolean = true,
    val userName: String = "Traveler"
)

/**
 * ViewModel for the Dashboard Screen.
 * Orchestrates multiple data sources to provide a dynamic, engaging experience.
 * Strategy: Retention via streaks, missions, and live city pulses.
 */
class DashboardViewModel(
    private val serviceLocator: ServiceLocator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeStreakAndMissions()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 1. Fetch cities
            val result = serviceLocator.cityCostRepository.getAllCities()
            val allCities = result.getOrDefault(emptyList())

            // 2. Determine Spot of the Day
            val spotIndex = serviceLocator.preferenceManager.getSpotOfTheDay(allCities.size)
            val spot = allCities.getOrNull(spotIndex)

            // 3. Fetch Pulse for Spot
            val pulse = spot?.let {
                PulseService.fetchWeatherPulse(20.0, 70.0, Calendar.getInstance().get(Calendar.HOUR_OF_DAY), it.cityName)
            }

            // 4. Fetch latest community message
            val communityResult = serviceLocator.communityRepository.getRecentMessages(limit = 1)
            val latestMsg = communityResult.getOrDefault(emptyList()).firstOrNull()

            // 5. Get User Name
            val userId = serviceLocator.tokenSessionManager.getUserId()
            val profile = userId?.let { serviceLocator.userProfileRepository.getProfile(it).getOrNull() }

            _uiState.update { state ->
                state.copy(
                    trendingCities = allCities.shuffled().take(6),
                    spotOfTheDay = spot,
                    pulseData = pulse,
                    latestCommunityMessage = latestMsg,
                    userName = profile?.displayName ?: "Traveler",
                    isLoading = false
                )
            }
        }
    }

    private fun observeStreakAndMissions() {
        viewModelScope.launch {
            serviceLocator.preferenceManager.discoveryStreak.collect { streak ->
                _uiState.update { it.copy(streak = streak) }
            }
        }
        viewModelScope.launch {
            serviceLocator.preferenceManager.missionsState.collect { missions ->
                _uiState.update { it.copy(missions = missions) }
            }
        }
    }

    fun refresh() {
        loadData()
    }
}
