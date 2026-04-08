package com.pranav.punecityguide.model

data class SavedPlace(
    val id: String,
    val name: String,
    val subtitle: String?,
    val imageUrl: String?,
    val category: String? = null,
    val isVisited: Boolean = false,
    val savedAt: Long = System.currentTimeMillis(),
)
