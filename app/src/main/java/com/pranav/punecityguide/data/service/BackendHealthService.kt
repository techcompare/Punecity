package com.pranav.punecityguide.data.service

import android.util.Log
import com.pranav.punecityguide.data.repository.SyncAuditRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.http.takeFrom

/**
 * Centralized backend health diagnostics for CostPilot.
 *
 * Probes:
 * - Supabase Auth
 * - Supabase REST API (data layer)
 * - AI Service (OpenRouter/Anthropic)
 * - Local SQLite (Room)
 * - Currency API (frankfurter.app)
 * - Offline cache health
 * - Background sync status
 */
class BackendHealthService(
    private val auditRepository: SyncAuditRepository
) {
    private val TAG = "BackendHealthService"
    private val httpClient by lazy { SupabaseClient.getHttpClient() }

    @Serializable
    data class HealthReport(
        val overallStatus: String, // "HEALTHY", "DEGRADED", "UNHEALTHY"
        val checks: List<HealthCheck>,
        val timestamp: Long = System.currentTimeMillis(),
        val appVersion: String = com.pranav.punecityguide.AppConfig.APP_VERSION
    )

    @Serializable
    data class HealthCheck(
        val name: String,
        val status: String,   // "UP", "DOWN", "DEGRADED"
        val latencyMs: Long,
        val details: String = ""
    )

    suspend fun runFullHealthCheck(): HealthReport = withContext(Dispatchers.IO) {
        val checks = mutableListOf<HealthCheck>()

        checks.add(probeSupabaseAuth())
        checks.add(probeSupabaseRest())
        checks.add(probeAiService())
        checks.add(probeLocalDatabase())
        checks.add(probeCurrencyApi())
        checks.add(probeOfflineCache())
        checks.add(probeBackgroundSync())

        val downCount = checks.count { it.status == "DOWN" }
        val degradedCount = checks.count { it.status == "DEGRADED" }
        
        val overallStatus = when {
            downCount >= 3 -> "UNHEALTHY"
            downCount >= 1 || degradedCount >= 3 -> "DEGRADED"
            else -> "HEALTHY"
        }

        val report = HealthReport(
            overallStatus = overallStatus,
            checks = checks
        )

        auditRepository.log(
            "HEALTH_CHECK",
            "Overall: $overallStatus | ${checks.size} probes",
            checks.joinToString("; ") { "${it.name}=${it.status}(${it.latencyMs}ms)" }
        )

        Log.i(TAG, "Health check complete: $overallStatus [${checks.size} probes]")
        report
    }

    /**
     * Run a quick health check (critical services only).
     */
    suspend fun runQuickHealthCheck(): HealthReport = withContext(Dispatchers.IO) {
        val checks = mutableListOf<HealthCheck>()
        checks.add(probeSupabaseAuth())
        checks.add(probeLocalDatabase())

        val downCount = checks.count { it.status == "DOWN" }
        val overallStatus = if (downCount > 0) "DEGRADED" else "HEALTHY"

        HealthReport(overallStatus = overallStatus, checks = checks)
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

    private suspend fun probeSupabaseRest(): HealthCheck {
        val start = System.currentTimeMillis()
        return try {
            val url = com.pranav.punecityguide.AppConfig.Supabase.SUPABASE_URL
            if (url.isBlank()) {
                return HealthCheck("Supabase REST", "DOWN", 0, "URL not configured")
            }

            val response = httpClient.get("$url/rest/v1/city_costs?select=cityName&limit=1") {
                headers.append("apikey", com.pranav.punecityguide.AppConfig.Supabase.SUPABASE_ANON_KEY)
            }
            val latency = System.currentTimeMillis() - start

            if (response.status.value in 200..299) {
                HealthCheck("Supabase REST", if (latency > 3000) "DEGRADED" else "UP", latency, "Data API responsive")
            } else {
                HealthCheck("Supabase REST", "DOWN", latency, "HTTP ${response.status.value}")
            }
        } catch (e: Exception) {
            HealthCheck("Supabase REST", "DOWN", System.currentTimeMillis() - start, e.message ?: "Unknown error")
        }
    }

    private suspend fun probeAiService(): HealthCheck {
        val start = System.currentTimeMillis()
        return try {
            val apiKey = com.pranav.punecityguide.BuildConfig.CLAUDE_API_KEY
            if (apiKey.isBlank()) {
                return HealthCheck("AI Service", "DOWN", 0, "API key not configured")
            }

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

            HealthCheck("AI Service (Anthropic)", "UP", System.currentTimeMillis() - start, "Key configured")
        } catch (e: Exception) {
            HealthCheck("AI Service", "DOWN", System.currentTimeMillis() - start, e.message ?: "Unknown error")
        }
    }

    private suspend fun probeLocalDatabase(): HealthCheck {
        val start = System.currentTimeMillis()
        return try {
            auditRepository.log("HEALTH_PROBE", "Database write probe")
            val latency = System.currentTimeMillis() - start
            HealthCheck("Local Database", if (latency > 500) "DEGRADED" else "UP", latency, "Read-write OK")
        } catch (e: Exception) {
            HealthCheck("Local Database", "DOWN", System.currentTimeMillis() - start, e.message ?: "Unknown error")
        }
    }

    private suspend fun probeCurrencyApi(): HealthCheck {
        val start = System.currentTimeMillis()
        return try {
            val response = httpClient.get("https://api.frankfurter.app/latest?from=USD&to=EUR") {}
            val latency = System.currentTimeMillis() - start

            if (response.status.value in 200..299) {
                HealthCheck("Currency API", if (latency > 3000) "DEGRADED" else "UP", latency, "frankfurter.app")
            } else {
                HealthCheck("Currency API", "DEGRADED", latency, "HTTP ${response.status.value} — using cached rates")
            }
        } catch (e: Exception) {
            HealthCheck("Currency API", "DEGRADED", System.currentTimeMillis() - start, "Offline — using cached rates")
        }
    }

    private fun probeOfflineCache(): HealthCheck {
        return try {
            val cacheSize = ServiceLocator.offlineCacheService.getCacheSize()
            HealthCheck("Offline Cache", "UP", 0, "$cacheSize entries in memory")
        } catch (e: Exception) {
            HealthCheck("Offline Cache", "DEGRADED", 0, e.message ?: "Unknown")
        }
    }

    private fun probeBackgroundSync(): HealthCheck {
        return try {
            val statuses = ServiceLocator.backgroundSyncManager.getWorkStatus()
            val allRunning = statuses.values.all { it == "ENQUEUED" || it == "RUNNING" || it == "SUCCEEDED" }
            val details = statuses.entries.joinToString(", ") { "${it.key.substringAfterLast('_')}=${it.value}" }
            HealthCheck(
                "Background Sync",
                if (allRunning) "UP" else "DEGRADED",
                0,
                details
            )
        } catch (e: Exception) {
            HealthCheck("Background Sync", "DEGRADED", 0, e.message ?: "Unknown")
        }
    }
}

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
