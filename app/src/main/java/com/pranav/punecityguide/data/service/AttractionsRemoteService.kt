package com.pranav.punecityguide.data.service

import com.pranav.punecityguide.AppConfig
import com.pranav.punecityguide.BuildConfig
import com.pranav.punecityguide.data.model.Attraction
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull

class AttractionsRemoteService(
    private val httpClient: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    suspend fun fetchAttractions(): Result<List<Attraction>> {
        return try {
            val response = httpClient.get("${AppConfig.Supabase.SUPABASE_URL}/rest/v1/${AppConfig.Supabase.TABLE_ATTRACTIONS}") {
                header("apikey", AppConfig.Supabase.SUPABASE_ANON_KEY)
                header("Authorization", "Bearer ${AppConfig.Supabase.SUPABASE_ANON_KEY}")
                parameter("select", "*")
                parameter("order", "rating.desc")
            }

            if (!response.status.isSuccess()) {
                return Result.failure(IllegalStateException("Remote attractions fetch failed (${response.status.value})"))
            }

            val body = response.bodyAsText()
            val root = json.parseToJsonElement(body)
            val rows = (root as? JsonArray).orEmpty()
            val mapped = rows.mapNotNull { it.toAttractionOrNull() }
            if (mapped.isEmpty()) {
                Result.failure(IllegalStateException("Remote attractions response was empty or invalid"))
            } else {
                Result.success(mapped)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun JsonElement.toAttractionOrNull(): Attraction? {
        val obj = this as? JsonObject ?: return null
        val id = obj.int("id") ?: 0
        val name = obj.string("name") ?: return null
        val description = obj.string("description") ?: ""
        val imageUrl = obj.string("image_url") ?: obj.string("imageUrl") ?: ""
        val category = obj.string("category") ?: "General"
        val latitude = obj.double("latitude") ?: 0.0
        val longitude = obj.double("longitude") ?: 0.0
        val rating = obj.float("rating") ?: 0f
        val reviewCount = obj.int("review_count") ?: obj.int("reviewCount") ?: 0
        val nativeLanguageName = obj.string("native_language_name") ?: obj.string("nativeLanguageName") ?: ""
        val bestTimeToVisit = obj.string("best_time_to_visit") ?: obj.string("bestTimeToVisit") ?: "Throughout the year"
        val entryFee = obj.string("entry_fee") ?: obj.string("entryFee") ?: "Free"
        val openingHours = obj.string("opening_hours") ?: obj.string("openingHours") ?: "Open daily"

        return Attraction(
            id = id,
            name = name,
            description = description,
            imageUrl = imageUrl,
            category = category,
            latitude = latitude,
            longitude = longitude,
            rating = rating,
            reviewCount = reviewCount,
            nativeLanguageName = nativeLanguageName,
            bestTimeToVisit = bestTimeToVisit,
            entryFee = entryFee,
            openingHours = openingHours,
            isFavorite = false,
            isVerified = true
        )
    }

    private fun JsonObject.string(key: String): String? {
        val p = this[key] as? JsonPrimitive ?: return null
        return p.content.takeIf { it.isNotBlank() }
    }

    private fun JsonObject.int(key: String): Int? {
        val p = this[key] as? JsonPrimitive ?: return null
        return p.intOrNull
    }

    private fun JsonObject.float(key: String): Float? {
        val p = this[key] as? JsonPrimitive ?: return null
        return p.floatOrNull
    }

    private fun JsonObject.double(key: String): Double? {
        val p = this[key] as? JsonPrimitive ?: return null
        return p.content.toDoubleOrNull()
    }
}
