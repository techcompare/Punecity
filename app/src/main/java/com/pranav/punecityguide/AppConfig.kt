package com.pranav.punecityguide

/**
 * Pune Buzz — The Heartbeat of the City
 * Central app configuration constants.
 */
object AppConfig {
    const val APP_NAME = "Pune Buzz"
    const val APP_TAGLINE = "Pune Buzz"
    const val APP_VERSION = "2.0.0"
    const val APP_PACKAGE = "com.pranav.punecityguide"

    // Backend
    const val SUPABASE_TABLE_MESSAGES = "community_messages"
    const val SUPABASE_TABLE_PROFILES = "profiles"
    
    // AI
    const val AI_MODEL = "meta-llama/llama-4-scout-17b-16e-instruct:free"
    const val AI_MAX_TOKENS = 800
    
    // Polling
    const val COMMUNITY_POLL_INTERVAL_MS = 5_000L
    const val PULSE_CACHE_DURATION_MS = 30 * 60 * 1000L
    
    // Limits
    const val MAX_MESSAGE_LENGTH = 1000

    object Supabase {
        const val SUPABASE_URL = "https://ibhmxxcxnuzmyxsasxhf.supabase.co"
        const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImliaG14eGN4bnV6bXl4c2FzeGhmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDI4MzI1ODksImV4cCI6MjA1ODQwODU4OX0.TJiM4AC2i_TSHjWe3tB4Fol_x-Ap1jdkJ-87z9n1EYg"
    }

    object Features {
        const val ENABLE_CRASH_REPORTING = false
        const val ENABLE_ANALYTICS = false
        const val ENABLE_REALTIME_CHAT = true
        const val GITHUB_DATA_URL = "https://raw.githubusercontent.com/techcompare/Punecity/main/data.json"
    }
}
