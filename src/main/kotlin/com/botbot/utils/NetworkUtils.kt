package com.botbot.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.net.HttpURLConnection
import java.net.URI

suspend fun <T> withRetryAndTimeout(
    name: String,
    times: Int = 3,
    timeoutMillis: Long = 5000,
    initialDelayMillis: Long = 1000,
    maxDelayMillis: Long = 4000,
    factor: Double = 2.0,
    block: suspend () -> T
): T? {
    var currentDelay = initialDelayMillis
    repeat(times) { iteration ->
        try {
            return withTimeout(timeoutMillis) {
                block()
            }
        } catch (e: Exception) {
            println("⚠️ Error in $name (attempt ${iteration + 1}/$times): ${e.message}")
            if (iteration == times - 1) return null
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMillis)
        }
    }
    return null
}

/**
 * Helper to perform a GET request with a timeout using standard Java HttpURLConnection.
 */
fun fetchUrl(url: String, timeoutMillis: Int = 5000): String {
    val connection = URI(url).toURL().openConnection() as HttpURLConnection
    connection.connectTimeout = timeoutMillis
    connection.readTimeout = timeoutMillis
    connection.requestMethod = "GET"
    
    return connection.inputStream.bufferedReader().use { it.readText() }
}
