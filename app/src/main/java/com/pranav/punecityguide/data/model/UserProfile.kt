package com.pranav.punecityguide.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * User profile stored in Supabase `profiles` table.
 *
 * Linked to auth.users via `id` (same as auth user ID).
 * Stores user preferences and display information.
 */
@Serializable
data class UserProfile(
    @SerialName("id")
    val id: String,

    @SerialName("display_name")
    val displayName: String? = null,

    @SerialName("avatar_url")
    val avatarUrl: String? = null,

    @SerialName("preferred_currency")
    val preferredCurrency: String = "USD",

    @SerialName("travel_style")
    val travelStyle: String = "MID_RANGE", // BACKPACKER, MID_RANGE, LUXURY

    @SerialName("home_city")
    val homeCity: String? = null,

    @SerialName("bio")
    val bio: String? = null,

    @SerialName("trips_count")
    val tripsCount: Int = 0,

    @SerialName("plans_shared")
    val plansShared: Int = 0,

    @SerialName("is_premium")
    val isPremium: Boolean = false,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null
)
