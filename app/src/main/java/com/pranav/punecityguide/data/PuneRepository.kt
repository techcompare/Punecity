package com.pranav.punecityguide.data

import com.pranav.punecityguide.model.PuneSpot
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

object PuneRepository {
    private const val GITHUB_BLOB_URL = "https://github.com/techcompare/Punecity/blob/main/data.json"

    suspend fun getFeaturedSpots(limit: Int = 1000): Result<List<PuneSpot>> {
        return fetchSpotsFromGithub(limit)
    }

    private suspend fun fetchSpotsFromGithub(limit: Int): Result<List<PuneSpot>> {
        return try {
            val rawUrl = githubBlobToRawUrl(GITHUB_BLOB_URL)
            val response = SupabaseRest.client.get(rawUrl)

            if (!response.status.isSuccess()) {
                return Result.failure(IllegalStateException("GitHub attractions request failed: ${response.status}"))
            }

            val bodyText = response.bodyAsText()
            val root = try {
                SupabaseRest.json.parseToJsonElement(bodyText)
            } catch (e: SerializationException) {
                return Result.failure(IllegalStateException("GitHub attractions payload is not valid JSON"))
            }

            val rows = when (root) {
                is JsonArray -> root.mapNotNull { it as? JsonObject }
                is JsonObject -> {
                    val dataArray = root["data"] as? JsonArray
                    dataArray?.mapNotNull { it as? JsonObject } ?: emptyList()
                }
                else -> emptyList()
            }

            val parsed = rows
                .mapNotNull { row -> row.toPuneSpotOrNull() }
                .take(limit)
            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun githubBlobToRawUrl(blobUrl: String): String {
    val prefix = "https://github.com/"
    if (!blobUrl.startsWith(prefix) || !blobUrl.contains("/blob/")) return blobUrl
    val withoutPrefix = blobUrl.removePrefix(prefix)
    val parts = withoutPrefix.split("/blob/")
    if (parts.size != 2) return blobUrl
    return "https://raw.githubusercontent.com/${parts[0]}/${parts[1]}"
}

private fun JsonObject.toPuneSpotOrNull(): PuneSpot? {
    val id = firstInt("id", "place_id") ?: return null
    val name = firstString("name", "title", "place_name") ?: return null

    return PuneSpot(
        id = id,
        name = name,
        category = firstString("category", "type"),
        area = firstString("location", "area", "address"),
        description = firstString("description", "details"),
        bestTime = firstString("bestTime", "best_time"),
        rating = firstDouble("rating"),
        reviewCount = firstInt("reviews", "review_count"),
        imageUrl = firstString("image", "image_url", "thumbnail"),
        tags = parseTags(),
    )
}

private fun JsonObject.parseTags(): List<String> {
    val tagsElement = this["tags"] ?: return emptyList()
    return when (tagsElement) {
        is JsonArray -> tagsElement.mapNotNull { element ->
            (element as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
        }
        is JsonPrimitive -> tagsElement.contentOrNull
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
        else -> emptyList()
    }
}

private fun JsonObject.stringOf(key: String): String? {
    val primitive = this[key] as? JsonPrimitive ?: return null
    return primitive.contentOrNull
}

private fun JsonObject.intOf(key: String): Int? {
    val primitive = this[key] as? JsonPrimitive ?: return null
    return primitive.intOrNull
}

private fun JsonObject.doubleOf(key: String): Double? {
    val primitive = this[key] as? JsonPrimitive ?: return null
    return primitive.doubleOrNull
}

private fun JsonObject.firstString(vararg keys: String): String? {
    return keys.firstNotNullOfOrNull { key -> stringOf(key) }
}

private fun JsonObject.firstInt(vararg keys: String): Int? {
    return keys.firstNotNullOfOrNull { key -> intOf(key) }
}

private fun JsonObject.firstDouble(vararg keys: String): Double? {
    return keys.firstNotNullOfOrNull { key -> doubleOf(key) }
}
