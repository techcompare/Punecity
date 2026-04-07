package com.pranav.punecityguide.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.max
import kotlin.math.roundToInt

data class GuideUiState(
    val distanceKm: String = "",
    val isNightRate: Boolean = false,
    val luggageCount: Int = 0,
    val calculatedFare: Double = 0.0,
    val fareBreakdown: String = ""
)

class GuideViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GuideUiState())
    val uiState: StateFlow<GuideUiState> = _uiState.asStateFlow()

    // Official Pune RTO Rates (Effective Feb 1, 2025)
    private val BASE_FARE = 26.0
    private val BASE_DISTANCE = 1.5
    private val PER_KM_RATE = 17.0
    private val NIGHT_SURCHARGE = 1.25 // 25% extra
    private val LUGGAGE_RATE = 20.0

    fun onDistanceChange(distance: String) {
        _uiState.update { it.copy(distanceKm = distance) }
        calculateFare()
    }

    fun onNightRateToggle(isNight: Boolean) {
        _uiState.update { it.copy(isNightRate = isNight) }
        calculateFare()
    }

    fun onLuggageChange(count: Int) {
        _uiState.update { it.copy(luggageCount = count) }
        calculateFare()
    }

    private fun calculateFare() {
        val distance = _uiState.value.distanceKm.toDoubleOrNull() ?: 0.0
        if (distance <= 0.0) {
            _uiState.update { it.copy(calculatedFare = 0.0, fareBreakdown = "") }
            return
        }

        var fare = if (distance <= BASE_DISTANCE) {
            BASE_FARE
        } else {
            BASE_FARE + (distance - BASE_DISTANCE) * PER_KM_RATE
        }

        val breakdown = StringBuilder()
        breakdown.append("Base (1.5km): ₹${BASE_FARE.toInt()}")
        
        if (distance > BASE_DISTANCE) {
            val extraKm = distance - BASE_DISTANCE
            val extraFare = extraKm * PER_KM_RATE
            breakdown.append("\nExtra (${String.format("%.2f", extraKm)}km): ₹${extraFare.roundToInt()}")
        }

        if (_uiState.value.isNightRate) {
            fare *= NIGHT_SURCHARGE
            breakdown.append("\nNight Surcharge (25%): ₹${(fare / NIGHT_SURCHARGE * 0.25).roundToInt()}")
        }

        if (_uiState.value.luggageCount > 0) {
            val luggageFare = _uiState.value.luggageCount * LUGGAGE_RATE
            fare += luggageFare
            breakdown.append("\nLuggage (${_uiState.value.luggageCount} bags): ₹${luggageFare.roundToInt()}")
        }

        _uiState.update { 
            it.copy(
                calculatedFare = fare.roundToInt().toDouble(),
                fareBreakdown = breakdown.toString()
            ) 
        }
    }
}
