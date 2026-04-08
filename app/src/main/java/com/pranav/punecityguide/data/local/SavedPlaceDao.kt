package com.pranav.punecityguide.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPlaceDao {
    @Query("SELECT * FROM saved_places ORDER BY timestamp DESC")
    fun getAllSavedPlaces(): Flow<List<SavedPlaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedPlace(place: SavedPlaceEntity)

    @Delete
    suspend fun deleteSavedPlace(place: SavedPlaceEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_places WHERE id = :placeId)")
    suspend fun isPlaceSaved(placeId: String): Boolean
}
