package com.pranav.punecityguide.data.repository

import android.util.Log
import com.pranav.punecityguide.data.database.SyncAuditDao
import com.pranav.punecityguide.data.model.SyncAuditLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Production-grade audit repository for tracking all backend events.
 *
 * Features:
 * - Non-blocking writes (dispatched to IO)
 * - Automatic log rotation (clears entries older than N days)
 * - Error-resilient logging (never throws from log() — backend audit must not crash the app)
 * - Analytics queries for event frequency and error rates
 */
class SyncAuditRepository(private val dao: SyncAuditDao) {

    private val TAG = "SyncAuditRepository"

    /**
     * Write an audit log entry. This is fire-and-forget safe:
     * if the write fails, it logs to Logcat but never throws.
     */
    suspend fun log(eventType: String, message: String, metadata: String? = null) {
        try {
            withContext(Dispatchers.IO) {
                val entry = SyncAuditLog(
                    eventType = eventType,
                    message = message,
                    metadata = metadata
                )
                dao.log(entry)
            }
        } catch (e: Exception) {
            // Audit logging must NEVER crash the app
            Log.e(TAG, "Failed to write audit log [$eventType]: ${e.message}")
        }
    }

    /** Get recent logs, ordered by most recent first. */
    fun getRecentLogs(limit: Int = 100): Flow<List<SyncAuditLog>> = dao.getRecentLogs(limit)

    /** Get logs filtered by event type. */
    fun getLogsByType(type: String): Flow<List<SyncAuditLog>> = dao.getLogsByType(type)

    /**
     * Remove logs older than [daysToKeep] days.
     * Call this periodically (e.g., on app startup) to prevent unbounded growth.
     */
    suspend fun clearOldLogs(daysToKeep: Int = 7) {
        try {
            withContext(Dispatchers.IO) {
                val threshold = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
                dao.clearOldLogs(threshold)
                Log.d(TAG, "Cleared audit logs older than $daysToKeep days")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear old logs: ${e.message}")
        }
    }

    /** Clear all audit logs (for dev/testing). */
    suspend fun clearAll() {
        try {
            withContext(Dispatchers.IO) { dao.clearAll() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all logs: ${e.message}")
        }
    }

    /**
     * Generate a summary of recent audit events for diagnostics.
     * Groups events by type and counts occurrences.
     */
    suspend fun generateSummary(hoursBack: Int = 24): Map<String, Int> = withContext(Dispatchers.IO) {
        try {
            val cutoff = System.currentTimeMillis() - (hoursBack * 60 * 60 * 1000L)
            val allLogs = dao.getLogsSince(cutoff)
            allLogs.groupBy { it.eventType }.mapValues { it.value.size }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate summary: ${e.message}")
            emptyMap()
        }
    }
}
