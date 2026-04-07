package com.pranav.punecityguide.data.service

import android.content.Context
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * WorkManager-based background sync orchestrator.
 *
 * Schedules and manages:
 * - Periodic expense sync (every 6 hours)
 * - Currency rate refresh (every 12 hours)
 * - Audit log cleanup (daily)
 * - One-shot immediate sync on demand
 *
 * Uses constraints to respect battery and network conditions.
 */
class BackgroundSyncManager(private val context: Context) {

    companion object {
        private const val TAG = "BackgroundSyncManager"
        const val WORK_EXPENSE_SYNC = "costpilot_expense_sync"
        const val WORK_CURRENCY_REFRESH = "costpilot_currency_refresh"
        const val WORK_AUDIT_CLEANUP = "costpilot_audit_cleanup"
        const val WORK_IMMEDIATE_SYNC = "costpilot_immediate_sync"
    }

    private val workManager = WorkManager.getInstance(context)

    /**
     * Schedule all periodic background tasks.
     * Call this once during app initialization.
     */
    fun scheduleAll() {
        scheduleExpenseSync()
        scheduleCurrencyRefresh()
        scheduleAuditCleanup()
        Log.i(TAG, "All periodic work scheduled")
    }

    /**
     * Schedule periodic expense sync (every 6 hours).
     * Only runs when connected to network.
     */
    private fun scheduleExpenseSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<ExpenseSyncWorker>(
            6, TimeUnit.HOURS,
            30, TimeUnit.MINUTES // flex interval
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .addTag(WORK_EXPENSE_SYNC)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_EXPENSE_SYNC,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        Log.d(TAG, "Expense sync scheduled (6h interval)")
    }

    /**
     * Schedule periodic currency rate refresh (every 12 hours).
     */
    private fun scheduleCurrencyRefresh() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<CurrencyRefreshWorker>(
            12, TimeUnit.HOURS,
            1, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .addTag(WORK_CURRENCY_REFRESH)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_CURRENCY_REFRESH,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        Log.d(TAG, "Currency refresh scheduled (12h interval)")
    }

    /**
     * Schedule daily audit log cleanup.
     */
    private fun scheduleAuditCleanup() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<AuditCleanupWorker>(
            24, TimeUnit.HOURS,
            2, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag(WORK_AUDIT_CLEANUP)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_AUDIT_CLEANUP,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        Log.d(TAG, "Audit cleanup scheduled (24h interval)")
    }

    /**
     * Trigger an immediate one-shot sync of all data.
     */
    fun triggerImmediateSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<ImmediateSyncWorker>()
            .setConstraints(constraints)
            .addTag(WORK_IMMEDIATE_SYNC)
            .build()

        workManager.enqueueUniqueWork(
            WORK_IMMEDIATE_SYNC,
            ExistingWorkPolicy.REPLACE,
            request
        )
        Log.i(TAG, "Immediate sync triggered")
    }

    /**
     * Cancel all scheduled work (for sign-out).
     */
    fun cancelAll() {
        workManager.cancelAllWorkByTag(WORK_EXPENSE_SYNC)
        workManager.cancelAllWorkByTag(WORK_CURRENCY_REFRESH)
        workManager.cancelAllWorkByTag(WORK_AUDIT_CLEANUP)
        workManager.cancelAllWorkByTag(WORK_IMMEDIATE_SYNC)
        Log.i(TAG, "All background work cancelled")
    }

    /**
     * Get status of background work for diagnostics.
     */
    fun getWorkStatus(): Map<String, String> {
        val tags = listOf(WORK_EXPENSE_SYNC, WORK_CURRENCY_REFRESH, WORK_AUDIT_CLEANUP)
        val statuses = mutableMapOf<String, String>()
        for (tag in tags) {
            val info = workManager.getWorkInfosByTag(tag).get()
            val status = info.firstOrNull()?.state?.name ?: "NOT_SCHEDULED"
            statuses[tag] = status
        }
        return statuses
    }
}

// ── Workers ──

class ExpenseSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("ExpenseSyncWorker", "Starting expense sync...")
            val sessionManager = SupabaseClient.getSessionManager()
            val userId = sessionManager.getUserId()
            if (userId == null) {
                Log.w("ExpenseSyncWorker", "No user session, skipping sync")
                return@withContext Result.success()
            }

            val repo = ServiceLocator.expenseSyncRepository
            val result = repo.fullSync(userId)

            result.onSuccess {
                Log.i("ExpenseSyncWorker", "Sync complete: $it")
                ServiceLocator.syncAuditRepository.log("EXPENSE_SYNC", "Background sync: $it")
            }.onFailure {
                Log.e("ExpenseSyncWorker", "Sync failed: ${it.message}")
                ServiceLocator.syncAuditRepository.log("EXPENSE_SYNC_FAIL", "Error: ${it.message}")
            }

            if (result.isSuccess) Result.success() else Result.retry()
        } catch (e: Exception) {
            Log.e("ExpenseSyncWorker", "Worker failed", e)
            Result.retry()
        }
    }
}

class CurrencyRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("CurrencyRefreshWorker", "Refreshing currency rates...")
            val success = ServiceLocator.currencyService.forceRefresh()
            if (success) {
                ServiceLocator.syncAuditRepository.log("CURRENCY_REFRESH", "Rates refreshed successfully")
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("CurrencyRefreshWorker", "Refresh failed", e)
            Result.retry()
        }
    }
}

class AuditCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("AuditCleanupWorker", "Cleaning up old audit logs...")
            ServiceLocator.syncAuditRepository.clearOldLogs(daysToKeep = 7)
            Result.success()
        } catch (e: Exception) {
            Log.e("AuditCleanupWorker", "Cleanup failed", e)
            Result.success() // Don't retry cleanup failures
        }
    }
}

class ImmediateSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.i("ImmediateSyncWorker", "Starting immediate full sync...")
            val sessionManager = SupabaseClient.getSessionManager()
            val userId = sessionManager.getUserId()

            // 1. Sync expenses
            if (userId != null) {
                ServiceLocator.expenseSyncRepository.fullSync(userId)
            }

            // 2. Refresh currency rates
            ServiceLocator.currencyService.forceRefresh()

            // 3. Clear stale caches
            ServiceLocator.offlineCacheService.clearAll()

            ServiceLocator.syncAuditRepository.log("IMMEDIATE_SYNC", "Full sync completed")
            Result.success()
        } catch (e: Exception) {
            Log.e("ImmediateSyncWorker", "Immediate sync failed", e)
            ServiceLocator.syncAuditRepository.log("IMMEDIATE_SYNC_FAIL", "Error: ${e.message}")
            Result.failure()
        }
    }
}
