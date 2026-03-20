package com.pranav.punecityguide.util

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Maps system and network exceptions to user-friendly messages.
 */
object ErrorMapper {
    fun map(throwable: Throwable): String {
        return when (throwable) {
            is UnknownHostException -> "No internet connection. Please check your network."
            is ConnectException -> "Unable to reach the server. It might be down."
            is SocketTimeoutException -> "Request timed out. Please try again later."
            is IOException -> "Network error. Please try again."
            is SecurityException -> "Permission denied."
            else -> throwable.message ?: "An unexpected error occurred."
        }
    }
}
