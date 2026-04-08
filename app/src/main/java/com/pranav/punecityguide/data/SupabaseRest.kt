package com.pranav.punecityguide.data

import com.pranav.punecityguide.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object SupabaseRest {
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    val client: HttpClient = HttpClient(Android) {
        defaultRequest {
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        }
        install(ContentNegotiation) {
            json(json)
        }
    }

    /** e.g. `https://xxxx.supabase.co` without trailing slash */
    val projectUrl: String
        get() = BuildConfig.SUPABASE_URL.trim().trimEnd('/')

    val isConfigured: Boolean
        get() = projectUrl.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    fun applyAuth(builder: io.ktor.client.request.HttpRequestBuilder, userToken: String? = null) {
        val key = BuildConfig.SUPABASE_ANON_KEY
        builder.header("apikey", key)
        val token = userToken ?: key
        builder.header("Authorization", "Bearer $token")
    }
}
