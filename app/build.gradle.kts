plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.pranav.punecityguide"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pranav.punecityguide"
        minSdk = 24
        targetSdk = 36
        versionCode = 7
        versionName = "7"

        val claudeApiKey = providers.gradleProperty("CLAUDE_API_KEY").orElse("").get()
        buildConfigField("String", "CLAUDE_API_KEY", "\"$claudeApiKey\"")

        val claudeModel = providers.gradleProperty("CLAUDE_MODEL").orElse("nvidia/nemotron-3-super-120b-a12b:free").get()
        buildConfigField("String", "CLAUDE_MODEL", "\"$claudeModel\"")

        val remoteAttractionsPath = providers.gradleProperty("REMOTE_ATTRACTIONS_PATH").orElse("attractions").get()
        buildConfigField("String", "REMOTE_ATTRACTIONS_PATH", "\"$remoteAttractionsPath\"")

        val supabaseUrl = providers.gradleProperty("SUPABASE_URL").get()
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")

        val supabaseAnonKey = providers.gradleProperty("SUPABASE_ANON_KEY").get()
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")

        val communitySupabaseUrl = providers.gradleProperty("COMMUNITY_SUPABASE_URL").get()
        buildConfigField("String", "COMMUNITY_SUPABASE_URL", "\"$communitySupabaseUrl\"")

        val communitySupabaseAnonKey = providers.gradleProperty("COMMUNITY_SUPABASE_ANON_KEY").get()
        buildConfigField("String", "COMMUNITY_SUPABASE_ANON_KEY", "\"$communitySupabaseAnonKey\"")

        val communityTable = providers.gradleProperty("COMMUNITY_TABLE").get()
        buildConfigField("String", "COMMUNITY_TABLE", "\"$communityTable\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    
    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    
    // Material3
    implementation(libs.google.material)
    
    // Navigation
    implementation(libs.androidx.navigation.compose)
    
    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    
    // Image Loading
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    
    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    // Serialization
    implementation(libs.kotlinx.serialization.json)
    
    // Supabase Auth via REST API
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    
    // DataStore for caching
    implementation(libs.androidx.datastore.preferences)

    // WorkManager
    implementation(libs.androidx.work.runtime)

    // ML Kit (on-device OCR)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.text.recognition.devanagari)
    
    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.ktor.client.cio)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
