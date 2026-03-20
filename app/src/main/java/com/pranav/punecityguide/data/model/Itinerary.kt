package com.pranav.punecityguide.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "itineraries")
data class Itinerary(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val createdAtEpochMillis: Long,
    val sourceText: String = "",
    val matchedAttractionId: Int? = null,
    val plannedStartEpochMillis: Long? = null,
    val notes: String = "",
)

