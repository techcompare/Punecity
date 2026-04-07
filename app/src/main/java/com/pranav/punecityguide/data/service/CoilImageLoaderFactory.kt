package com.pranav.punecityguide.data.service

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Production-grade Coil image loader with:
 * - User-Agent header injection (critical for Wikipedia/Wikimedia)
 * - Memory & disk caching
 * - Timeouts tuned for mobile networks
 * - Crossfade animations
 */
object CoilImageLoaderFactory {

    private var instance: ImageLoader? = null

    fun createImageLoader(context: Context): ImageLoader {
        instance?.let { return it }

        val userAgentInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "CostPilot/9.0 (Android; Coil)")
                .header("Accept", "image/webp,image/avif,image/apng,image/*,*/*;q=0.8")
                .build()
            chain.proceed(request)
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(userAgentInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val loader = ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
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
                    .directory(File(context.cacheDir, "image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .crossfade(true)
            .build()

        instance = loader
        return loader
    }
}
