package com.pranav.punecityguide.data.repository

import android.util.Log
import com.pranav.punecityguide.data.database.AttractionDao
import com.pranav.punecityguide.data.model.Attraction
import com.pranav.punecityguide.data.service.AttractionCache
import com.pranav.punecityguide.util.CacheConstants
import com.pranav.punecityguide.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Single source-of-truth for attraction data.
 *
 * Production-grade features:
 * - L1 in-memory cache (AttractionCache) for hot-path reads
 * - L2 Room database for persistence
 * - Write-through caching: mutations invalidate relevant cache keys
 * - Batch operations with integrity validation
 * - Thread-safe via coroutine dispatchers
 */
class AttractionRepository(
    private val dao: AttractionDao,
    private val recentlyViewedDao: com.pranav.punecityguide.data.database.RecentlyViewedDao
) {

    private val TAG = "AttractionRepository"

    fun getRecentlyViewedCount(): Flow<Int> {
        return recentlyViewedDao.getCount()
    }

    // ── Read Operations (cached where possible) ──

    fun getTopAttractions(limit: Int = 10): Flow<List<Attraction>> {
        return dao.getTopAttractions(limit)
    }

    fun getAttractionsByCategory(category: String): Flow<List<Attraction>> {
        return dao.getAttractionsByCategory(category)
    }

    fun searchAttractions(query: String): Flow<List<Attraction>> {
        return dao.searchAttractions("%$query%")
    }

    fun getAllCategories(): Flow<List<String>> {
        return dao.getAllCategories()
    }

    fun observeAttractionById(id: Int): Flow<Attraction?> {
        return dao.observeAttractionById(id)
    }

    suspend fun getAttractionById(id: Int): Attraction? = withContext(Dispatchers.IO) {
        dao.getAttractionById(id)
    }

    fun getFavoriteAttractions(): Flow<List<Attraction>> {
        return dao.getFavoriteAttractions()
    }

    fun observeAllAttractions(): Flow<List<Attraction>> {
        return dao.observeAllAttractions()
    }

    /**
     * Returns all attractions with L1 cache support.
     * Cache key: [CacheConstants.ALL_ATTRACTIONS]
     */
    suspend fun getAllAttractions(): List<Attraction> = withContext(Dispatchers.IO) {
        // L1 cache check
        val cached = AttractionCache.get(CacheConstants.ALL_ATTRACTIONS)
        if (cached != null) return@withContext cached

        // L2: Room DB
        val dbResult = dao.getAllAttractions()
        if (dbResult.isNotEmpty()) {
            AttractionCache.put(CacheConstants.ALL_ATTRACTIONS, dbResult)
        }
        dbResult
    }

    suspend fun getAttractionCount(): Int = withContext(Dispatchers.IO) {
        dao.getAttractionCount()
    }

    // ── Write Operations (with cache invalidation) ──

    suspend fun addAttraction(attraction: Attraction) = withContext(Dispatchers.IO) {
        dao.insertAttraction(attraction)
        invalidateAllCaches()
        Logger.d("[$TAG] Added attraction: ${attraction.name}")
    }

    suspend fun addAttractions(attractions: List<Attraction>) = withContext(Dispatchers.IO) {
        if (attractions.isEmpty()) return@withContext
        dao.insertAttractions(attractions)
        invalidateAllCaches()
        Logger.d("[$TAG] Batch inserted ${attractions.size} attractions")
    }

    /**
     * Production-grade upsert with data integrity.
     * - Preserves local-only fields (isFavorite)
     * - De-duplicates by natural key (name + category)
     * - Validates incoming data before writing
     * - Invalidates all caches after write
     */
    suspend fun upsertAllAttractions(attractions: List<Attraction>) = withContext(Dispatchers.IO) {
        if (attractions.isEmpty()) {
            Logger.w("[$TAG] upsertAllAttractions called with empty list — skipping")
            return@withContext
        }

        // Pre-flight validation: filter out clearly bad data
        val validated = attractions.filter { attraction ->
            val isValid = attraction.name.isNotBlank() && attraction.category.isNotBlank()
            if (!isValid) {
                Logger.w("[$TAG] Skipping invalid attraction: name='${attraction.name}', category='${attraction.category}'")
            }
            isValid
        }

        if (validated.isEmpty()) {
            Logger.w("[$TAG] All ${attractions.size} attractions failed validation — skipping upsert")
            return@withContext
        }

        dao.upsertAllAttractions(validated)
        invalidateAllCaches()
        Logger.d("[$TAG] Upserted ${validated.size}/${attractions.size} attractions (${attractions.size - validated.size} rejected)")
    }

    suspend fun updateAttraction(attraction: Attraction) = withContext(Dispatchers.IO) {
        dao.updateAttraction(attraction)
        invalidateAllCaches()
    }

    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        dao.updateFavoriteStatus(id, isFavorite)
        // Only invalidate the "all" cache — category caches remain valid
        AttractionCache.invalidate(CacheConstants.ALL_ATTRACTIONS)
        Logger.d("[$TAG] Favorite status updated: id=$id, isFavorite=$isFavorite")
    }

    suspend fun deleteAttraction(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteAttraction(id)
        invalidateAllCaches()
        Logger.d("[$TAG] Deleted attraction id=$id")
    }

    // ── Cache Management ──

    private suspend fun invalidateAllCaches() {
        AttractionCache.invalidate(CacheConstants.ALL_ATTRACTIONS)
        AttractionCache.invalidateByPrefix(CacheConstants.PREFIX_TOP)
        AttractionCache.invalidateByPrefix(CacheConstants.PREFIX_CATEGORY)
    }

    /** Expose cache statistics for health monitoring. */
    suspend fun getCacheStats(): AttractionCache.CacheStats {
        return AttractionCache.stats()
    }

    /** Force clear all caches (e.g. after a full data refresh). */
    suspend fun clearCaches() {
        AttractionCache.clear()
    }
}
