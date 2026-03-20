package com.pranav.punecityguide.data.service

import android.util.Log
import kotlinx.coroutines.delay
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Production-grade network resilience layer.
 *
 * Provides:
 * - Exponential backoff retry with jitter
 * - Circuit breaker pattern to avoid hammering failing services
 * - Structured error classification for UI-friendly messages
 */
object NetworkResilience {

    private const val TAG = "NetworkResilience"

    // ── Circuit Breaker State ──
    private data class CircuitState(
        var consecutiveFailures: Int = 0,
        var lastFailureMs: Long = 0L,
        var isOpen: Boolean = false
    )

    private val circuits = mutableMapOf<String, CircuitState>()
    private const val CIRCUIT_FAILURE_THRESHOLD = 5
    private const val CIRCUIT_RESET_TIMEOUT_MS = 60_000L // 1 minute cool-down

    /**
     * Execute a suspending network call with automatic retry,
     * exponential backoff, and circuit breaker protection.
     *
     * @param tag      A human-readable label for the circuit (e.g. "google_places")
     * @param maxRetries Maximum retry attempts (default 3)
     * @param initialDelayMs Starting backoff delay
     * @param maxDelayMs Ceiling for the backoff delay
     * @param block    The actual suspending network call
     */
    suspend fun <T> withRetry(
        tag: String,
        maxRetries: Int = 3,
        initialDelayMs: Long = 500L,
        maxDelayMs: Long = 8_000L,
        block: suspend () -> T
    ): Result<T> {
        val circuit = circuits.getOrPut(tag) { CircuitState() }

        // Check circuit breaker
        if (circuit.isOpen) {
            val elapsed = System.currentTimeMillis() - circuit.lastFailureMs
            if (elapsed < CIRCUIT_RESET_TIMEOUT_MS) {
                Log.w(TAG, "Circuit OPEN for '$tag'. Skipping request (${elapsed / 1000}s since last failure).")
                return Result.failure(
                    CircuitOpenException("Service '$tag' is temporarily unavailable. Retry in ${(CIRCUIT_RESET_TIMEOUT_MS - elapsed) / 1000}s.")
                )
            }
            // Allow a probe after timeout
            circuit.isOpen = false
            Log.i(TAG, "Circuit half-open for '$tag'. Allowing probe request.")
        }

        var currentDelay = initialDelayMs
        var lastException: Exception? = null

        for (attempt in 0..maxRetries) {
            try {
                val result = block()
                // Success — reset the circuit
                circuit.consecutiveFailures = 0
                circuit.isOpen = false
                return Result.success(result)
            } catch (e: Exception) {
                lastException = e
                val isRetryable = isRetryableException(e)

                if (!isRetryable || attempt == maxRetries) {
                    circuit.consecutiveFailures++
                    circuit.lastFailureMs = System.currentTimeMillis()
                    if (circuit.consecutiveFailures >= CIRCUIT_FAILURE_THRESHOLD) {
                        circuit.isOpen = true
                        Log.e(TAG, "Circuit OPENED for '$tag' after ${circuit.consecutiveFailures} consecutive failures.")
                    }
                    break
                }

                // Add jitter: ±25% of the current delay
                val jitter = (currentDelay * 0.25 * (Math.random() * 2 - 1)).toLong()
                val sleepMs = (currentDelay + jitter).coerceIn(initialDelayMs, maxDelayMs)

                Log.w(TAG, "Attempt ${attempt + 1}/$maxRetries for '$tag' failed: ${e.message}. Retrying in ${sleepMs}ms...")
                delay(sleepMs)
                currentDelay = (currentDelay * 2).coerceAtMost(maxDelayMs)
            }
        }

        return Result.failure(lastException ?: IOException("Unknown network failure for '$tag'"))
    }

    /**
     * Determines whether an exception is transient and retryable.
     */
    private fun isRetryableException(e: Exception): Boolean {
        return when (e) {
            is SocketTimeoutException -> true
            is UnknownHostException -> true  // DNS resolution failure (e.g. airplane mode toggle)
            is IOException -> true
            is io.ktor.client.plugins.ServerResponseException -> {
                // 5xx errors are retryable; 4xx are not
                val code = e.response.status.value
                code in 500..599
            }
            else -> {
                val msg = e.message?.lowercase().orEmpty()
                "timeout" in msg || "reset" in msg || "connection" in msg
            }
        }
    }

    /** Classifies an exception into a user-friendly message. */
    fun classifyError(e: Throwable): String {
        val msg = e.message?.lowercase().orEmpty()
        return when {
            e is CircuitOpenException -> e.message ?: "Service temporarily unavailable."
            e is SocketTimeoutException || "timeout" in msg -> "Connection timed out. Check your network and try again."
            e is UnknownHostException || "unable to resolve" in msg -> "No internet connection. Please check your network."
            "401" in msg || "unauthorized" in msg -> "Authentication expired. Please sign in again."
            "403" in msg || "forbidden" in msg -> "Access denied. Your session may have expired."
            "429" in msg || "rate limit" in msg -> "Too many requests. Please wait a moment."
            "500" in msg || "internal server" in msg -> "Server error. The team has been notified."
            "502" in msg || "bad gateway" in msg -> "Service is updating. Please try again in a few seconds."
            "503" in msg || "service unavailable" in msg -> "Service is temporarily down for maintenance."
            else -> e.message ?: "An unexpected error occurred."
        }
    }

    /** Reset all circuit breakers (e.g. on manual refresh). */
    fun resetAllCircuits() {
        circuits.values.forEach {
            it.consecutiveFailures = 0
            it.isOpen = false
        }
        Log.i(TAG, "All circuit breakers have been reset.")
    }
}

/** Thrown when a circuit breaker is open and blocking requests. */
class CircuitOpenException(message: String) : Exception(message)
