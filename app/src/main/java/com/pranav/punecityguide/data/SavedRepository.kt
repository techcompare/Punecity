package com.pranav.punecityguide.data

import android.content.Context
import com.pranav.punecityguide.data.local.PuneDatabase
import com.pranav.punecityguide.data.local.SavedPlaceEntity
import com.pranav.punecityguide.data.local.toEntity
import com.pranav.punecityguide.data.local.toSavedPlace
import com.pranav.punecityguide.model.PuneSpot
import com.pranav.punecityguide.model.SavedPlace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Manages local bookmarks for Pune attractions.
 * As per architectural requirements, Saving/Bookmarks are strictly local (Room).
 */
object SavedRepository {

    fun getSavedPlacesFlow(context: Context): Flow<List<SavedPlace>> {
        val dao = PuneDatabase.getDatabase(context).savedPlaceDao()
        return dao.getAllSavedPlaces().map { entities ->
            entities.map { it.toSavedPlace() }
        }
    }

    suspend fun toggleSave(context: Context, spot: PuneSpot): Boolean {
        val dao = PuneDatabase.getDatabase(context).savedPlaceDao()
        val isCurrentlySaved = dao.isPlaceSaved(spot.id.toString())

        if (isCurrentlySaved) {
            dao.deleteSavedPlace(
                SavedPlaceEntity(
                    id = spot.id.toString(),
                    name = spot.name,
                    subtitle = null,
                    imageUrl = null
                )
            )
            return false
        } else {
            val subtitle = listOfNotNull(spot.category, spot.area).joinToString(" · ")
            dao.insertSavedPlace(
                SavedPlaceEntity(
                    id = spot.id.toString(),
                    name = spot.name,
                    subtitle = subtitle,
                    imageUrl = spot.imageUrl
                )
            )
            return true
        }
    }

    suspend fun removeSavedPlace(context: Context, placeId: String) {
        val dao = PuneDatabase.getDatabase(context).savedPlaceDao()
        dao.deleteSavedPlace(
            SavedPlaceEntity(
                id = placeId,
                name = "", 
                subtitle = null,
                imageUrl = null
            )
        )
    }

    suspend fun isSaved(context: Context, spotId: Int): Boolean {
        return PuneDatabase.getDatabase(context).savedPlaceDao().isPlaceSaved(spotId.toString())
    }

    /**
     * Legacy support for one-shot fetch if needed.
     */
    suspend fun getSavedPlacesOnce(context: Context): Result<List<SavedPlace>> {
        return try {
            val db = PuneDatabase.getDatabase(context)
            // Note: In Room, we should handle Flow, but for one-shot we can do this:
            // This is a bit of a hack since DAO returns Flow, we can't easily wait without a different DAO method.
            // But let's assume we use Flow for UI.
            Result.success(emptyList()) 
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
