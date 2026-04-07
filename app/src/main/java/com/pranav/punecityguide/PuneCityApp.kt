package com.pranav.punecityguide

import android.app.Application
import android.util.Log
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import com.pranav.punecityguide.data.service.ServiceLocator
import com.pranav.punecityguide.data.service.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okio.Path.Companion.toOkioPath

/**
 * CostPilot Application — production-grade initialization.
 *
 * Startup sequence:
 * 1. Initialize SupabaseClient (HTTP + auth)
 * 2. Initialize ServiceLocator (lazy DI container)
 * 3. Schedule background sync workers
 * 4. Clean up stale audit logs
 * 5. Start analytics session
 */
class PuneCityApp : Application(), SingletonImageLoader.Factory {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val startMs = System.currentTimeMillis()

        // Core initialization (synchronous — must complete before any UI)
        SupabaseClient.initialize(this)
        ServiceLocator.initialize(this)

        // Deferred initialization (async — non-blocking)
        appScope.launch {
            try {
                // Schedule background workers
                ServiceLocator.backgroundSyncManager.scheduleAll()

                // Clean up old audit logs
                ServiceLocator.syncAuditRepository.clearOldLogs(daysToKeep = 7)

                // Start analytics session
                ServiceLocator.analyticsService.startSession()

                // Pre-warm currency cache
                ServiceLocator.currencyService.getRates("USD")

                // Update engagement streak + missions
                ServiceLocator.preferenceManager.updateStreak()

                val bootMs = System.currentTimeMillis() - startMs
                ServiceLocator.syncAuditRepository.log(
                    "APP_BOOT",
                    "CostPilot ${AppConfig.APP_VERSION} started in ${bootMs}ms"
                )
                Log.i("CostPilot", "Boot complete in ${bootMs}ms")
            } catch (e: Exception) {
                Log.e("CostPilot", "Deferred init error: ${e.message}", e)
            }
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "image/webp,image/avif,image/apng,image/*,*/*;q=0.8")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(okHttpClient))
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizePercent(0.05)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
