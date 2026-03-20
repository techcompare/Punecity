package com.pranav.punecityguide.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "sync_audit_logs")
data class SyncAuditLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val eventType: String, // e.g., "SYNC_START", "SYNC_SUCCESS", "SYNC_FAILURE", "AUTH_LOGIN"
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: String? = null // JSON or additional details
)
