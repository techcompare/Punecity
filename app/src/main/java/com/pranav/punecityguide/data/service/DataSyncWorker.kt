package com.pranav.punecityguide.data.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pranav.punecityguide.data.database.PuneCityDatabase
import com.pranav.punecityguide.data.repository.AttractionRepository
import com.pranav.punecityguide.data.repository.SyncAuditRepository
import com.pranav.punecityguide.data.service.AttractionsRemoteService
import com.pranav.punecityguide.data.service.SupabaseClient
import com.pranav.punecityguide.data.service.NetworkResilience
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Background worker for synchronizing attraction data.
 *
 * Outstanding features:
 * - Runs in background with WorkManager (respects battery/network constraints)
 * - Merges Google Places and Supabase datasets with deduplication
 * - Uses NetworkResilience for individual service calls
 * - Handles data stale-ness and legacy data cleanup
 * - Logs progress and errors to the persistent Audit Log
 */
class DataSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val TAG = "DataSyncWorker"

    override suspend fun doWork(): Result {
        val database = PuneCityDatabase.getInstance(applicationContext)
        val repository = AttractionRepository(database.attractionDao(), database.recentlyViewedDao())
        val auditRepository = SyncAuditRepository(database.syncAuditDao())

        auditRepository.log("SYNC_BG_START", "Background sync wake-up")

        val syncUseCase = com.pranav.punecityguide.domain.attraction.SyncAttractionsUseCase(repository, auditRepository)

        return try {
            val result = syncUseCase()
            if (result.isSuccess) {
                auditRepository.log("SYNC_BG_SUCCESS", "Background sync completed: ${result.getOrNull()} items")
                // Housekeeping
                auditRepository.clearOldLogs(daysToKeep = 7)
                androidx.work.ListenableWorker.Result.success()
            } else {
                val error = result.exceptionOrNull()
                com.pranav.punecityguide.util.Logger.e("Sync background error", error)
                auditRepository.log("SYNC_BG_RETRY", "Background sync failed: ${error?.message}")
                androidx.work.ListenableWorker.Result.retry()
            }
        } catch (e: Exception) {
            com.pranav.punecityguide.util.Logger.e("Sync worker crash", e)
            auditRepository.log("SYNC_BG_CRASH", "Background sync crashed: ${e.message}")
            androidx.work.ListenableWorker.Result.failure()
        }
    }
}
