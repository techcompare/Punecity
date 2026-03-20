package com.pranav.punecityguide.domain.attraction

import com.pranav.punecityguide.data.repository.AttractionRepository
import com.pranav.punecityguide.data.repository.SyncAuditRepository
import com.pranav.punecityguide.data.service.AttractionsRemoteService
import com.pranav.punecityguide.data.service.LocalAttractionProvider
import com.pranav.punecityguide.data.service.NetworkResilience
import com.pranav.punecityguide.data.service.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncAttractionsUseCase(
    private val repository: AttractionRepository,
    private val auditRepository: SyncAuditRepository
) {
    suspend operator fun invoke(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // 1. Initial Seeding: If DB is empty, load the 50+ hand-curated local attractions immediately.
            // This ensures "Version 6 Style" richness without waiting for the network.
            val localCount = repository.getAttractionCount()
            if (localCount == 0) {
                auditRepository.log("SYNC_LOCAL_SEED", "Empty DB detected. Seeding 50+ hand-curated attractions.")
                val seedData = LocalAttractionProvider.getLocalAttractions()
                repository.addAttractions(seedData)
            }

            // 2. Remote Sync: Attempt to update from Supabase for live additions.
            val remoteService = AttractionsRemoteService(SupabaseClient.getHttpClient())

            val remoteResult = NetworkResilience.withRetry("supabase_data", maxRetries = 2) {
                remoteService.fetchAttractions().getOrThrow()
            }

            if (remoteResult.isFailure) {
                val err = remoteResult.exceptionOrNull()!!
                auditRepository.log("SYNC_FAILURE", "Remote sync failed (Normal offline behavior): ${err.message}. Using local seed.")
                
                // If we have local data (even from the seed), return "Success" to the UI.
                return@withContext if (repository.getAttractionCount() > 0) {
                    Result.success(repository.getAttractionCount())
                } else {
                    Result.failure(err)
                }
            }

            val livePlaces = remoteResult.getOrDefault(emptyList())
            if (livePlaces.isNotEmpty()) {
                repository.upsertAllAttractions(livePlaces)
                auditRepository.log("SYNC_SUCCESS", "Synced ${livePlaces.size} live records from Supabase")
                Result.success(livePlaces.size)
            } else {
                Result.success(repository.getAttractionCount())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
