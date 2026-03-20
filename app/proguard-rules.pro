# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserv all public and protected methods of views
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

# Preserve Kotlin metadata for reflection
-keep class kotlin.Metadata { *; }
-keepclassmembers class ** {
    *** **(android.content.Context, android.util.AttributeSet);
}

# Keep our app classes
-keep class com.pranav.punecityguide.** { *; }
-keep interface com.pranav.punecityguide.** { *; }

# Room database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# Serialization
-keepclassmembers class * {
    *** *_Serializer(...);
}

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Compose
-keep class androidx.compose.** { *; }

# Coil
-keep class coil3.** { *; }
-dontwarn coil3.PlatformContext
-dontwarn coil3.network.ConnectivityChecker
-dontwarn coil3.network.NetworkObserver
