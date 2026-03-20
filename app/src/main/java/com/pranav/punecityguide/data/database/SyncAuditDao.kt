package com.pranav.punecityguide.data.database

import androidx.room.*
import com.pranav.punecityguide.data.model.SyncAuditLog
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncAuditDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun log(entry: SyncAuditLog): Long

    @Query("SELECT * FROM sync_audit_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<SyncAuditLog>>

    @Query("SELECT * FROM sync_audit_logs WHERE eventType = :type ORDER BY timestamp DESC")
    fun getLogsByType(type: String): Flow<List<SyncAuditLog>>

    @Query("DELETE FROM sync_audit_logs WHERE timestamp < :threshold")
    suspend fun clearOldLogs(threshold: Long)

    @Query("DELETE FROM sync_audit_logs")
    suspend fun clearAll()

    @Query("SELECT * FROM sync_audit_logs WHERE timestamp >= :sinceMs ORDER BY timestamp DESC")
    suspend fun getLogsSince(sinceMs: Long): List<com.pranav.punecityguide.data.model.SyncAuditLog>

    @Query("SELECT COUNT(*) FROM sync_audit_logs")
    suspend fun getLogCount(): Int
}
