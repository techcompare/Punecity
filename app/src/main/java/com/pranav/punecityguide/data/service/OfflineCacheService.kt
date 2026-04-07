package com.pranav.punecityguide.data.service

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

private val Context.offlineCacheDataStore: DataStore<Preferences> by preferencesDataStore(name = "offline_cache")

/**
 * Production-grade offline cache with smart invalidation.
 *
 * Design Principles:
 * - Generic key-value cache backed by DataStore
 * - Per-key TTL (time-to-live) support
 * - Stale-while-revalidate strategy
 * - Atomic writes via Mutex
 * - Type-safe retrieval via reified generics
 *
 * This acts as a local HTTP cache for Supabase REST responses,
 * enabling full offline-first behavior for the app.
 */
class OfflineCacheService(private val context: Context) {

    companion object {
        private const val TAG = "OfflineCacheService"
        private const val PREFIX_DATA = "cache_data_"
        private const val PREFIX_TIMESTAMP = "cache_ts_"
        private const val PREFIX_ETAG = "cache_etag_"
        private const val DEFAULT_TTL_MS = 30 * 60 * 1000L // 30 minutes
    }

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val writeMutex = Mutex()

    // In-memory LRU for hot-path reads
    private val memoryCache = LinkedHashMap<String, CacheEntry>(32, 0.75f, true)
    private val maxMemoryEntries = 50

    data class CacheEntry(
        val data: String,
        val timestamp: Long,
        val etag: String? = null
    )

    /**
     * Store data in the cache with a given key.
     *
     * @param key     Cache key (should be stable, e.g. "cities_all", "plans_user_xxx")
     * @param data    The JSON string to cache
     * @param etag    Optional ETag for conditional revalidation
     */
    suspend fun put(key: String, data: String, etag: String? = null) {
        val now = System.currentTimeMillis()
        val entry = CacheEntry(data, now, etag)

        // Memory cache
        synchronized(memoryCache) {
            memoryCache[key] = entry
            if (memoryCache.size > maxMemoryEntries) {
                val oldest = memoryCache.keys.first()
                memoryCache.remove(oldest)
            }
        }

        // Persistent cache
        writeMutex.withLock {
            try {
                context.offlineCacheDataStore.edit { prefs ->
                    prefs[stringPreferencesKey("$PREFIX_DATA$key")] = data
                    prefs[longPreferencesKey("$PREFIX_TIMESTAMP$key")] = now
                    etag?.let { prefs[stringPreferencesKey("$PREFIX_ETAG$key")] = it }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist cache for key=$key: ${e.message}")
            }
        }
    }

    /**
     * Retrieve cached data.
     *
     * @param key    Cache key
     * @param ttlMs  Time-to-live in milliseconds. Returns null if data is older.
     *               Pass Long.MAX_VALUE to always return cached data regardless of age.
     * @return CacheEntry if found and within TTL, null otherwise
     */
    suspend fun get(key: String, ttlMs: Long = DEFAULT_TTL_MS): CacheEntry? {
        val now = System.currentTimeMillis()

        // Check memory first
        val memEntry = synchronized(memoryCache) { memoryCache[key] }
        if (memEntry != null && (ttlMs == Long.MAX_VALUE || now - memEntry.timestamp < ttlMs)) {
            return memEntry
        }

        // Check DataStore
        return try {
            val prefs = context.offlineCacheDataStore.data.firstOrNull() ?: return null
            val data = prefs[stringPreferencesKey("$PREFIX_DATA$key")] ?: return null
            val timestamp = prefs[longPreferencesKey("$PREFIX_TIMESTAMP$key")] ?: return null
            val etag = prefs[stringPreferencesKey("$PREFIX_ETAG$key")]

            if (ttlMs != Long.MAX_VALUE && now - timestamp >= ttlMs) {
                return null // Expired
            }

            val entry = CacheEntry(data, timestamp, etag)
            // Warm memory cache
            synchronized(memoryCache) { memoryCache[key] = entry }
            entry
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read cache for key=$key: ${e.message}")
            null
        }
    }

    /**
     * Stale-while-revalidate: Returns cached data even if expired,
     * but indicates freshness via the second return value.
     *
     * @return Pair of (data, isFresh). Data may be null if never cached.
     */
    suspend fun getStale(key: String, ttlMs: Long = DEFAULT_TTL_MS): Pair<CacheEntry?, Boolean> {
        val fresh = get(key, ttlMs)
        if (fresh != null) return fresh to true

        // Return stale data
        val stale = get(key, Long.MAX_VALUE)
        return stale to false
    }

    /**
     * Invalidate a specific cache key.
     */
    suspend fun invalidate(key: String) {
        synchronized(memoryCache) { memoryCache.remove(key) }
        writeMutex.withLock {
            try {
                context.offlineCacheDataStore.edit { prefs ->
                    prefs.remove(stringPreferencesKey("$PREFIX_DATA$key"))
                    prefs.remove(longPreferencesKey("$PREFIX_TIMESTAMP$key"))
                    prefs.remove(stringPreferencesKey("$PREFIX_ETAG$key"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to invalidate key=$key: ${e.message}")
            }
        }
    }

    /**
     * Clear all cached data.
     */
    suspend fun clearAll() {
        synchronized(memoryCache) { memoryCache.clear() }
        writeMutex.withLock {
            try {
                context.offlineCacheDataStore.edit { it.clear() }
                Log.i(TAG, "All offline cache cleared")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear cache: ${e.message}")
            }
        }
    }

    /**
     * Get the age of a cached entry in milliseconds.
     * Returns null if key doesn't exist.
     */
    suspend fun getAge(key: String): Long? {
        val entry = get(key, Long.MAX_VALUE) ?: return null
        return System.currentTimeMillis() - entry.timestamp
    }

    /**
     * Get cache diagnostics for the health check service.
     */
    fun getCacheSize(): Int = synchronized(memoryCache) { memoryCache.size }
}
