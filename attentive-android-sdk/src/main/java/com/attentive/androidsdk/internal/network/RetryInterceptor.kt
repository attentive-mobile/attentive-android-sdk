package com.attentive.androidsdk.internal.network

import androidx.annotation.RestrictTo
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.pow
import kotlin.random.Random

@RestrictTo(RestrictTo.Scope.LIBRARY)
class RetryInterceptor(
    private val config: RetryConfiguration = RetryConfiguration(),
    private val random: Random = Random.Default,
    private val sleeper: Sleeper = Sleeper { Thread.sleep(it) },
) : Interceptor {

    fun interface Sleeper {
        fun sleep(millis: Long)
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        var cumulativeDelayMs = 0L

        while (true) {
            val outcome = runCatching { chain.proceed(chain.request()) }
            val response = outcome.getOrNull()
            val error = outcome.exceptionOrNull()

            val isNetworkError = error is IOException
            val isRetryableHttp = response != null && (response.code == 429 || response.code in 500..599)
            val shouldRetry = (isNetworkError || isRetryableHttp) && attempt < config.maxRetries

            if (!shouldRetry) {
                if (error != null) throw error
                return response!!
            }

            val backoffMs = if (response?.code == 429) {
                parseRetryAfterMs(response) ?: computeBackoffMs(attempt)
            } else {
                computeBackoffMs(attempt)
            }

            val nextCumulative = cumulativeDelayMs + backoffMs
            if (nextCumulative > config.maxCumulativeDelayMs) {
                Timber.w(
                    "Retry budget exhausted (cumulative=%dms cap=%dms). Returning last result.",
                    nextCumulative,
                    config.maxCumulativeDelayMs,
                )
                if (error != null) throw error
                return response!!
            }

            // Close the unsuccessful response before retrying — OkHttp requires it.
            response?.close()

            Timber.i(
                "Retrying request (attempt=%d, backoff=%dms, status=%s)",
                attempt + 1,
                backoffMs,
                response?.code?.toString() ?: error?.javaClass?.simpleName ?: "unknown",
            )

            sleeper.sleep(backoffMs)
            cumulativeDelayMs = nextCumulative
            attempt += 1
        }
    }

    private fun computeBackoffMs(attempt: Int): Long {
        val exponential = config.initialDelayMs * 2.0.pow(attempt)
        val jitter = random.nextLong(
            config.jitterRangeMs.first,
            config.jitterRangeMs.last + 1,
        )
        return max(0L, exponential.toLong() + jitter)
    }

    private fun parseRetryAfterMs(response: Response): Long? {
        val header = response.header("Retry-After") ?: return null
        header.toLongOrNull()?.let { return it * 1000L }
        val parsed = retryAfterDateFormat.get()!!.parse(header, ParsePosition(0)) ?: return null
        return max(0L, parsed.time - System.currentTimeMillis())
    }

    companion object {
        private val retryAfterDateFormat = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat =
                SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("GMT")
                }
        }

        // Exposed for tests that need a deterministic clock for RFC-1123 parsing.
        @JvmStatic
        internal fun parseRfc1123(header: String): Date? =
            retryAfterDateFormat.get()!!.parse(header, ParsePosition(0))
    }
}
