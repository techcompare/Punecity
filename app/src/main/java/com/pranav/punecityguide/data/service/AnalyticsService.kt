package com.pranav.punecityguide.data.service

import android.util.Log
import com.pranav.punecityguide.data.repository.SyncAuditRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lightweight analytics service for tracking user engagement patterns.
 *
 * All events are funneled through the audit repository (local Room DB),
 * keeping analytics self-contained and privacy-respecting (no third-party SDKs).
 *
 * Categories:
 * - ENGAGEMENT: Screen views, feature usage, session duration
 * - CONVERSION: AI queries, plan creation, expense tracking
 * - RETENTION: Streaks, return visits, feature discovery
 * - PERFORMANCE: API latency, cache hit rates, error rates
 */
class AnalyticsService(
    private val auditRepository: SyncAuditRepository,
    private val preferenceManager: PreferenceManager
) {

    companion object {
        private const val TAG = "AnalyticsService"

        // Event types
        const val SCREEN_VIEW = "SCREEN_VIEW"
        const val FEATURE_USED = "FEATURE_USED"
        const val AI_QUERY = "AI_QUERY"
        const val PLAN_CREATED = "PLAN_CREATED"
        const val EXPENSE_ADDED = "EXPENSE_ADDED"
        const val CITY_COMPARED = "CITY_COMPARED"
        const val CURRENCY_CONVERTED = "CURRENCY_CONVERTED"
        const val COMMUNITY_POST = "COMMUNITY_POST"
        const val SESSION_START = "SESSION_START"
        const val SESSION_END = "SESSION_END"
        const val ERROR_OCCURRED = "ERROR_OCCURRED"
        const val CACHE_HIT = "CACHE_HIT"
        const val CACHE_MISS = "CACHE_MISS"
        const val API_LATENCY = "API_LATENCY"
    }

    // Session tracking
    private var sessionStartMs: Long = 0L
    private var screenViewCount: Int = 0

    /**
     * Track a screen view event.
     */
    suspend fun trackScreenView(screenName: String) {
        screenViewCount++
        auditRepository.log(SCREEN_VIEW, screenName, "view_count=$screenViewCount")
    }

    /**
     * Track feature usage.
     */
    suspend fun trackFeatureUsed(featureName: String, metadata: String? = null) {
        auditRepository.log(FEATURE_USED, featureName, metadata)
    }

    /**
     * Track AI query with latency.
     */
    suspend fun trackAiQuery(model: String, latencyMs: Long, success: Boolean) {
        val status = if (success) "success" else "failure"
        auditRepository.log(AI_QUERY, "model=$model,status=$status,latency=${latencyMs}ms")
    }

    /**
     * Track city comparison.
     */
    suspend fun trackCityComparison(city1: String, city2: String) {
        auditRepository.log(CITY_COMPARED, "$city1 vs $city2")
    }

    /**
     * Track currency conversion.
     */
    suspend fun trackCurrencyConversion(from: String, to: String) {
        auditRepository.log(CURRENCY_CONVERTED, "$from → $to")
    }

    /**
     * Track API call latency for performance monitoring.
     */
    suspend fun trackApiLatency(endpoint: String, latencyMs: Long, statusCode: Int) {
        auditRepository.log(
            API_LATENCY,
            "endpoint=$endpoint,latency=${latencyMs}ms,status=$statusCode"
        )
    }

    /**
     * Track cache hit/miss for optimization insights.
     */
    suspend fun trackCacheEvent(key: String, hit: Boolean) {
        auditRepository.log(
            if (hit) CACHE_HIT else CACHE_MISS,
            "key=$key"
        )
    }

    /**
     * Track an error event.
     */
    suspend fun trackError(context: String, error: String) {
        auditRepository.log(ERROR_OCCURRED, "context=$context", "error=$error")
    }

    /**
     * Start a user session.
     */
    suspend fun startSession() {
        sessionStartMs = System.currentTimeMillis()
        screenViewCount = 0
        auditRepository.log(SESSION_START, "Session started")
        preferenceManager.updateStreak()
    }

    /**
     * End a user session and log duration.
     */
    suspend fun endSession() {
        if (sessionStartMs > 0) {
            val durationMs = System.currentTimeMillis() - sessionStartMs
            val durationSec = durationMs / 1000
            auditRepository.log(
                SESSION_END,
                "duration=${durationSec}s,screens=$screenViewCount"
            )
            sessionStartMs = 0L
        }
    }

    /**
     * Generate a comprehensive analytics report.
     */
    suspend fun generateReport(hoursBack: Int = 24): AnalyticsReport = withContext(Dispatchers.IO) {
        try {
            val summary = auditRepository.generateSummary(hoursBack)

            AnalyticsReport(
                totalEvents = summary.values.sum(),
                screenViews = summary[SCREEN_VIEW] ?: 0,
                aiQueries = summary[AI_QUERY] ?: 0,
                cityComparisons = summary[CITY_COMPARED] ?: 0,
                expensesAdded = summary[EXPENSE_ADDED] ?: 0,
                communityPosts = summary[COMMUNITY_POST] ?: 0,
                errors = summary[ERROR_OCCURRED] ?: 0,
                cacheHitRate = calculateCacheHitRate(summary),
                topFeatures = summary.filterKeys { it == FEATURE_USED }
                    .entries.sortedByDescending { it.value }
                    .take(5)
                    .associate { it.key to it.value },
                period = "${hoursBack}h"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Report generation failed: ${e.message}")
            AnalyticsReport()
        }
    }

    private fun calculateCacheHitRate(summary: Map<String, Int>): Double {
        val hits = summary[CACHE_HIT] ?: 0
        val misses = summary[CACHE_MISS] ?: 0
        val total = hits + misses
        return if (total > 0) (hits.toDouble() / total) * 100 else 0.0
    }
}

data class AnalyticsReport(
    val totalEvents: Int = 0,
    val screenViews: Int = 0,
    val aiQueries: Int = 0,
    val cityComparisons: Int = 0,
    val expensesAdded: Int = 0,
    val communityPosts: Int = 0,
    val errors: Int = 0,
    val cacheHitRate: Double = 0.0,
    val topFeatures: Map<String, Int> = emptyMap(),
    val period: String = "24h"
)
