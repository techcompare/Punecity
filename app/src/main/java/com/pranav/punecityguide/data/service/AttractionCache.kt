package com.pranav.punecityguide.data.service

import android.util.Log
import com.pranav.punecityguide.data.model.Attraction
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe, time-aware in-memory cache for attractions.
 *
 * Production features:
 * - LRU eviction when capacity is exceeded
 * - TTL-based staleness detection (entries expire after [ttlMs])
 * - Thread-safe via Kotlin Mutex for coroutine-friendly locking
 * - Cache statistics for monitoring hit rates
 */
object AttractionCache {

    private const val TAG = "AttractionCache"
    private const val DEFAULT_TTL_MS = 10 * 60 * 1000L  // 10 minutes
    private const val MAX_ENTRIES = 500

    private data class CacheEntry(
        val attractions: List<Attraction>,
        val insertedAt: Long = System.currentTimeMillis()
    ) {
        fun isStale(ttlMs: Long = DEFAULT_TTL_MS): Boolean =
            System.currentTimeMillis() - insertedAt > ttlMs
    }

    // Keyed caches: "top_30", "category_Historical", "search_fort", "all", etc.
    private val cache = LinkedHashMap<String, CacheEntry>(MAX_ENTRIES, 0.75f, true) // access-order for LRU
    private val mutex = Mutex()

    // Stats
    private var hits = 0L
    private var misses = 0L

    /** Retrieve cached attractions for a given key, or null if stale/missing. */
    suspend fun get(key: String, ttlMs: Long = DEFAULT_TTL_MS): List<Attraction>? = mutex.withLock {
        val entry = cache[key]
        if (entry != null && !entry.isStale(ttlMs)) {
            hits++
            Log.d(TAG, "CACHE HIT for '$key' (${entry.attractions.size} items, age=${ageString(entry.insertedAt)})")
            entry.attractions
        } else {
            misses++
            if (entry != null) {
                cache.remove(key)
                Log.d(TAG, "CACHE STALE for '$key' (age=${ageString(entry.insertedAt)}) — evicted")
            }
            null
        }
    }

    /** Insert or update attractions for a given key. */
    suspend fun put(key: String, attractions: List<Attraction>) = mutex.withLock {
        // Evict oldest if at capacity
        if (cache.size >= MAX_ENTRIES) {
            val oldestKey = cache.keys.firstOrNull()
            if (oldestKey != null) {
                cache.remove(oldestKey)
                Log.d(TAG, "LRU eviction: removed '$oldestKey'")
            }
        }
        cache[key] = CacheEntry(attractions)
        Log.d(TAG, "CACHE PUT '$key' (${attractions.size} items)")
    }

    /** Invalidate a specific key. */
    suspend fun invalidate(key: String) = mutex.withLock {
        cache.remove(key)
        Log.d(TAG, "CACHE INVALIDATE '$key'")
    }

    /** Invalidate all keys whose name starts with [prefix]. */
    suspend fun invalidateByPrefix(prefix: String) = mutex.withLock {
        val keysToRemove = cache.keys.filter { it.startsWith(prefix) }
        keysToRemove.forEach { cache.remove(it) }
        if (keysToRemove.isNotEmpty()) {
            Log.d(TAG, "CACHE INVALIDATE prefix '$prefix': removed ${keysToRemove.size} entries")
        }
    }

    /** Clear the entire cache. */
    suspend fun clear() = mutex.withLock {
        cache.clear()
        Log.d(TAG, "CACHE CLEARED")
    }

    /** Return a snapshot of cache statistics. */
    suspend fun stats(): CacheStats = mutex.withLock {
        CacheStats(
            size = cache.size,
            hits = hits,
            misses = misses,
            hitRate = if (hits + misses > 0) (hits.toDouble() / (hits + misses) * 100) else 0.0
        )
    }

    private fun ageString(insertedAt: Long): String {
        val ageSec = (System.currentTimeMillis() - insertedAt) / 1000
        return when {
            ageSec < 60 -> "${ageSec}s"
            ageSec < 3600 -> "${ageSec / 60}m ${ageSec % 60}s"
            else -> "${ageSec / 3600}h ${(ageSec % 3600) / 60}m"
        }
    }

    data class CacheStats(
        val size: Int,
        val hits: Long,
        val misses: Long,
        val hitRate: Double
    ) {
        override fun toString(): String =
            "CacheStats(size=$size, hits=$hits, misses=$misses, hitRate=${"%.1f".format(hitRate)}%)"
    }
}
