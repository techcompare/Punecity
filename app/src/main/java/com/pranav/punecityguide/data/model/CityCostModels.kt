package com.pranav.punecityguide.data.model

import kotlinx.serialization.Serializable

/**
 * Represents cost-of-living data for a single city.
 * All costs are normalized to USD for easy comparison.
 */
@Serializable
data class CityCost(
    val cityName: String,
    val country: String,
    val countryCode: String,
    val currency: String,
    val currencySymbol: String,
    val continent: String,
    // Food costs (in USD)
    val budgetMeal: Double,
    val midRangeMeal: Double,
    val fineDiningMeal: Double,
    // Drinks
    val coffee: Double,
    val beer: Double,
    val cocktail: Double,
    // Transport
    val publicTransport: Double,   // single ticket
    val taxiPerKm: Double,
    val monthlyPass: Double,
    // Accommodation (per night)
    val hostel: Double,
    val midHotel: Double,
    val luxuryHotel: Double,
    // Services
    val simCard: Double,           // tourist SIM/data plan
    val haircut: Double,
    val gymDayPass: Double,
    // Daily budget estimates
    val backpackerDaily: Double,
    val midRangeDaily: Double,
    val luxuryDaily: Double,
    // Index
    val costIndex: Double = 50.0   // 0-100 scale relative to NYC (100)
)

/**
 * Result of comparing two cities
 */
data class CityComparison(
    val city1: CityCost,
    val city2: CityCost,
    val percentageDifference: Double,  // positive = city2 is more expensive
    val cheaperCity: String,
    val categoryBreakdown: List<CategoryComparison>
)

data class CategoryComparison(
    val category: String,
    val icon: String,
    val city1Value: Double,
    val city2Value: Double,
    val city1Label: String,
    val city2Label: String,
    val percentDiff: Double
)

/**
 * Trip budget model
 */
@Serializable
data class TripBudget(
    val destination: String,
    val durationDays: Int,
    val travelStyle: TravelStyle,
    val groupSize: Int = 1,
    val dailyBudget: Double,
    val totalBudget: Double,
    val breakdown: List<BudgetCategory>
)

@Serializable
enum class TravelStyle(val label: String, val emoji: String) {
    BACKPACKER("Backpacker", "🎒"),
    MID_RANGE("Mid-Range", "🧳"),
    LUXURY("Luxury", "💎")
}

@Serializable
data class BudgetCategory(
    val name: String,
    val emoji: String,
    val dailyCost: Double,
    val totalCost: Double,
    val percentage: Double
)
