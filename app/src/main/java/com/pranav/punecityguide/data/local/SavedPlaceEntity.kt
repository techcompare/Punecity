package com.pranav.punecityguide.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pranav.punecityguide.model.SavedPlace

@Entity(tableName = "saved_places")
data class SavedPlaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val subtitle: String?,
    val imageUrl: String?,
    val timestamp: Long = System.currentTimeMillis()
)

fun SavedPlaceEntity.toSavedPlace() = SavedPlace(
    id = id,
    name = name,
    subtitle = subtitle,
    imageUrl = imageUrl
)

fun SavedPlace.toEntity() = SavedPlaceEntity(
    id = id,
    name = name,
    subtitle = subtitle,
    imageUrl = imageUrl
)
