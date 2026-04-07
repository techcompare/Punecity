package com.pranav.punecityguide.data.service

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pranav.punecityguide.AppConfig
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

private val Context.currencyDataStore: DataStore<Preferences> by preferencesDataStore(name = "currency_cache")

/**
 * Production-grade currency conversion service with multi-layer caching.
 *
 * Architecture:
 * - Layer 1: In-memory cache (instant, volatile)
 * - Layer 2: DataStore persistence (survives process death)
 * - Layer 3: Remote API fetch (refreshed every 6 hours)
 * - Layer 4: Hardcoded fallback rates (offline-safe baseline)
 *
 * Uses frankfurter.app (free, no API key required, ECB-sourced data).
 */
class CurrencyService(private val context: Context) {

    companion object {
        private const val TAG = "CurrencyService"
        private const val REFRESH_INTERVAL_MS = 6 * 60 * 60 * 1000L // 6 hours
        private val KEY_RATES_JSON = stringPreferencesKey("rates_json")
        private val KEY_RATES_TIMESTAMP = longPreferencesKey("rates_timestamp")
        private val KEY_BASE_CURRENCY = stringPreferencesKey("base_currency")
    }

    // In-memory cache
    private var cachedRates: Map<String, Double>? = null
    private var cacheTimestamp: Long = 0L
    private var cacheBaseCurrency: String = "USD"
    private val refreshMutex = Mutex()

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Convert an amount from one currency to another.
     *
     * @param amount     The value to convert
     * @param from       Source currency code (e.g. "USD")
     * @param to         Target currency code (e.g. "EUR")
     * @return Converted amount, or null if conversion is impossible
     */
    suspend fun convert(amount: Double, from: String, to: String): Double? {
        if (from.equals(to, ignoreCase = true)) return amount
        val rates = getRates("USD") ?: return null
        val fromRate = if (from.equals("USD", ignoreCase = true)) 1.0 else rates[from.uppercase()]
        val toRate = if (to.equals("USD", ignoreCase = true)) 1.0 else rates[to.uppercase()]
        if (fromRate == null || toRate == null) return null
        return amount * (toRate / fromRate)
    }

    /**
     * Get the exchange rate between two currencies.
     */
    suspend fun getRate(from: String, to: String): Double? {
        return convert(1.0, from, to)
    }

    /**
     * Get all available exchange rates relative to a base currency.
     * Uses multi-layer caching strategy.
     */
    suspend fun getRates(baseCurrency: String = "USD"): Map<String, Double>? {
        val now = System.currentTimeMillis()

        // Layer 1: In-memory cache
        if (cachedRates != null && cacheBaseCurrency == baseCurrency && now - cacheTimestamp < REFRESH_INTERVAL_MS) {
            return cachedRates
        }

        return refreshMutex.withLock {
            // Double-check after acquiring lock
            if (cachedRates != null && cacheBaseCurrency == baseCurrency && now - cacheTimestamp < REFRESH_INTERVAL_MS) {
                return@withLock cachedRates
            }

            // Layer 2: DataStore cache
            val stored = loadFromDataStore()
            if (stored != null && stored.third == baseCurrency && now - stored.second < REFRESH_INTERVAL_MS) {
                cachedRates = stored.first
                cacheTimestamp = stored.second
                cacheBaseCurrency = stored.third
                return@withLock stored.first
            }

            // Layer 3: Remote fetch
            val remote = fetchRemoteRates(baseCurrency)
            if (remote != null) {
                cachedRates = remote
                cacheTimestamp = now
                cacheBaseCurrency = baseCurrency
                saveToDataStore(remote, now, baseCurrency)
                return@withLock remote
            }

            // Layer 4: Stale DataStore cache (better than nothing)
            if (stored != null) {
                cachedRates = stored.first
                cacheTimestamp = stored.second
                cacheBaseCurrency = stored.third
                Log.w(TAG, "Using stale cached rates (age: ${(now - stored.second) / 3600000}h)")
                return@withLock stored.first
            }

            // Layer 5: Hardcoded fallback
            Log.w(TAG, "Using hardcoded fallback rates")
            val fallback = getHardcodedRates()
            cachedRates = fallback
            cacheTimestamp = now
            cacheBaseCurrency = "USD"
            fallback
        }
    }

    /**
     * Get list of supported currency codes.
     */
    suspend fun getSupportedCurrencies(): List<String> {
        val rates = getRates("USD") ?: return getHardcodedRates().keys.toList()
        return (rates.keys + "USD").sorted()
    }

    /**
     * Force refresh rates from remote API.
     */
    suspend fun forceRefresh(baseCurrency: String = "USD"): Boolean {
        val remote = fetchRemoteRates(baseCurrency)
        if (remote != null) {
            refreshMutex.withLock {
                val now = System.currentTimeMillis()
                cachedRates = remote
                cacheTimestamp = now
                cacheBaseCurrency = baseCurrency
                saveToDataStore(remote, now, baseCurrency)
            }
            return true
        }
        return false
    }

    // ── Remote API ──

    private suspend fun fetchRemoteRates(baseCurrency: String): Map<String, Double>? = withContext(Dispatchers.IO) {
        try {
            val httpClient = SupabaseClient.getHttpClient()
            val response = httpClient.get("https://api.frankfurter.app/latest") {
                parameter("from", baseCurrency.uppercase())
            }

            if (!response.status.isSuccess()) {
                Log.w(TAG, "Currency API returned ${response.status}")
                return@withContext null
            }

            val body = response.bodyAsText()
            val parsed = json.parseToJsonElement(body).jsonObject
            val ratesObj = parsed["rates"]?.jsonObject ?: return@withContext null

            val rates = mutableMapOf<String, Double>()
            for ((key, value) in ratesObj) {
                val rate = when (value) {
                    is JsonPrimitive -> value.doubleOrNull
                    else -> null
                }
                if (rate != null) rates[key] = rate
            }

            Log.i(TAG, "Fetched ${rates.size} exchange rates from API")
            rates
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch rates: ${e.message}")
            null
        }
    }

    // ── DataStore Persistence ──

    private suspend fun saveToDataStore(rates: Map<String, Double>, timestamp: Long, baseCurrency: String) {
        try {
            val ratesJson = buildJsonObject {
                rates.forEach { (k, v) -> put(k, v) }
            }.toString()
            context.currencyDataStore.edit { prefs ->
                prefs[KEY_RATES_JSON] = ratesJson
                prefs[KEY_RATES_TIMESTAMP] = timestamp
                prefs[KEY_BASE_CURRENCY] = baseCurrency
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save rates to DataStore: ${e.message}")
        }
    }

    private suspend fun loadFromDataStore(): Triple<Map<String, Double>, Long, String>? {
        return try {
            val prefs = context.currencyDataStore.data.firstOrNull() ?: return null
            val ratesStr = prefs[KEY_RATES_JSON] ?: return null
            val timestamp = prefs[KEY_RATES_TIMESTAMP] ?: return null
            val base = prefs[KEY_BASE_CURRENCY] ?: "USD"

            val ratesObj = json.parseToJsonElement(ratesStr).jsonObject
            val rates = mutableMapOf<String, Double>()
            for ((key, value) in ratesObj) {
                (value as? JsonPrimitive)?.doubleOrNull?.let { rates[key] = it }
            }
            Triple(rates, timestamp, base)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load rates from DataStore: ${e.message}")
            null
        }
    }

    // ── Hardcoded Fallback (ECB approximate rates as of 2026) ──

    private fun getHardcodedRates(): Map<String, Double> = mapOf(
        "EUR" to 0.92, "GBP" to 0.79, "JPY" to 149.50, "AUD" to 1.53,
        "CAD" to 1.36, "CHF" to 0.88, "CNY" to 7.24, "INR" to 83.12,
        "KRW" to 1325.0, "SGD" to 1.34, "HKD" to 7.82, "NZD" to 1.63,
        "SEK" to 10.42, "NOK" to 10.55, "DKK" to 6.88, "PLN" to 4.02,
        "CZK" to 22.85, "HUF" to 357.0, "THB" to 35.50, "IDR" to 15680.0,
        "MYR" to 4.72, "PHP" to 56.20, "VND" to 24850.0, "BRL" to 4.97,
        "MXN" to 17.15, "ZAR" to 18.65, "TRY" to 32.50, "AED" to 3.67,
        "SAR" to 3.75, "EGP" to 30.90, "NGN" to 1550.0, "KES" to 153.0,
        "COP" to 3950.0, "ARS" to 850.0, "PEN" to 3.72, "CLP" to 925.0
    )
}
