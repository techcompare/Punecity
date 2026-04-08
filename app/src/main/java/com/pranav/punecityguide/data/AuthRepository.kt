package com.pranav.punecityguide.data

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

data class AuthOutcome(
    val accessToken: String?,
    val email: String,
    val displayName: String,
    val requiresEmailVerification: Boolean,
)

object AuthRepository {
    suspend fun signIn(email: String, password: String): Result<AuthOutcome> {
        if (!SupabaseRest.isConfigured) {
            return Result.failure(
                IllegalStateException(
                    "Supabase is not configured. Add SUPABASE_URL and SUPABASE_ANON_KEY to the project root local.properties, then Sync Gradle / Rebuild.",
                ),
            )
        }

        return try {
            val response = SupabaseRest.client.post("${SupabaseRest.projectUrl}/auth/v1/token") {
                SupabaseRest.applyAuth(this)
                contentType(ContentType.Application.Json)
                parameter("grant_type", "password")
                setBody(
                    buildJsonObject {
                        put("email", email)
                        put("password", password)
                    },
                )
            }

            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText()
                return Result.failure(
                    IllegalStateException(parseAuthError(errorBody, "Login failed (${response.status})")),
                )
            }

            val root: JsonObject = response.body()
            val token = root.accessTokenFromAuthResponse()
            if (token.isNullOrBlank()) {
                return Result.failure(
                    IllegalStateException(
                        "No access token returned. If email confirmation is ON in Supabase, open the verification link first, then try login.",
                    ),
                )
            }

            val user = root.userFromAuthResponse()
            val resolvedEmail = user?.firstString("email") ?: email
            val displayName = displayNameFromUser(user, resolvedEmail)

            Result.success(
                AuthOutcome(
                    accessToken = token,
                    email = resolvedEmail,
                    displayName = displayName,
                    requiresEmailVerification = false,
                ),
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String, fullName: String = ""): Result<AuthOutcome> {
        if (!SupabaseRest.isConfigured) {
            return Result.failure(
                IllegalStateException(
                    "Supabase is not configured. Add SUPABASE_URL and SUPABASE_ANON_KEY to the project root local.properties, then Sync Gradle / Rebuild.",
                ),
            )
        }

        return try {
            val response = SupabaseRest.client.post("${SupabaseRest.projectUrl}/auth/v1/signup") {
                SupabaseRest.applyAuth(this)
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("email", email)
                        put("password", password)
                        val trimmedName = fullName.trim()
                        if (trimmedName.isNotEmpty()) {
                            put(
                                "data",
                                buildJsonObject {
                                    put("full_name", trimmedName)
                                },
                            )
                        }
                    },
                )
            }

            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText()
                return Result.failure(
                    IllegalStateException(parseAuthError(errorBody, "Sign up failed (${response.status})")),
                )
            }

            val root: JsonObject = response.body()
            val token = root.accessTokenFromAuthResponse()
            val user = root.userFromAuthResponse()
            val resolvedEmail = user?.firstString("email") ?: email
            val displayName = displayNameFromUser(user, resolvedEmail)

            Result.success(
                AuthOutcome(
                    accessToken = token,
                    email = resolvedEmail,
                    displayName = displayName,
                    requiresEmailVerification = token.isNullOrBlank(),
                ),
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun JsonObject.accessTokenFromAuthResponse(): String? {
    firstString("access_token")?.takeIf { it.isNotBlank() }?.let { return it }
    val session = this["session"] as? JsonObject ?: return null
    return session.firstString("access_token")?.takeIf { it.isNotBlank() }
}

private fun JsonObject.userFromAuthResponse(): JsonObject? {
    (this["user"] as? JsonObject)?.let { return it }
    val session = this["session"] as? JsonObject ?: return null
    return session["user"] as? JsonObject
}

private fun displayNameFromUser(user: JsonObject?, resolvedEmail: String): String {
    val meta = user?.get("user_metadata") as? JsonObject
    val metaName = meta?.firstString("full_name", "name", "display_name")
    val fromMeta = metaName?.trim().orEmpty()
    if (fromMeta.isNotEmpty()) return fromMeta
    return resolvedEmail.substringBefore("@").ifBlank { "Pune User" }
}

private fun parseAuthError(rawBody: String, fallback: String): String {
    val trimmed = rawBody.trim()
    if (trimmed.isEmpty()) return fallback

    val asJson = runCatching { SupabaseRest.json.parseToJsonElement(trimmed) as? JsonObject }.getOrNull()
    if (asJson != null) {
        val code = (asJson.firstString("error_code")
            ?: (asJson["error"] as? JsonPrimitive)?.contentOrNull)
            ?.trim()
            ?.lowercase()

        val message = asJson.firstString(
            "msg",
            "message",
            "error_description",
        )?.trim()

        val mapped = when (code) {
            "email_not_confirmed" ->
                "Email not confirmed. Open the Supabase verification email, then try Login."
            "invalid_credentials",
            "invalid_grant",
            -> "Wrong email or password."
            "user_already_registered",
            "email_exists",
            -> "This email is already registered — use Login instead."
            "signup_disabled" -> "Sign-ups are disabled in your Supabase project settings."
            "weak_password" -> (message?.takeIf { it.isNotBlank() } ?: "Password is too weak for this project.")
            else -> null
        }
        if (mapped != null) return mapped
        if (!message.isNullOrBlank() && message != code) return message
        if (!code.isNullOrBlank()) return "$fallback ($code)"
    }
    return fallback
}

private fun JsonObject.stringOf(key: String): String? {
    val primitive = this[key] as? JsonPrimitive ?: return null
    return primitive.contentOrNull
}

private fun JsonObject.firstString(vararg keys: String): String? {
    return keys.firstNotNullOfOrNull { stringOf(it) }
}
