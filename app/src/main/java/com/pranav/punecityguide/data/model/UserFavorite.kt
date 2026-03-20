package com.pranav.punecityguide.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_favorites",
    foreignKeys = [
        ForeignKey(
            entity = Attraction::class,
            parentColumns = ["id"],
            childColumns = ["attractionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserFavorite(
    @PrimaryKey
    val id: String = "",
    val attractionId: Int,
    val userId: String,
    val addedAt: Long = System.currentTimeMillis()
)
