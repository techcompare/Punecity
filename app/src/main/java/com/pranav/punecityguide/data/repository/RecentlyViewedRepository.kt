package com.pranav.punecityguide.data.repository

import com.pranav.punecityguide.data.database.RecentlyViewedDao
import com.pranav.punecityguide.data.model.RecentlyViewed
import kotlinx.coroutines.flow.Flow

class RecentlyViewedRepository(
    private val dao: RecentlyViewedDao,
    private val auditRepository: com.pranav.punecityguide.data.repository.SyncAuditRepository
) {
    fun getLatest(limit: Int = 50): Flow<List<RecentlyViewed>> = dao.getLatest(limit)

    suspend fun upsert(item: RecentlyViewed) {
        dao.upsert(item)
        auditRepository.log("HISTORY_UPDATE", "Pinned attraction ${item.attractionId} to history")
    }

    suspend fun trimTo(keep: Int = 50) = dao.trimTo(keep)
}

