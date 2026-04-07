package com.pranav.punecityguide.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pranav.punecityguide.data.model.Plan
import com.pranav.punecityguide.data.model.PlanPlace
import com.pranav.punecityguide.data.repository.PlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlanUiState(
    val isLoading: Boolean = false,
    val publicPlans: List<Plan> = emptyList(),
    val myPlans: List<Plan> = emptyList(),
    val currentPlan: Plan? = null,
    val currentPlaces: List<PlanPlace> = emptyList(),
    val error: String? = null
)

class PlanViewModel(
    private val repository: PlanRepository,
    private val userId: String
) : ViewModel() {
    private val _uiState = MutableStateFlow(PlanUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadPublicPlans()
        loadMyPlans()
    }

    fun loadPublicPlans() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repository.getPublicPlans().onSuccess { list ->
                _uiState.update { it.copy(publicPlans = list, isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun loadMyPlans() {
        if (userId == "user_default" || userId == "anon") return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repository.getMyPlans(userId).onSuccess { list ->
                _uiState.update { it.copy(myPlans = list, isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun selectPlan(plan: Plan) {
        _uiState.update { it.copy(currentPlan = plan, isLoading = true, currentPlaces = emptyList()) }
        viewModelScope.launch {
            repository.getPlanPlaces(plan.id).onSuccess { places ->
                _uiState.update { it.copy(currentPlaces = places, isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun createPlan(title: String, description: String?, duration: String?, isPublic: Boolean) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repository.createPlan(userId, title, description, duration, isPublic).onSuccess { plan ->
                loadMyPlans()
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun copyPlan(sourcePlanId: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repository.clonePlan(userId, sourcePlanId).onSuccess {
                loadMyPlans()
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun addPlace(planId: String, name: String, placeId: String?, order: Int) {
        viewModelScope.launch {
            repository.addPlaceToPlan(planId, name, placeId, order).onSuccess {
                if (_uiState.value.currentPlan?.id == planId) {
                    selectPlan(_uiState.value.currentPlan!!)
                }
            }
        }
    }

    companion object {
        fun factory(repository: PlanRepository, userId: String): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = PlanViewModel(repository, userId) as T
        }
    }
}
