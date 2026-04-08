package com.pranav.punecityguide.model

data class ProfileData(
    val displayName: String,
    val username: String?,
    val email: String?,
    val bio: String?,
    val profilePhotoUri: String? = null,
)
