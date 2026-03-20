package com.pranav.punecityguide.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class AuthCredentials(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val user: User? = null,
    val session: Session? = null,
    @SerialName("access_token")
    val accessToken: String? = null,
    @SerialName("token_type")
    val tokenType: String? = null,
    @SerialName("expires_in")
    val expiresIn: Long? = null,
    @SerialName("refresh_token")
    val refreshToken: String? = null
) {
    // Resolve session whether nested or at root level.
    fun resolveSession(): Session? {
        return session ?: if (!accessToken.isNullOrEmpty()) {
            Session(
                access_token = accessToken,
                refresh_token = refreshToken,
                expires_in = expiresIn ?: 3600,
                token_type = tokenType ?: "Bearer"
            )
        } else null
    }
}

@Serializable
data class User(
    val id: String,
    val email: String? = null,
    val user_metadata: JsonObject? = null,
    @SerialName("phone")
    val phone: String? = null,
    @SerialName("confirmed_at")
    val confirmedAt: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class Session(
    val access_token: String,
    val refresh_token: String? = null,
    val expires_in: Long = 3600,
    val token_type: String = "Bearer"
)
