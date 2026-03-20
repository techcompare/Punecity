package com.pranav.punecityguide.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pranav.punecityguide.data.model.AiTokenQuota
import kotlinx.coroutines.flow.Flow

@Dao
interface AiTokenQuotaDao {
    @Query("SELECT * FROM ai_token_quota WHERE userId = :userId")
    suspend fun getQuota(userId: String): AiTokenQuota?

    @Query("SELECT * FROM ai_token_quota WHERE userId = :userId")
    fun getQuotaFlow(userId: String): Flow<AiTokenQuota?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuota(quota: AiTokenQuota)

    @Update
    suspend fun updateQuota(quota: AiTokenQuota)

    @Query("UPDATE ai_token_quota SET tokensUsed = tokensUsed + 1, date = :today WHERE userId = :userId")
    suspend fun incrementTokenUsage(userId: String, today: String)

    @Query("UPDATE ai_token_quota SET tokensUsed = 0, date = :today WHERE userId = :userId AND date != :today")
    suspend fun resetQuotaIfNewDay(userId: String, today: String)

    @Query("DELETE FROM ai_token_quota WHERE userId = :userId")
    suspend fun deleteQuota(userId: String)
}
