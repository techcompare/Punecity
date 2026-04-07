package com.pranav.punecityguide.data.service

import android.util.Log
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

/**
 * CostPilot Pulse Engine — real-time city intelligence.
 *
 * Provides live and derived data about any destination:
 * - Weather conditions (via Open-Meteo API — free, no key required)
 * - Time-of-day crowd estimates (algorithmic)
 * - Cost pulse (relative cost intensity for the hour)
 *
 * Falls back to algorithmic estimates when API is unavailable.
 */
object PulseService {

    private const val TAG = "PulseService"
    private val json = Json { ignoreUnknownKeys = true }

    // Cache
    private var cachedWeather: WeatherData? = null
    private var cacheTimestamp: Long = 0L
    private var cacheCity: String = ""
    private const val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 minutes

    data class PulseData(
        val temp: Int,
        val condition: String,
        val crowdIndex: String,
        val crowdColor: Long,
        val liveStatus: String,
        val humidity: Int = 0,
        val windSpeed: Double = 0.0,
        val costPulse: String = "MODERATE" // LOW, MODERATE, HIGH, PEAK
    )

    data class WeatherData(
        val temperature: Double,
        val humidity: Int,
        val windSpeed: Double,
        val weatherCode: Int,
        val isDay: Boolean
    )

    /**
     * Get current pulse data using time-of-day algorithms.
     * For weather, call [fetchWeatherPulse] with coordinates.
     */
    fun getCurrentPulse(hour: Int): PulseData {
        val temp = estimateTemperature(hour)
        val condition = getConditionLabel(hour)
        val (crowd, color) = getCrowdEstimate(hour)
        val costPulse = getCostPulse(hour)

        return PulseData(
            temp = temp,
            condition = condition,
            crowdIndex = crowd,
            crowdColor = color,
            liveStatus = "LIVE",
            costPulse = costPulse
        )
    }

    /**
     * Get pulse data with real weather from Open-Meteo API.
     *
     * @param lat  Latitude of the city
     * @param lon  Longitude of the city
     * @param hour Current hour (0-23)
     * @param cityName City name for caching
     */
    suspend fun fetchWeatherPulse(
        lat: Double,
        lon: Double,
        hour: Int,
        cityName: String = ""
    ): PulseData = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        // Check cache
        if (cachedWeather != null && cacheCity == cityName && now - cacheTimestamp < CACHE_DURATION_MS) {
            val w = cachedWeather!!
            val (crowd, color) = getCrowdEstimate(hour)
            return@withContext PulseData(
                temp = w.temperature.toInt(),
                condition = weatherCodeToCondition(w.weatherCode, w.isDay),
                crowdIndex = crowd,
                crowdColor = color,
                liveStatus = "LIVE",
                humidity = w.humidity,
                windSpeed = w.windSpeed,
                costPulse = getCostPulse(hour)
            )
        }

        // Fetch from Open-Meteo (free, no API key)
        try {
            val httpClient = SupabaseClient.getHttpClient()
            val response = httpClient.get("https://api.open-meteo.com/v1/forecast") {
                parameter("latitude", lat)
                parameter("longitude", lon)
                parameter("current_weather", true)
                parameter("hourly", "relative_humidity_2m")
            }

            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                val root = json.parseToJsonElement(body).jsonObject
                val current = root["current_weather"]?.jsonObject

                if (current != null) {
                    val weather = WeatherData(
                        temperature = current["temperature"]?.jsonPrimitive?.doubleOrNull ?: 25.0,
                        humidity = root["hourly"]?.jsonObject
                            ?.get("relative_humidity_2m")?.jsonArray
                            ?.getOrNull(hour)?.jsonPrimitive?.intOrNull ?: 50,
                        windSpeed = current["windspeed"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                        weatherCode = current["weathercode"]?.jsonPrimitive?.intOrNull ?: 0,
                        isDay = (current["is_day"]?.jsonPrimitive?.intOrNull ?: 1) == 1
                    )

                    cachedWeather = weather
                    cacheTimestamp = now
                    cacheCity = cityName

                    val (crowd, color) = getCrowdEstimate(hour)
                    return@withContext PulseData(
                        temp = weather.temperature.toInt(),
                        condition = weatherCodeToCondition(weather.weatherCode, weather.isDay),
                        crowdIndex = crowd,
                        crowdColor = color,
                        liveStatus = "LIVE",
                        humidity = weather.humidity,
                        windSpeed = weather.windSpeed,
                        costPulse = getCostPulse(hour)
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Weather fetch failed: ${e.message}")
        }

        // Fallback to algorithmic estimate
        getCurrentPulse(hour)
    }

    // ── Algorithmic Estimates ──

    private fun estimateTemperature(hour: Int): Int = when (hour) {
        in 0..6 -> 21
        in 7..10 -> 24
        in 11..16 -> 33
        in 17..20 -> 28
        else -> 23
    }

    private fun getConditionLabel(hour: Int): String = when (hour) {
        in 5..7 -> "Dawn Glow"
        in 8..10 -> "Morning Fresh"
        in 11..16 -> "Sunny Pulse"
        in 17..20 -> "Sunset Glow"
        in 21..23 -> "City Lights"
        else -> "Night Calm"
    }

    private fun getCrowdEstimate(hour: Int): Pair<String, Long> = when {
        hour in 18..21 -> "PEAK" to 0xFFE91E63
        hour in 12..14 -> "BUSY" to 0xFFFF9800
        hour in 1..5 -> "QUIET" to 0xFF9C27B0
        else -> "OPTIMAL" to 0xFF4CAF50
    }

    private fun getCostPulse(hour: Int): String = when {
        hour in 18..22 -> "PEAK"      // Dinner rush, surge pricing
        hour in 11..14 -> "HIGH"      // Lunch period
        hour in 6..10 -> "MODERATE"   // Normal morning rates
        hour in 23..23 || hour in 0..5 -> "LOW" // Late night / early morning
        else -> "MODERATE"
    }

    /**
     * Convert WMO weather codes to human-readable conditions.
     * See: https://open-meteo.com/en/docs
     */
    private fun weatherCodeToCondition(code: Int, isDay: Boolean): String = when (code) {
        0 -> if (isDay) "Clear Sky ☀️" else "Clear Night 🌙"
        1 -> if (isDay) "Mostly Clear" else "Mostly Clear Night"
        2 -> "Partly Cloudy ⛅"
        3 -> "Overcast ☁️"
        45, 48 -> "Foggy 🌫️"
        51, 53, 55 -> "Light Drizzle 🌦️"
        61, 63, 65 -> "Rain 🌧️"
        66, 67 -> "Freezing Rain 🧊"
        71, 73, 75 -> "Snow ❄️"
        77 -> "Snow Grains"
        80, 81, 82 -> "Rain Showers 🌧️"
        85, 86 -> "Snow Showers ❄️"
        95 -> "Thunderstorm ⛈️"
        96, 99 -> "Thunderstorm with Hail ⛈️"
        else -> if (isDay) "Fair Weather" else "Night Sky 🌙"
    }
}
