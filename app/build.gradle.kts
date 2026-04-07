import java.util.Properties

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
        versionCode = 8
        versionName = "8"

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }

        fun getProp(name: String, default: String = ""): String {
            return localProperties.getProperty(name) 
                ?: providers.gradleProperty(name).orNull?.takeIf { !it.startsWith("\${") }
                ?: default
        }

        val claudeApiKey = getProp("CLAUDE_API_KEY")
        buildConfigField("String", "CLAUDE_API_KEY", "\"$claudeApiKey\"")

        val claudeModel = getProp("CLAUDE_MODEL", "nvidia/nemotron-3-super-120b-a12b:free")
        buildConfigField("String", "CLAUDE_MODEL", "\"$claudeModel\"")

        val remoteAttractionsPath = getProp("REMOTE_ATTRACTIONS_PATH", "attractions")
        buildConfigField("String", "REMOTE_ATTRACTIONS_PATH", "\"$remoteAttractionsPath\"")

        val supabaseUrl = getProp("SUPABASE_URL")
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")

        val supabaseAnonKey = getProp("SUPABASE_ANON_KEY")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")

        val communitySupabaseUrl = getProp("COMMUNITY_SUPABASE_URL")
        buildConfigField("String", "COMMUNITY_SUPABASE_URL", "\"$communitySupabaseUrl\"")

        val communitySupabaseAnonKey = getProp("COMMUNITY_SUPABASE_ANON_KEY")
        buildConfigField("String", "COMMUNITY_SUPABASE_ANON_KEY", "\"$communitySupabaseAnonKey\"")

        val communityTable = getProp("COMMUNITY_TABLE", "posts")
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
