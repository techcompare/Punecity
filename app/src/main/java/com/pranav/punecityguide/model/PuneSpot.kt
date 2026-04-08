package com.pranav.punecityguide.model

data class PuneSpot(
    val id: Int,
    val name: String,
    val category: String?,
    val area: String?,
    val description: String?,
    val bestTime: String?,
    val rating: Double?,
    val reviewCount: Int?,
    val imageUrl: String?,
    val tags: List<String>,
)
