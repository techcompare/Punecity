package com.pranav.punecityguide.data.repository

import com.pranav.punecityguide.data.database.ItineraryDao
import com.pranav.punecityguide.data.model.Itinerary
import kotlinx.coroutines.flow.Flow

class ItineraryRepository(
    private val dao: ItineraryDao,
    private val auditRepository: com.pranav.punecityguide.data.repository.SyncAuditRepository
) {
    fun getAll(): Flow<List<Itinerary>> = dao.getAll()

    suspend fun insert(itinerary: Itinerary): Long {
        val id = dao.insert(itinerary)
        auditRepository.log("ITINERARY_CREATE", "Created itinerary '${itinerary.title}'", "ID: $id")
        return id
    }

    suspend fun update(itinerary: Itinerary) {
        dao.update(itinerary)
        auditRepository.log("ITINERARY_UPDATE", "Updated itinerary '${itinerary.title}'", "ID: ${itinerary.id}")
    }

    suspend fun delete(id: Int) {
        dao.delete(id)
        auditRepository.log("ITINERARY_DELETE", "Deleted itinerary ID: $id")
    }

    suspend fun clear() {
        dao.clear()
        auditRepository.log("ITINERARY_CLEAR", "Cleared all itineraries from local DB")
    }
}

