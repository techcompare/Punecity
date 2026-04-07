package com.pranav.punecityguide.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Plan(
    @SerialName("id")
    val id: String,
    @SerialName("title")
    val title: String,
    @SerialName("description")
    val description: String? = null,
    @SerialName("duration")
    val duration: String? = null,
    @SerialName("created_by")
    val createdBy: String,
    @SerialName("is_public")
    val isPublic: Boolean = false,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class PlanPlace(
    @SerialName("id")
    val id: String? = null,
    @SerialName("plan_id")
    val planId: String,
    @SerialName("place_name")
    val placeName: String,
    @SerialName("place_id") // Google Places API ID or internal ID
    val placeId: String? = null,
    @SerialName("order")
    val order: Int,
    @SerialName("description")
    val description: String? = null,
    @SerialName("time_slot")
    val timeSlot: String? = null
)
