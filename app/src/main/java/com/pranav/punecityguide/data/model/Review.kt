package com.pranav.punecityguide.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "reviews",
    foreignKeys = [
        ForeignKey(
            entity = Attraction::class,
            parentColumns = ["id"],
            childColumns = ["attractionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Review(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val attractionId: Int,
    val userId: String,
    val userName: String,
    val rating: Float,
    val comment: String,
    val timestamp: Long = System.currentTimeMillis(),
    val userAvatarUrl: String = ""
)
