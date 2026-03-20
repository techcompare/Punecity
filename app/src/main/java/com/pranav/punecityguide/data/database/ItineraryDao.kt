package com.pranav.punecityguide.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pranav.punecityguide.data.model.Itinerary
import kotlinx.coroutines.flow.Flow

@Dao
interface ItineraryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(itinerary: Itinerary): Long

    @Query("SELECT * FROM itineraries ORDER BY createdAtEpochMillis DESC")
    fun getAll(): Flow<List<Itinerary>>

    @androidx.room.Update
    suspend fun update(itinerary: Itinerary)

    @Query("DELETE FROM itineraries WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM itineraries")
    suspend fun clear()
}

