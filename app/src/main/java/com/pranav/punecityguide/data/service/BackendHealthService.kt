package com.pranav.punecityguide.data.service

import android.util.Log
import com.pranav.punecityguide.data.repository.SyncAuditRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.http.takeFrom

/**
 * Centralized backend health diagnostics service.
 *
 * Production features:
 * - Probes all critical backend dependencies (Supabase, Google Places, AI, local DB)
 * - Returns structured health status with latency metrics
 * - Integrates with audit log for persistent health history
 * - Designed to be called periodically or on-demand from a "System Health" screen
 */
class BackendHealthService(
    private val auditRepository: SyncAuditRepository
) {
    private val TAG = "BackendHealthService"
    private val httpClient by lazy { SupabaseClient.getHttpClient() }
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class HealthReport(
        val overallStatus: String, // "HEALTHY", "DEGRADED", "UNHEALTHY"
        val checks: List<HealthCheck>,
        val timestamp: Long = System.currentTimeMillis(),
        val cacheStats: String = ""
    )

    @Serializable
    data class HealthCheck(
        val name: String,
        val status: String,   // "UP", "DOWN", "DEGRADED"
        val latencyMs: Long,
        val details: String = ""
    )

    /**
     * Run a full health probe across all backend dependencies.
     */
    suspend fun runFullHealthCheck(): HealthReport = withContext(Dispatchers.IO) {
        val checks = mutableListOf<HealthCheck>()

        checks.add(probeSupabaseAuth())
        checks.add(probeSupabaseData())
        checks.add(probeCommunityService())
        checks.add(probeAiService())
        checks.add(probeLocalDatabase())

        val cacheStats = AttractionCache.stats()

        val downCount = checks.count { it.status == "DOWN" }
        val degradedCount = checks.count { it.status == "DEGRADED" }
        val overallStatus = when {
            downCount >= 3 -> "UNHEALTHY"
            downCount >= 1 || degradedCount >= 2 -> "DEGRADED"
            else -> "HEALTHY"
        }

        val report = HealthReport(
            overallStatus = overallStatus,
            checks = checks,
            cacheStats = cacheStats.toString()
        )

        // Persist the health check result
        auditRepository.log(
            "HEALTH_CHECK",
            "Overall: $overallStatus | ${checks.size} probes | Cache: $cacheStats",
            checks.joinToString("; ") { "${it.name}=${it.status}(${it.latencyMs}ms)" }
        )

        Log.i(TAG, "Health check complete: $overallStatus")
        report
    }

    private suspend fun probeSupabaseAuth(): HealthCheck {
        val start = System.currentTimeMillis()
        return try {
            val url = com.pranav.punecityguide.AppConfig.Supabase.SUPABASE_URL
            if (url.isBlank()) {
                return HealthCheck("Supabase Auth", "DOWN", 0, "URL not configured")
            }

            val response = httpClient.get("$url/auth/v1/settings") {
                headers.append("apikey", com.pranav.punecityguide.AppConfig.Supabase.SUPABASE_ANON_KEY)
            }
            val latency = System.currentTimeMillis() - start

            if (response.status.value in 200..299) {
                HealthCheck("Supabase Auth", if (latency > 3000) "DEGRADED" else "UP", latency)
            } else {
                HealthCheck("Supabase Auth", "DOWN", latency, "HTTP ${response.status.value}")
            }
        } catch (e: Exception) {
            HealthCheck("Supabase Auth", "DOWN", System.currentTimeMillis() - start, e.message ?: "Unknown error")
        }
    }

    private suspend fun probeSupabaseData(): HealthCheck {
        val start = System.currentTimeMillis()
        return try {
            val url = com.pranav.punecityguide.AppConfig.Supabase.SUPABASE_URL
            val key = com.pranav.punecityguide.AppConfig.Supabase.SUPABASE_ANON_KEY
            if (url.isBlank() || key.isBlank()) {
                return HealthCheck("Supabase Data", "DOWN", 0, "URL or key not configured")
            }

            val remotePath = com.pranav.punecityguide.AppConfig.Supabase.TABLE_ATTRACTIONS
            val response = httpClient.get("$url/rest/v1/$remotePath?select=count&limit=1") {
                headers.append("apikey", key)
                headers.append("Authorization", "Bearer $key")
                headers.append("Prefer", "count=exact")
            }
            val latency = System.currentTimeMillis() - start

            if (response.status.value in 200..299) {
                HealthCheck("Supabase Data", if (latency > 4000) "DEGRADED" else "UP", latency, "Table '$remotePath' accessible")
            } else {
                HealthCheck("Supabase Data", "DEGRADED", latency, "HTTP ${response.status.value} on '$remotePath'")
            }
        } catch (e: Exception) {
            HealthCheck("Supabase Data", "DOWN", System.currentTimeMillis() - start, e.message ?: "Unknown error")
        }
    }

    private suspend fun probeCommunityService(): HealthCheck {
        val start = System.currentTimeMillis()
        return try {
            val communityUrl = com.pranav.punecityguide.AppConfig.Supabase.COMMUNITY_SUPABASE_URL
                .ifBlank { com.pranav.punecityguide.AppConfig.Supabase.SUPABASE_URL }
            val communityKey = when {
                com.pranav.punecityguide.AppConfig.Supabase.COMMUNITY_SUPABASE_ANON_KEY.isNotBlank() ->
                    com.pranav.punecityguide.AppConfig.Supabase.COMMUNITY_SUPABASE_ANON_KEY
                communityUrl == com.pranav.punecityguide.AppConfig.Supabase.SUPABASE_URL ->
                    com.pranav.punecityguide.AppConfig.Supabase.SUPABASE_ANON_KEY
                else -> ""
            }

            if (communityUrl.isBlank() || communityKey.isBlank()) {
                return HealthCheck("Community Feed", "DOWN", 0, "Community keys not configured")
            }

            val table = com.pranav.punecityguide.AppConfig.Supabase.TABLE_POSTS
            val response = httpClient.get("$communityUrl/rest/v1/$table?select=id&limit=1") {
                headers.append("apikey", communityKey)
                headers.append("Authorization", "Bearer $communityKey")
            }
            val latency = System.currentTimeMillis() - start

            if (response.status.value in 200..299) {
                HealthCheck("Community Feed", if (latency > 4000) "DEGRADED" else "UP", latency)
            } else {
                HealthCheck("Community Feed", "DEGRADED", latency, "HTTP ${response.status.value}")
            }
        } catch (e: Exception) {
            HealthCheck("Community Feed", "DOWN", System.currentTimeMillis() - start, e.message ?: "Unknown error")
        }
    }

    private suspend fun probeAiService(): HealthCheck {
        val start = System.currentTimeMillis()
        return try {
            val apiKey = com.pranav.punecityguide.BuildConfig.CLAUDE_API_KEY
            if (apiKey.isBlank()) {
                return HealthCheck("AI Service", "DOWN", 0, "CLAUDE_API_KEY not configured")
            }

            // For OpenRouter keys, probe their health endpoint
            if (apiKey.startsWith("sk-or-v1-")) {
                val response = httpClient.get("https://openrouter.ai/api/v1/models") {
                    headers.append("Authorization", "Bearer $apiKey")
                }
                val latency = System.currentTimeMillis() - start
                return if (response.status.value in 200..299) {
                    HealthCheck("AI Service (OpenRouter)", if (latency > 5000) "DEGRADED" else "UP", latency)
                } else {
                    HealthCheck("AI Service (OpenRouter)", "DOWN", latency, "HTTP ${response.status.value}")
                }
            }

            // For direct Anthropic API keys
            HealthCheck("AI Service (Anthropic)", "UP", System.currentTimeMillis() - start, "Key configured – live probe skipped for cost")
        } catch (e: Exception) {
            HealthCheck("AI Service", "DOWN", System.currentTimeMillis() - start, e.message ?: "Unknown error")
        }
    }

    private suspend fun probeLocalDatabase(): HealthCheck {
        val start = System.currentTimeMillis()
        return try {
            // This just checks if the audit repo can write
            auditRepository.log("HEALTH_PROBE", "Database write probe")
            val latency = System.currentTimeMillis() - start
            HealthCheck("Local Database", if (latency > 500) "DEGRADED" else "UP", latency, "Read-write OK")
        } catch (e: Exception) {
            HealthCheck("Local Database", "DOWN", System.currentTimeMillis() - start, e.message ?: "Unknown error")
        }
    }
}

// Extension to add the missing get function used in probes
private suspend fun io.ktor.client.HttpClient.get(
    url: String,
    block: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {}
): io.ktor.client.statement.HttpResponse {
    return this.request(io.ktor.client.request.HttpRequestBuilder().apply {
        this.url.takeFrom(url)
        this.method = io.ktor.http.HttpMethod.Get
        block()
    })
}
