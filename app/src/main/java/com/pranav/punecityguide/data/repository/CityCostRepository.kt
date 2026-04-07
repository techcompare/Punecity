package com.pranav.punecityguide.data.repository

import android.util.Log
import com.pranav.punecityguide.AppConfig
import com.pranav.punecityguide.data.model.*
import com.pranav.punecityguide.data.service.NetworkResilience
import com.pranav.punecityguide.data.service.OfflineCacheService
import com.pranav.punecityguide.data.service.SupabaseClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Production-grade city cost repository with offline-first architecture.
 *
 * Data flow:
 * 1. Check in-memory cache (instant)
 * 2. Check OfflineCacheService / DataStore (fast, survives process death)
 * 3. Fetch from Supabase with NetworkResilience (retry + circuit breaker)
 * 4. Return stale cache if network fails (graceful degradation)
 *
 * All Supabase responses are automatically cached for offline use.
 */
class CityCostRepository(
    private val cacheService: OfflineCacheService? = null
) {
    private val client: HttpClient get() = SupabaseClient.getHttpClient()
    private val baseUrl = AppConfig.Supabase.SUPABASE_URL
    private val TAG = "CityCostRepo"
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val CACHE_KEY_ALL_CITIES = "cities_all"
        private const val CACHE_TTL_MS = 30 * 60 * 1000L // 30 minutes
        private const val SEARCH_CACHE_PREFIX = "cities_search_"
    }

    // In-memory cache for ultra-fast repeated access
    private var memoryCities: List<CityCost>? = null
    private var memoryCacheTimestamp: Long = 0L

    suspend fun getAllCities(): Result<List<CityCost>> {
        // Layer 1: In-memory
        val now = System.currentTimeMillis()
        memoryCities?.let {
            if (now - memoryCacheTimestamp < CACHE_TTL_MS) {
                return Result.success(it)
            }
        }

        // Layer 2: Offline cache (stale-while-revalidate)
        val cached = cacheService?.getStale(CACHE_KEY_ALL_CITIES, CACHE_TTL_MS)
        if (cached != null && cached.first != null) {
            val cities = try {
                json.decodeFromString<List<CityCost>>(cached.first!!.data)
            } catch (_: Exception) { null }

            if (cities != null) {
                memoryCities = cities
                memoryCacheTimestamp = cached.first!!.timestamp

                if (cached.second) {
                    // Data is fresh, return immediately
                    return Result.success(cities)
                }
                // Data is stale — try to refresh in background, but return stale data now
                val freshResult = fetchFromNetwork()
                return if (freshResult.isSuccess) freshResult else Result.success(cities)
            }
        }

        // Layer 3: Network fetch with resilience
        return fetchFromNetwork()
    }

    private suspend fun fetchFromNetwork(): Result<List<CityCost>> {
        return NetworkResilience.withRetry("city_costs", maxRetries = 2) {
            val response = client.get("$baseUrl/rest/v1/city_costs") {
                parameter("select", "*")
                parameter("order", "cityName.asc")
            }
            if (response.status.isSuccess()) {
                val list: List<CityCost> = response.body()
                // Update caches
                memoryCities = list
                memoryCacheTimestamp = System.currentTimeMillis()
                try {
                    cacheService?.put(CACHE_KEY_ALL_CITIES, json.encodeToString(list))
                } catch (e: Exception) {
                    Log.w(TAG, "Cache write failed: ${e.message}")
                }
                list
            } else {
                throw Exception("Failed to fetch cities: HTTP ${response.status.value}")
            }
        }
    }

    suspend fun searchCities(query: String): Result<List<CityCost>> {
        if (query.isBlank()) return getAllCities()

        // Try local search first if we have all cities cached
        memoryCities?.let { all ->
            val filtered = all.filter {
                it.cityName.contains(query, ignoreCase = true) ||
                it.country.contains(query, ignoreCase = true) ||
                it.continent.contains(query, ignoreCase = true)
            }
            if (filtered.isNotEmpty()) return Result.success(filtered)
        }

        // Fallback to remote search
        return try {
            val response = client.get("$baseUrl/rest/v1/city_costs") {
                parameter("cityName", "ilike.%${query}%")
                parameter("select", "*")
                parameter("order", "cityName.asc")
            }
            if (response.status.isSuccess()) {
                val list: List<CityCost> = response.body()
                Result.success(list)
            } else {
                Result.failure(Exception("Failed to search cities"))
            }
        } catch (e: Exception) {
            // Graceful fallback: search in stale cached data
            val stale = cacheService?.get(CACHE_KEY_ALL_CITIES, Long.MAX_VALUE)
            if (stale != null) {
                try {
                    val all = json.decodeFromString<List<CityCost>>(stale.data)
                    val filtered = all.filter {
                        it.cityName.contains(query, ignoreCase = true) ||
                        it.country.contains(query, ignoreCase = true)
                    }
                    Result.success(filtered)
                } catch (_: Exception) {
                    Result.failure(e)
                }
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * Get cities filtered by continent.
     */
    suspend fun getCitiesByContinent(continent: String): Result<List<CityCost>> {
        val all = getAllCities()
        return all.map { cities ->
            cities.filter { it.continent.equals(continent, ignoreCase = true) }
        }
    }

    /**
     * Get the cheapest cities globally (sorted by backpacker daily budget).
     */
    suspend fun getCheapestCities(limit: Int = 10): Result<List<CityCost>> {
        val all = getAllCities()
        return all.map { cities ->
            cities.sortedBy { it.backpackerDaily }.take(limit)
        }
    }

    /**
     * Get the most expensive cities globally.
     */
    suspend fun getMostExpensiveCities(limit: Int = 10): Result<List<CityCost>> {
        val all = getAllCities()
        return all.map { cities ->
            cities.sortedByDescending { it.luxuryDaily }.take(limit)
        }
    }

    /**
     * Get unique continents from the city data.
     */
    suspend fun getContinents(): List<String> {
        return getAllCities().getOrDefault(emptyList())
            .map { it.continent }
            .distinct()
            .sorted()
    }

    fun compareCities(city1: CityCost, city2: CityCost): CityComparison {
        val categories = listOf(
            CategoryComparison("Budget Meal", "🍽️", city1.budgetMeal, city2.budgetMeal, city1.cityName, city2.cityName, pctDiff(city1.budgetMeal, city2.budgetMeal)),
            CategoryComparison("Mid-Range Dinner", "🥘", city1.midRangeMeal, city2.midRangeMeal, city1.cityName, city2.cityName, pctDiff(city1.midRangeMeal, city2.midRangeMeal)),
            CategoryComparison("Coffee", "☕", city1.coffee, city2.coffee, city1.cityName, city2.cityName, pctDiff(city1.coffee, city2.coffee)),
            CategoryComparison("Beer", "🍺", city1.beer, city2.beer, city1.cityName, city2.cityName, pctDiff(city1.beer, city2.beer)),
            CategoryComparison("Public Transport", "🚌", city1.publicTransport, city2.publicTransport, city1.cityName, city2.cityName, pctDiff(city1.publicTransport, city2.publicTransport)),
            CategoryComparison("Taxi (per km)", "🚕", city1.taxiPerKm, city2.taxiPerKm, city1.cityName, city2.cityName, pctDiff(city1.taxiPerKm, city2.taxiPerKm)),
            CategoryComparison("Hostel Night", "🛏️", city1.hostel, city2.hostel, city1.cityName, city2.cityName, pctDiff(city1.hostel, city2.hostel)),
            CategoryComparison("Mid Hotel Night", "🏨", city1.midHotel, city2.midHotel, city1.cityName, city2.cityName, pctDiff(city1.midHotel, city2.midHotel)),
            CategoryComparison("Luxury Hotel", "🏰", city1.luxuryHotel, city2.luxuryHotel, city1.cityName, city2.cityName, pctDiff(city1.luxuryHotel, city2.luxuryHotel)),
            CategoryComparison("SIM Card", "📱", city1.simCard, city2.simCard, city1.cityName, city2.cityName, pctDiff(city1.simCard, city2.simCard)),
            CategoryComparison("Haircut", "💇", city1.haircut, city2.haircut, city1.cityName, city2.cityName, pctDiff(city1.haircut, city2.haircut))
        )

        val avgDiff = categories.map { it.percentDiff }.average()
        val cheaper = if (avgDiff > 0) city1.cityName else city2.cityName

        return CityComparison(
            city1 = city1,
            city2 = city2,
            percentageDifference = avgDiff,
            cheaperCity = cheaper,
            categoryBreakdown = categories
        )
    }

    fun generateTripBudget(city: CityCost, days: Int, style: TravelStyle, groupSize: Int): TripBudget {
        val dailyBudget = when (style) {
            TravelStyle.BACKPACKER -> city.backpackerDaily
            TravelStyle.MID_RANGE -> city.midRangeDaily
            TravelStyle.LUXURY -> city.luxuryDaily
        }

        val foodPct = 0.30
        val accommodationPct = 0.40
        val transportPct = 0.15
        val activitiesPct = 0.10
        val miscPct = 0.05

        val breakdown = listOf(
            BudgetCategory("Food & Drinks", "🍽️", dailyBudget * foodPct, dailyBudget * foodPct * days, foodPct * 100),
            BudgetCategory("Accommodation", "🏨", dailyBudget * accommodationPct, dailyBudget * accommodationPct * days, accommodationPct * 100),
            BudgetCategory("Transport", "🚕", dailyBudget * transportPct, dailyBudget * transportPct * days, transportPct * 100),
            BudgetCategory("Activities", "🎟️", dailyBudget * activitiesPct, dailyBudget * activitiesPct * days, activitiesPct * 100),
            BudgetCategory("Miscellaneous", "🛍️", dailyBudget * miscPct, dailyBudget * miscPct * days, miscPct * 100)
        )

        return TripBudget(
            destination = "${city.cityName}, ${city.country}",
            durationDays = days,
            travelStyle = style,
            groupSize = groupSize,
            dailyBudget = dailyBudget * groupSize,
            totalBudget = dailyBudget * days * groupSize,
            breakdown = breakdown.map { it.copy(dailyCost = it.dailyCost * groupSize, totalCost = it.totalCost * groupSize) }
        )
    }

    /**
     * Invalidate all caches (for pull-to-refresh).
     */
    suspend fun invalidateCache() {
        memoryCities = null
        memoryCacheTimestamp = 0L
        cacheService?.invalidate(CACHE_KEY_ALL_CITIES)
        Log.d(TAG, "City cost cache invalidated")
    }

    private fun pctDiff(a: Double, b: Double): Double {
        if (a == 0.0) return 0.0
        return ((b - a) / a) * 100.0
    }
}
