package com.pranav.punecityguide.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "attractions")
@Serializable
data class Attraction(
    @PrimaryKey val id: Int,
    val name: String,
    val category: String,
    val description: String,
    val image: String,
    val rating: Double,
    val latitude: Double,
    val longitude: Double,
    val address: String = "Pune, Maharashtra",
    val tags: List<String> = emptyList(),
    val reviews: Int = 0,
    val visitDuration: String = "1-2 hours",
    val location: String = "Pune, Maharashtra",
    val timings: String = "Open Daily",
    val entryFee: String = "Free",
    val localName: String? = null,
    val bestTime: String? = null,
    val currentCrowdLevel: String? = null, // "Low", "Moderate", "High" or null if unavailable
    val neighborhood: String? = null,
    val budgetFriendly: Boolean? = true,
    val hasWifi: Boolean? = false,
    val isVerified: Boolean = false
)
