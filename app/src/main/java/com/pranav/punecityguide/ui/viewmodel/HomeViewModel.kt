package com.pranav.punecityguide.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pranav.punecityguide.data.model.Attraction
import com.pranav.punecityguide.data.repository.AttractionRepository
import com.pranav.punecityguide.data.repository.SyncAuditRepository
import com.pranav.punecityguide.data.service.BackendHealthService
import kotlinx.coroutines.Dispatchers
import com.pranav.punecityguide.data.service.NetworkResilience
import com.pranav.punecityguide.data.service.PreferenceManager
import com.pranav.punecityguide.domain.attraction.GetAttractionsUseCase
import com.pranav.punecityguide.domain.attraction.SyncAttractionsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PassportProgress(
    val levelName: String = "New Explorer",
    val collectedPlaces: Int = 0,
    val unlockedCategories: Int = 0,
    val totalCategories: Int = 0,
    val badges: List<String> = emptyList(),
    val nextLevelTarget: Int = 3,
    val progressToNext: Float = 0f,
    val todaysQuest: String = "Add 1 place to favorites to begin your Pune Passport."
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val topAttractions: List<Attraction> = emptyList(),
    val favorites: List<Attraction> = emptyList(),
    val categories: List<String> = emptyList(),
    val passport: PassportProgress = PassportProgress(),
    val healthStatus: String = "HEALTHY",
    val discoveryStreak: Int = 0,
    val showOnboarding: Boolean = false,
    val spotOfTheDay: Attraction? = null,
    val isSpotRevealed: Boolean = false,
    val error: String? = null
)

class HomeViewModel(
    private val application: Application,
    private val getAttractionsUseCase: GetAttractionsUseCase,
    private val syncAttractionsUseCase: SyncAttractionsUseCase,
    private val repository: AttractionRepository,
    private val healthService: BackendHealthService,
    private val prefManager: PreferenceManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private var initJob: Job? = null

    init {
        checkOnboarding()
        loadInitialData()
        loadFavorites()
        loadCategories()
        checkBackendHealth()
        updateStreak()
    }

    private fun checkOnboarding() {
        viewModelScope.launch {
            prefManager.onboardingCompleted.collect { completed ->
                _uiState.value = _uiState.value.copy(showOnboarding = !completed)
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            prefManager.setOnboardingCompleted(true)
        }
    }

    private fun updateStreak() {
        viewModelScope.launch {
            prefManager.updateStreak()
            prefManager.discoveryStreak.collect { streak ->
                _uiState.value = _uiState.value.copy(discoveryStreak = streak)
            }
        }
    }
    
    private fun loadInitialData() {
        initJob?.cancel()
        initJob = viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // Pre-populate with hardcoded real data (Version 6 style)
                launch(Dispatchers.IO) {
                    val initialAttractions = com.pranav.punecityguide.data.AttractionData.getHardcodedAttractions()
                    repository.upsertAllAttractions(initialAttractions)
                }

                // 1. Immediate load from local DB (domain layer)
                launch {
                    getAttractionsUseCase().collect { attractions ->
                        if (attractions.isNotEmpty()) {
                            _uiState.value = _uiState.value.copy(
                                topAttractions = attractions,
                                isLoading = false
                            )
                        }
                    }
                }

                // 2. Perform sync via specialized UseCase
                val syncResult = syncAttractionsUseCase()
                
                if (syncResult.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        error = NetworkResilience.classifyError(syncResult.exceptionOrNull()!!),
                        isLoading = false
                    )
                } else {
                    val liveCount = repository.getAttractionCount()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = if (liveCount == 0) "No live attractions available yet. Pull to refresh." else null
                    )

                    // 3. Handle Spot of the Day
                    launch {
                        if (liveCount > 0) {
                            val index = prefManager.getSpotOfTheDay(liveCount)
                            repository.getTopAttractions(liveCount).take(1).collect { all ->
                                if (all.indices.contains(index)) {
                                    _uiState.value = _uiState.value.copy(spotOfTheDay = all[index])
                                }
                            }
                        } else {
                            _uiState.value = _uiState.value.copy(spotOfTheDay = null)
                        }
                    }
                }
                
                // 4. Track Reveal Status
                launch {
                    repository.getRecentlyViewedCount().collect { count ->
                        val level = when {
                            count >= 20 -> "Pune Pro"
                            count >= 10 -> "Local Hero"
                            else -> "Explorer"
                        }
                        val nextLevel = when {
                            count < 10 -> 10
                            count < 20 -> 20
                            else -> 50
                        }
                        _uiState.update { it.copy(
                            passport = PassportProgress(
                                collectedPlaces = count,
                                levelName = level,
                                nextLevelTarget = nextLevel,
                                progressToNext = (count.toFloat() / nextLevel).coerceIn(0f, 1f)
                            )
                        )}
                    }
                }
                launch {
                    prefManager.isSpotRevealed.collect { revealed ->
                        _uiState.value = _uiState.value.copy(isSpotRevealed = revealed)
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading initial data", e)
                _uiState.value = _uiState.value.copy(
                    error = com.pranav.punecityguide.util.ErrorMapper.map(e),
                    isLoading = false
                )
            }
        }
    }

    private fun checkBackendHealth() {
        viewModelScope.launch {
            try {
                val report = healthService.runFullHealthCheck()
                _uiState.value = _uiState.value.copy(healthStatus = report.overallStatus)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(healthStatus = "UNKNOWN")
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            NetworkResilience.resetAllCircuits()
            checkBackendHealth()
            loadInitialData()
        }
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            repository.getFavoriteAttractions().collect { favorites ->
                _uiState.value = _uiState.value.copy(
                    favorites = favorites,
                    passport = buildPassport(
                        favorites = favorites,
                        categories = _uiState.value.categories
                    )
                )
            }
        }
    }

    fun toggleFavorite(attraction: Attraction) {
        viewModelScope.launch {
            repository.updateFavoriteStatus(attraction.id, !attraction.isFavorite)
        }
    }

    fun revealSpot() {
        viewModelScope.launch {
            prefManager.markSpotAsRevealed()
        }
    }
    
    fun loadCategories() {
        viewModelScope.launch {
            try {
                repository.getAllCategories().collect { categories ->
                    _uiState.value = _uiState.value.copy(
                        categories = categories,
                        passport = buildPassport(
                            favorites = _uiState.value.favorites,
                            categories = categories
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Error loading categories"
                )
            }
        }
    }

    private fun buildPassport(favorites: List<Attraction>, categories: List<String>): PassportProgress {
        return com.pranav.punecityguide.util.PassportUtil.buildPassport(favorites, categories)
    }
    
    companion object {
        fun factory(
            application: Application,
            repository: AttractionRepository,
            auditRepository: SyncAuditRepository,
            healthService: BackendHealthService,
            prefManager: PreferenceManager
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val getAttractionsUseCase = GetAttractionsUseCase(repository)
                    val syncAttractionsUseCase = SyncAttractionsUseCase(repository, auditRepository)
                    return HomeViewModel(
                        application, 
                        getAttractionsUseCase, 
                        syncAttractionsUseCase, 
                        repository, 
                        healthService, 
                        prefManager
                    ) as T
                }
            }
        }
    }
}
