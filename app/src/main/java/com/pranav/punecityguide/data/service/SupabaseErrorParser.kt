package com.pranav.punecityguide.data.service

import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Error classification types from Supabase responses.
 *
 * Used to categorize and handle different types of API errors appropriately.
 */
enum class SupabaseErrorType {
    /** Authentication or authorization failure (401, 403) */
    AUTH_ERROR,

    /** Rate limiting or quota exceeded (429) */
    RATE_LIMIT,

    /** Network-level error (timeout, connection refused) */
    NETWORK_ERROR,

    /** Data validation or constraint violation (400, 422) */
    DATA_ERROR,

    /** Resource not found (404) */
    NOT_FOUND,

    /** Server-side error (500, 502, 503, etc) */
    SERVER_ERROR,

    /** Conflict with existing data (409) */
    CONFLICT,

    /** Realtime subscription error */
    REALTIME_ERROR,

    /** Unknown or unclassified error */
    UNKNOWN
}

/**
 * Detailed information about a Supabase error.
 *
 * @property type The classification of the error
 * @property message Human-readable error message
 * @property code Internal error code from Supabase
 * @property details Additional error context or details
 * @property statusCode HTTP status code if applicable
 * @property isRetryable Whether the operation can be safely retried
 */
data class SupabaseErrorInfo(
    val type: SupabaseErrorType,
    val message: String,
    val code: String? = null,
    val details: String? = null,
    val statusCode: Int? = null,
    val isRetryable: Boolean = false
) {
    override fun toString(): String {
        return "SupabaseError[$type] $message" + 
               (code?.let { " (code: $it)" } ?: "") +
               (details?.let { " - $it" } ?: "")
    }
}

/**
 * Utility for parsing and classifying Supabase error responses.
 *
 * Handles errors from:
 * - REST API responses (HTTP status + JSON body)
 * - Realtime subscription messages
 * - Network-level failures
 *
 * Usage:
 * ```kotlin
 * try {
 *     // Supabase API call
 * } catch (e: Exception) {
 *     val errorInfo = SupabaseErrorParser.parseError(e)
 *     when (errorInfo.type) {
 *         SupabaseErrorType.AUTH_ERROR -> handleAuthError(errorInfo)
 *         SupabaseErrorType.RATE_LIMIT -> handleRateLimit(errorInfo)
 *         else -> handleGenericError(errorInfo)
 *     }
 * }
 * ```
 */
object SupabaseErrorParser {
    private const val TAG = "SupabaseErrorParser"
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Parses an exception and returns detailed error information.
     *
     * @param exception The exception to parse
     * @param statusCode Optional HTTP status code to help with classification
     * @return SupabaseErrorInfo with parsed error details
     */
    fun parseError(exception: Exception, statusCode: Int? = null): SupabaseErrorInfo {
        return when {
            // Network-level errors
            isNetworkError(exception) -> parseNetworkError(exception)

            // HTTP status code errors
            statusCode != null -> parseHttpError(exception, statusCode)

            // Try to parse error message for known patterns
            else -> parseExceptionMessage(exception)
        }
    }

    /**
     * Parses a JSON error response body from Supabase REST API.
     *
     * Expected formats:
     * - `{"error": "message", "error_description": "details"}`
     * - `{"message": "error message", "code": "error_code"}`
     * - `[{"message": "validation error"}]` (array format)
     *
     * @param jsonBody The JSON error response as string
     * @param statusCode HTTP status code from response
     * @return SupabaseErrorInfo with parsed details
     */
    fun parseJsonErrorResponse(jsonBody: String, statusCode: Int = 400): SupabaseErrorInfo {
        return try {
            val parsed = json.parseToJsonElement(jsonBody)

            when {
                // Handle array format errors
                parsed is JsonElement && parsed.jsonObject.contains("errors") -> {
                    val errors = parsed.jsonObject["errors"]
                    val firstError = errors?.jsonObject?.values?.firstOrNull()?.jsonPrimitive?.content 
                        ?: "Unknown validation error"
                    classifyAndCreateError(firstError, statusCode, "validation_error")
                }

                // Handle object format with nested fields
                parsed is JsonElement -> {
                    val obj = parsed.jsonObject

                    val message = obj["error"]?.jsonPrimitive?.content
                        ?: obj["message"]?.jsonPrimitive?.content
                        ?: obj["error_message"]?.jsonPrimitive?.content
                        ?: "Unknown error"

                    val code = obj["code"]?.jsonPrimitive?.content
                        ?: obj["error_code"]?.jsonPrimitive?.content

                    val details = obj["error_description"]?.jsonPrimitive?.content
                        ?: obj["details"]?.jsonPrimitive?.content

                    Log.d(TAG, "Parsed Supabase error: message=$message, code=$code, statusCode=$statusCode")

                    classifyAndCreateError(message, statusCode, code, details)
                }

                else -> createUnknownError(jsonBody, statusCode)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse error JSON: $jsonBody", e)
            createUnknownError(jsonBody, statusCode)
        }
    }

    /**
     * Parses a Realtime subscription error message.
     *
     * @param errorMessage The error message from Realtime
     * @return SupabaseErrorInfo classified as REALTIME_ERROR
     */
    fun parseRealtimeError(errorMessage: String): SupabaseErrorInfo {
        Log.d(TAG, "Parsing Realtime error: $errorMessage")

        val isRetryable = when {
            "timeout" in errorMessage.lowercase() -> true
            "connection" in errorMessage.lowercase() -> true
            "transient" in errorMessage.lowercase() -> true
            else -> false
        }

        return SupabaseErrorInfo(
            type = SupabaseErrorType.REALTIME_ERROR,
            message = errorMessage,
            details = "Realtime subscription failed",
            isRetryable = isRetryable
        )
    }

    /**
     * Parses a network-level exception.
     *
     * @param exception The network exception
     * @return SupabaseErrorInfo classified as NETWORK_ERROR
     */
    private fun parseNetworkError(exception: Exception): SupabaseErrorInfo {
        Log.d(TAG, "Parsing network error: ${exception.message}")

        val isRetryable = when (exception) {
            is java.net.SocketTimeoutException -> true
            is java.net.ConnectException -> true
            is java.io.InterruptedIOException -> true
            else -> false
        }

        return SupabaseErrorInfo(
            type = SupabaseErrorType.NETWORK_ERROR,
            message = exception.message ?: "Network error",
            details = exception.javaClass.simpleName,
            isRetryable = isRetryable
        )
    }

    /**
     * Parses an HTTP error based on status code.
     *
     * @param exception The exception (for message content)
     * @param statusCode HTTP status code
     * @return SupabaseErrorInfo with appropriate type
     */
    private fun parseHttpError(exception: Exception, statusCode: Int): SupabaseErrorInfo {
        Log.d(TAG, "Parsing HTTP error: statusCode=$statusCode, message=${exception.message}")

        // Try to extract message from exception
        val message = exception.message ?: "HTTP Error $statusCode"

        return classifyByStatusCode(message, statusCode)
    }

    /**
     * Parses exception message for known error patterns.
     *
     * @param exception The exception
     * @return SupabaseErrorInfo based on message content
     */
    private fun parseExceptionMessage(exception: Exception): SupabaseErrorInfo {
        val message = exception.message ?: exception.toString()
        Log.d(TAG, "Parsing exception message: $message")

        return when {
            "401" in message || "unauthorized" in message.lowercase() -> 
                SupabaseErrorInfo(
                    type = SupabaseErrorType.AUTH_ERROR,
                    message = "Unauthorized access",
                    statusCode = 401
                )

            "403" in message || "forbidden" in message.lowercase() ->
                SupabaseErrorInfo(
                    type = SupabaseErrorType.AUTH_ERROR,
                    message = "Access forbidden",
                    statusCode = 403
                )

            "429" in message || "rate limit" in message.lowercase() ->
                SupabaseErrorInfo(
                    type = SupabaseErrorType.RATE_LIMIT,
                    message = "Rate limit exceeded",
                    statusCode = 429,
                    isRetryable = true
                )

            "404" in message || "not found" in message.lowercase() ->
                SupabaseErrorInfo(
                    type = SupabaseErrorType.NOT_FOUND,
                    message = "Resource not found",
                    statusCode = 404
                )

            "500" in message || "502" in message || "503" in message ||
            "server error" in message.lowercase() ->
                SupabaseErrorInfo(
                    type = SupabaseErrorType.SERVER_ERROR,
                    message = message,
                    isRetryable = true
                )

            else -> createUnknownError(message)
        }
    }

    /**
     * Classifies an error based on its message, code, and status code.
     *
     * @param message Error message
     * @param statusCode HTTP status code
     * @param code Error code from Supabase
     * @param details Additional error details
     * @return Classified SupabaseErrorInfo
     */
    private fun classifyAndCreateError(
        message: String,
        statusCode: Int,
        code: String? = null,
        details: String? = null
    ): SupabaseErrorInfo {
        val type = classifyByStatusCode(message, statusCode, code)
        val isRetryable = shouldRetry(statusCode, code, message)

        return SupabaseErrorInfo(
            type = type.type,
            message = message,
            code = code,
            details = details,
            statusCode = statusCode,
            isRetryable = isRetryable
        )
    }

    /**
     * Classifies error type by HTTP status code and error code.
     *
     * @param message Error message
     * @param statusCode HTTP status code
     * @param code Optional error code
     * @return Classified SupabaseErrorInfo
     */
    private fun classifyByStatusCode(message: String, statusCode: Int, code: String? = null): SupabaseErrorInfo {
        val type = when (statusCode) {
            401, 403 -> SupabaseErrorType.AUTH_ERROR
            404 -> SupabaseErrorType.NOT_FOUND
            409 -> SupabaseErrorType.CONFLICT
            429 -> SupabaseErrorType.RATE_LIMIT
            in 400..499 -> SupabaseErrorType.DATA_ERROR
            in 500..599 -> SupabaseErrorType.SERVER_ERROR
            else -> when {
                code?.contains("auth", ignoreCase = true) == true -> SupabaseErrorType.AUTH_ERROR
                code?.contains("rate", ignoreCase = true) == true -> SupabaseErrorType.RATE_LIMIT
                code?.contains("unique", ignoreCase = true) == true -> SupabaseErrorType.CONFLICT
                else -> SupabaseErrorType.UNKNOWN
            }
        }

        val isRetryable = shouldRetry(statusCode, code, message)

        return SupabaseErrorInfo(
            type = type,
            message = message,
            code = code,
            statusCode = statusCode,
            isRetryable = isRetryable
        )
    }

    /**
     * Determines if an operation should be retried.
     *
     * @param statusCode HTTP status code
     * @param code Error code
     * @param message Error message
     * @return True if the operation can be safely retried
     */
    private fun shouldRetry(statusCode: Int, code: String?, message: String): Boolean {
        return when {
            // Definite no-retry codes
            statusCode in 400..403 -> false
            statusCode == 404 -> false
            statusCode == 409 -> false

            // Definite retry codes
            statusCode in 408..408 -> true // Request timeout
            statusCode in 429..429 -> true // Rate limit
            statusCode in 500..599 -> true // Server errors

            // Code-based retry logic
            code?.lowercase()?.contains("timeout") == true -> true
            code?.lowercase()?.contains("transient") == true -> true
            code?.lowercase()?.contains("temporarily") == true -> true

            // Message-based heuristics
            message.lowercase().contains("timeout") -> true
            message.lowercase().contains("connection refused") -> true
            message.lowercase().contains("temporarily") -> true

            else -> false
        }
    }

    /**
     * Creates an unknown error info object.
     *
     * @param message Error message or response body
     * @param statusCode Optional HTTP status code
     * @return SupabaseErrorInfo with UNKNOWN type
     */
    private fun createUnknownError(message: String, statusCode: Int? = null): SupabaseErrorInfo {
        Log.w(TAG, "Unknown error: statusCode=$statusCode, message=$message")

        return SupabaseErrorInfo(
            type = SupabaseErrorType.UNKNOWN,
            message = message.take(100), // Limit message length
            statusCode = statusCode
        )
    }

    /**
     * Checks if an exception represents a network-level error.
     *
     * @param exception The exception to check
     * @return True if it's a network error
     */
    private fun isNetworkError(exception: Exception): Boolean {
        return exception is java.net.SocketTimeoutException ||
                exception is java.net.ConnectException ||
                exception is java.io.InterruptedIOException ||
                exception is java.net.UnknownHostException ||
                exception is java.net.SocketException ||
                exception.message?.lowercase()?.contains("connection") == true ||
                exception.message?.lowercase()?.contains("timeout") == true ||
                exception.message?.lowercase()?.contains("network") == true
    }

    /**
     * Extracts HTTP status code from exception message if present.
     *
     * @param exception The exception
     * @return Status code if found, null otherwise
     */
    fun extractStatusCode(exception: Exception): Int? {
        val message = exception.message ?: return null
        
        // Look for patterns like "HTTP 401", "401 Unauthorized", etc.
        val regex = Regex("\\b(\\d{3})\\b")
        return regex.find(message)?.groupValues?.get(1)?.toIntOrNull()
    }
}
