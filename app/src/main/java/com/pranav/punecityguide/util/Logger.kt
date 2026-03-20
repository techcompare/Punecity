package com.pranav.punecityguide.util

import android.util.Log
import com.pranav.punecityguide.AppConfig

/**
 * Production-ready logging wrapper.
 * In a real production app, this would integrate with Timber and Crashlytics.
 */
object Logger {
    private const val TAG = "PuneCityApp"

    fun d(message: String) {
        if (com.pranav.punecityguide.BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
        if (AppConfig.Features.ENABLE_CRASH_REPORTING) {
            // Placeholder: FirebaseCrashlytics.getInstance().recordException(throwable)
        }
    }

    fun i(message: String) {
        Log.i(TAG, message)
    }

    fun w(message: String) {
        Log.w(TAG, message)
    }
}
