package com.pranav.punecityguide.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pranav.punecityguide.model.CuratedPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlansUiState(
    val isLoading: Boolean = false,
    val plans: List<CuratedPlan> = emptyList(),
    val selectedPlan: CuratedPlan? = null,
    val error: String? = null,
)

class PlansViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(PlansUiState())
    val uiState: StateFlow<PlansUiState> = _uiState.asStateFlow()
    
    init {
        loadPlans()
    }
    
    private fun loadPlans() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Curated plans with realistic Pune data
            val curatedPlans = listOf(
                CuratedPlan(
                    id = "cafe-hopping",
                    title = "Cafe Hopping Trail",
                    description = "Explore the best cafes in Koregaon Park and FC Road. Perfect for a relaxed afternoon with friends.",
                    category = "cafe",
                    durationHours = 4,
                    estimatedCost = 800,
                    tags = listOf("Cafes", "Coffee", "Desserts", "Instagram"),
                    imageUrl = "https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=800"
                ),
                CuratedPlan(
                    id = "weekend-trek",
                    title = "Sinhagad Fort Trek",
                    description = "A classic weekend trek to the historic Sinhagad Fort. Best attempted early morning for cooler weather.",
                    category = "nature",
                    durationHours = 6,
                    estimatedCost = 500,
                    tags = listOf("Trekking", "History", "Nature", "Fitness"),
                    imageUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800"
                ),
                CuratedPlan(
                    id = "heritage-walk",
                    title = "Old Pune Heritage Walk",
                    description = "Walk through the historic Peth areas. Visit Shaniwar Wada, Lal Mahal, and traditional markets.",
                    category = "heritage",
                    durationHours = 3,
                    estimatedCost = 300,
                    tags = listOf("History", "Architecture", "Walking", "Culture"),
                    imageUrl = "https://images.unsplash.com/photo-1587474260584-136574528ed5?w=800"
                ),
                CuratedPlan(
                    id = "food-trail",
                    title = "Puneri Food Trail",
                    description = "Taste authentic Puneri cuisine from Misal Pav to Mastani. A food lover's delight across the city.",
                    category = "food",
                    durationHours = 5,
                    estimatedCost = 600,
                    tags = listOf("Food", "Local Cuisine", "Street Food", "Authentic"),
                    imageUrl = "https://images.unsplash.com/photo-1567337710282-00832b415979?w=800"
                ),
                CuratedPlan(
                    id = "sunset-ride",
                    title = "Lavasa Sunset Drive",
                    description = "A scenic drive to Lavasa for stunning sunset views. Perfect for couples or solo adventurers.",
                    category = "nature",
                    durationHours = 5,
                    estimatedCost = 1200,
                    tags = listOf("Scenic", "Drive", "Sunset", "Photography"),
                    imageUrl = "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800"
                ),
                CuratedPlan(
                    id = "art-culture",
                    title = "Art & Culture Day",
                    description = "Visit Raja Dinkar Kelkar Museum, explore galleries in Koregaon Park, and catch a play at Bal Gandharva.",
                    category = "heritage",
                    durationHours = 6,
                    estimatedCost = 700,
                    tags = listOf("Art", "Culture", "Museums", "Theatre"),
                    imageUrl = "https://images.unsplash.com/photo-1536924940846-227afb31e2a5?w=800"
                ),
            )
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                plans = curatedPlans,
                error = null
            )
        }
    }
    
    fun selectPlan(plan: CuratedPlan) {
        _uiState.value = _uiState.value.copy(selectedPlan = plan)
    }
    
    fun clearSelectedPlan() {
        _uiState.value = _uiState.value.copy(selectedPlan = null)
    }
    
    fun refresh() {
        loadPlans()
    }
}
