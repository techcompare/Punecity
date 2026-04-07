package com.pranav.punecityguide.data.service

import android.content.Context
import com.pranav.punecityguide.AppConfig
import com.pranav.punecityguide.util.Logger

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object SupabaseClient {
    private var httpClient: HttpClient? = null
    private var sessionManager: TokenSessionManager? = null
    private var applicationContext: Context? = null
    
    // Cache for current token
    private var currentAccessToken: String? = null
    private var lastTokenFetchMs: Long = 0L

    fun initialize(context: Context) {
        if (applicationContext == null) {
            applicationContext = context.applicationContext
        }
        if (sessionManager == null) {
            sessionManager = TokenSessionManager(context.applicationContext)
        }
        if (httpClient == null) {
            val sessionMgr = sessionManager!!
            httpClient = HttpClient(Android) {
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = true
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                        encodeDefaults = false
                        explicitNulls = false
                    })
                }
                
                install(io.ktor.client.plugins.api.createClientPlugin("SupabaseAuthPlugin") {
                    on(io.ktor.client.plugins.api.Send) { request ->
                        val targetUrl = request.url.toString()
                        val baseUrl = AppConfig.Supabase.SUPABASE_URL
                        val anonKey = AppConfig.Supabase.SUPABASE_ANON_KEY
                        
                        if (targetUrl.startsWith(baseUrl)) {
                            val now = System.currentTimeMillis()
                            if (currentAccessToken == null || now - lastTokenFetchMs > 10_000) {
                                currentAccessToken = sessionMgr.getAccessToken()
                                lastTokenFetchMs = now
                            }

                            val tokenToUse = currentAccessToken ?: anonKey
                            request.headers.remove("apikey")
                            request.headers.append("apikey", anonKey)
                            request.headers.remove("Authorization")
                            request.headers.append("Authorization", "Bearer $tokenToUse")
                        }
                        
                        proceed(request)
                    }
                })

                engine {
                    connectTimeout = 30_000
                    socketTimeout = 30_000
                }
            }
        }
    }
    
    fun getHttpClient(): HttpClient {
        return httpClient ?: throw IllegalStateException("SupabaseClient not initialized.")
    }

    fun getSessionManager(): TokenSessionManager {
        return sessionManager ?: throw IllegalStateException("SupabaseClient not initialized.")
    }

    fun getContext(): Context {
        return applicationContext ?: throw IllegalStateException("SupabaseClient not initialized.")
    }

    fun invalidateAuthCache() {
        currentAccessToken = null
        lastTokenFetchMs = 0L
    }
}
