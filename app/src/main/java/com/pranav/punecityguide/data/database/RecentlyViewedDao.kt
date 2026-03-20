package com.pranav.punecityguide.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pranav.punecityguide.data.model.RecentlyViewed
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentlyViewedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: RecentlyViewed)

    @Query("SELECT * FROM recently_viewed ORDER BY viewedAtEpochMillis DESC LIMIT :limit")
    fun getLatest(limit: Int = 50): Flow<List<RecentlyViewed>>

    @Query("SELECT COUNT(*) FROM recently_viewed")
    fun getCount(): Flow<Int>

    @Query("DELETE FROM recently_viewed WHERE attractionId NOT IN (SELECT attractionId FROM recently_viewed ORDER BY viewedAtEpochMillis DESC LIMIT :keep)")
    suspend fun trimTo(keep: Int = 50)
}

