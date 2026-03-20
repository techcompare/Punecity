package com.pranav.punecityguide.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recently_viewed")
data class RecentlyViewed(
    @PrimaryKey
    val attractionId: Int,
    val viewedAtEpochMillis: Long,
)

