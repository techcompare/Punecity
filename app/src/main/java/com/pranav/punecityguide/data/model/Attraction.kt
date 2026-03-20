package com.pranav.punecityguide.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "attractions",
    indices = [
        androidx.room.Index(value = ["name", "category"], unique = true),
        androidx.room.Index(value = ["rating"]),
        androidx.room.Index(value = ["isFavorite"])
    ]
)
data class Attraction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val description: String,
    val imageUrl: String,
    val category: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val rating: Float = 4.5f,
    val reviewCount: Int = 0,
    val nativeLanguageName: String = "",
    val bestTimeToVisit: String = "Throughout the year",
    val entryFee: String = "Free",
    val openingHours: String = "Open daily",
    val isFavorite: Boolean = false,
    val isVerified: Boolean = false,
    val cityId: String = "pune-01",
    val neighborhood: String = ""
)
