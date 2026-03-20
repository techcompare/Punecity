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
    
    // Cache for current token to avoid excessive DataStore reads during a request burst
    private var currentAccessToken: String? = null
    private var lastTokenFetchMs: Long = 0L

    // Supabase base URL — resolved once from AppConfig
    private val baseUrl: String get() = AppConfig.Supabase.SUPABASE_URL
    private val anonKey: String get() = AppConfig.Supabase.SUPABASE_ANON_KEY
    
    fun initialize(context: Context) {
        Logger.i("Initializing SupabaseClient...")
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
                        encodeDefaults = false // Don't encode default Ints/Strings if not provided
                        explicitNulls = false // Don't serialize nulls
                    })
                }
                
                install(io.ktor.client.plugins.api.createClientPlugin("SupabaseAuthPlugin") {
                    on(io.ktor.client.plugins.api.Send) { request ->
                        val targetUrl = request.url.toString()

                        // Only inject Supabase auth headers for requests to OUR backend(s)
                        val mainBase = AppConfig.Supabase.SUPABASE_URL
                        val commBase = AppConfig.Supabase.COMMUNITY_SUPABASE_URL
                        
                        if (targetUrl.startsWith(mainBase) || targetUrl.startsWith(commBase)) {
                            // Refresh cached token periodically (every 10s)
                            val now = System.currentTimeMillis()
                            if (currentAccessToken == null || now - lastTokenFetchMs > 10_000) {
                                currentAccessToken = sessionMgr.getAccessToken()
                                lastTokenFetchMs = now
                            }

                            // Use specific anonKey based on target project
                            val targetAnonKey = if (targetUrl.startsWith(commBase)) {
                                AppConfig.Supabase.COMMUNITY_SUPABASE_ANON_KEY
                            } else {
                                AppConfig.Supabase.SUPABASE_ANON_KEY
                            }

                            val tokenToUse = currentAccessToken ?: targetAnonKey

                            // Always set apikey (Supabase requires it for RLS)
                            request.headers.remove("apikey")
                            request.headers.append("apikey", targetAnonKey)

                            // Set Authorization with the user's session token (or anon fallback)
                            request.headers.remove("Authorization")
                            request.headers.append("Authorization", "Bearer $tokenToUse")
                        }
                        
                        var response = proceed(request)
                        
                        // --- Global 401 Recovery (only for Supabase requests) ---
                        if (targetUrl.startsWith(baseUrl) && response.response.status.value == 401) {
                            Logger.w("SupabaseClient: 401 Unauthorized on ${request.url}. Attempting session refresh...")
                            
                            // 1. Invalidate local cache
                            currentAccessToken = null
                            lastTokenFetchMs = 0L
                            
                            // 2. Try to get a fresh token (triggers internal DataStore refresh)
                            val freshToken = sessionMgr.getAccessToken()
                            
                            if (freshToken != null) {
                                // Retry with the refreshed token
                                currentAccessToken = freshToken
                                lastTokenFetchMs = System.currentTimeMillis()
                                request.headers.remove("Authorization")
                                request.headers.append("Authorization", "Bearer $freshToken")
                                response = proceed(request)
                            } else {
                                // No valid session — fallback to anon key (read-only)
                                Logger.w("SupabaseClient: Session refresh failed. Falling back to anon key.")
                                request.headers.remove("Authorization")
                                request.headers.append("Authorization", "Bearer $anonKey")
                                response = proceed(request)
                            }
                        }
                        
                        response
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
        return httpClient ?: throw IllegalStateException("SupabaseClient not initialized. Call initialize(context) first.")
    }

    fun getSessionManager(): TokenSessionManager {
        return sessionManager ?: throw IllegalStateException("SupabaseClient not initialized.")
    }

    fun invalidateAuthCache() {
        currentAccessToken = null
        lastTokenFetchMs = 0L
    }
}
