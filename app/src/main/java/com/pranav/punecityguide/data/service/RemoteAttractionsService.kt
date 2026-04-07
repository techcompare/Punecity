package com.pranav.punecityguide.data.service

import com.pranav.punecityguide.data.model.Attraction
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Service to fetch attractions from a remote GitHub JSON file.
 */
class RemoteAttractionsService {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    suspend fun fetchAttractions(url: String): List<Attraction> {
        return try {
            client.get(url).body()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
