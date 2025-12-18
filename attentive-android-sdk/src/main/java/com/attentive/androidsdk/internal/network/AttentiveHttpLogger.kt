package com.attentive.androidsdk.internal.network

import android.annotation.SuppressLint
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.net.URLDecoder

/**
 * Custom HTTP logger that formats headers, URL parameters, and JSON payloads for better readability.
 * Each request/response pair is assigned a unique ID for easy correlation.
 */
internal class AttentiveHttpLogger : HttpLoggingInterceptor.Logger {
    private val headerBuffer = mutableListOf<String>()
    private var isCollectingHeaders = false
    private var requestCounter = 0

    // Map to track request IDs by URL (for matching responses to requests)
    private val urlToRequestIdMap = mutableMapOf<String, String>()

    // ThreadLocal to track current request ID per thread
    private val currentRequestId = ThreadLocal<String?>()

    override fun log(message: String) {
        when {
            // Request start - generate new request ID
            message.startsWith("-->") && !message.startsWith("--> END") -> {
                flushHeaders()
                val requestId = generateRequestId()
                currentRequestId.set(requestId)

                // Extract and store URL for this request
                extractUrl(message)?.let { url ->
                    synchronized(urlToRequestIdMap) {
                        urlToRequestIdMap[url] = requestId
                    }
                }

                Timber.tag("OkHttp").i("[$requestId] $message")
                extractAndLogUrlParams(message)
                isCollectingHeaders = true
            }
            // Response start - look up request ID by URL
            message.startsWith("<--") && !message.startsWith("<-- END") -> {
                flushHeaders()
                val requestId = extractUrl(message)?.let { url ->
                    synchronized(urlToRequestIdMap) {
                        urlToRequestIdMap[url]
                    }
                } ?: currentRequestId.get()

                currentRequestId.set(requestId)
                Timber.tag("OkHttp").i("[$requestId] $message")
                isCollectingHeaders = true
            }
            // End of request/response
            message.startsWith("--> END") || message.startsWith("<-- END") -> {
                flushHeaders()
                val requestId = currentRequestId.get()
                Timber.tag("OkHttp").i("[$requestId] $message")
                isCollectingHeaders = false

                // Clean up after response completes
                if (message.startsWith("<-- END")) {
                    extractUrl(message)?.let { url ->
                        synchronized(urlToRequestIdMap) {
                            urlToRequestIdMap.remove(url)
                        }
                    }
                    currentRequestId.remove()
                }
            }
            // JSON body
            message.startsWith("{") || message.startsWith("[") -> {
                flushHeaders()
                logFormattedJson(message)
                isCollectingHeaders = false
            }
            // Headers
            isCollectingHeaders && message.contains(": ") -> {
                headerBuffer.add(message)
            }
            // Everything else
            else -> {
                flushHeaders()
                val requestId = currentRequestId.get()
                Timber.tag("OkHttp").i("[$requestId] $message")
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun generateRequestId(): String {
        synchronized(this) {
            requestCounter++
            return String.format("REQUEST-ID-%04d", requestCounter)
        }
    }

    private fun extractUrl(message: String): String? {
        return try {
            // Extract URL from messages like:
            // "--> POST https://example.com/path?params"
            // "<-- 200 https://example.com/path?params (123ms)"
            val parts = message.split(" ")
            when {
                message.startsWith("-->") && parts.size >= 3 -> parts[2]
                message.startsWith("<--") && parts.size >= 3 -> {
                    // Find the URL (it's the part that starts with http)
                    parts.firstOrNull { it.startsWith("http") }?.substringBefore("(")?.trim()
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun flushHeaders() {
        if (headerBuffer.isNotEmpty()) {
            // Group headers together, 3 per line for readability
            val grouped = headerBuffer.chunked(3).joinToString("\n") { group ->
                "  ${group.joinToString(" | ")}"
            }
            val requestId = currentRequestId.get()
            Timber.tag("OkHttp").i("[$requestId] Headers:\n$grouped")
            headerBuffer.clear()
        }
    }

    private fun extractAndLogUrlParams(message: String) {
        try {
            // Extract URL from message like "--> POST https://example.com/path?param1=value1&param2=value2"
            val url = extractUrl(message)
            if (url != null && url.contains("?")) {
                val queryString = url.substringAfter("?")
                val params = parseQueryParams(queryString)

                if (params.isNotEmpty()) {
                    val formattedParams = params.entries.joinToString("\n") { (key, value) ->
                        "  $key = $value"
                    }
                    val requestId = currentRequestId.get()
                    Timber.tag("OkHttp").i("[$requestId] URL Parameters:\n$formattedParams")
                }
            }
        } catch (e: Exception) {
            // If parsing fails, silently continue
        }
    }

    private fun parseQueryParams(queryString: String): Map<String, String> {
        return queryString.split("&")
            .mapNotNull { param ->
                val parts = param.split("=", limit = 2)
                if (parts.size == 2) {
                    try {
                        URLDecoder.decode(parts[0], "UTF-8") to URLDecoder.decode(parts[1], "UTF-8")
                    } catch (e: Exception) {
                        parts[0] to parts[1]
                    }
                } else {
                    null
                }
            }
            .toMap()
    }

    private fun logFormattedJson(json: String) {
        try {
            val formatted = when {
                json.trimStart().startsWith("{") -> {
                    JSONObject(json).toString(2)
                }
                json.trimStart().startsWith("[") -> {
                    JSONArray(json).toString(2)
                }
                else -> json
            }
            val requestId = currentRequestId.get()
            Timber.tag("OkHttp").i("[$requestId] Payload:\n$formatted")
        } catch (e: JSONException) {
            // If JSON parsing fails, log as-is
            val requestId = currentRequestId.get()
            Timber.tag("OkHttp").i("[$requestId] $json")
        }
    }
}
