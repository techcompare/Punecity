package com.pranav.punecityguide

object AppConfig {
    const val APP_NAME = "Pune City Guide"
    const val APP_VERSION = "1.0"
    const val APP_VERSION_CODE = 1
    const val MIN_SDK = 24
    const val TARGET_SDK = 36
    
    object Database {
        const val DATABASE_NAME = "pune_city_database"
        const val DATABASE_VERSION = 7
    }
    
    object UI {
        const val ATTRACTIONS_PAGE_SIZE = 10
        const val ANIMATION_DURATION_SHORT = 200L
        const val ANIMATION_DURATION_MEDIUM = 400L
        const val ANIMATION_DURATION_LONG = 600L
    }
    
    object API {
        // Placeholder for future API endpoints
        const val BASE_URL = "https://api.punecityguide.com/"
        const val TIMEOUT = 15000L
    }
    
    object Supabase {
        val SUPABASE_URL = BuildConfig.SUPABASE_URL.trim().removeSuffix("/")
        val SUPABASE_ANON_KEY = BuildConfig.SUPABASE_ANON_KEY
        val COMMUNITY_SUPABASE_URL = BuildConfig.COMMUNITY_SUPABASE_URL.trim().removeSuffix("/")
        val COMMUNITY_SUPABASE_ANON_KEY = BuildConfig.COMMUNITY_SUPABASE_ANON_KEY
        val TABLE_POSTS = BuildConfig.COMMUNITY_TABLE.ifBlank { "posts" }
        val TABLE_PROFILES = "profiles"
        val TABLE_LOUNGE = "lounge_messages"
        val TABLE_ATTRACTIONS = BuildConfig.REMOTE_ATTRACTIONS_PATH.ifBlank { "attractions" }
    }
    
    object Features {
        const val ENABLE_CRASH_REPORTING = true
        const val ENABLE_ANALYTICS = true
        const val ENABLE_OFFLINE_MODE = true
    }
}
