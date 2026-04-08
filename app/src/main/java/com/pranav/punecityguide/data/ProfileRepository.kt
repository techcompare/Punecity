package com.pranav.punecityguide.data

import com.pranav.punecityguide.BuildConfig
import com.pranav.punecityguide.model.ProfileData
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

object ProfileRepository {
    suspend fun getProfile(): Result<ProfileData?> {
        if (!SupabaseRest.isConfigured) return Result.success(null)

        val tableCandidates = listOf("profiles", "users")
        tableCandidates.forEach { table ->
            val result = fetchFirstProfile(table)
            if (result.isSuccess) return result
        }

        return Result.failure(IllegalStateException("Profile table not accessible"))
    }

    private suspend fun fetchFirstProfile(table: String): Result<ProfileData?> {
        return try {
            val response = SupabaseRest.client.get("${SupabaseRest.projectUrl}/rest/v1/$table") {
                SupabaseRest.applyAuth(this)
                parameter("select", "*")
                parameter("limit", 1)
            }

            if (!response.status.isSuccess()) {
                return Result.failure(IllegalStateException("Profile request failed: ${response.status}"))
            }

            val rows: List<JsonObject> = response.body()
            val row = rows.firstOrNull() ?: return Result.success(null)
            val profile = ProfileData(
                displayName = row.firstString("display_name", "full_name", "name", "username") ?: "Pune Explorer",
                username = row.firstString("username"),
                email = row.firstString("email"),
                bio = row.firstString("bio", "about"),
            )
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun JsonObject.stringOf(key: String): String? {
    val primitive = this[key] as? JsonPrimitive ?: return null
    return primitive.contentOrNull
}

private fun JsonObject.firstString(vararg keys: String): String? {
    return keys.firstNotNullOfOrNull { key -> stringOf(key) }
}
