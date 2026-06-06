package com.attentive.androidsdk.internal.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.random.Random

class RetryInterceptorTest {
    @Test
    fun retriesOn5xxThenSucceeds() {
        val sleeps = mutableListOf<Long>()
        val responses = listOf(response(503), response(503), response(200))
        val chain = scriptedChain(responses)
        val interceptor = newInterceptor(sleeper = { sleeps.add(it) })

        val result = interceptor.intercept(chain)

        assertEquals(200, result.code)
        assertEquals(3, chain.callCount)
        assertEquals(2, sleeps.size)
    }

    @Test
    fun retriesOn429ThenSucceeds() {
        val sleeps = mutableListOf<Long>()
        val chain = scriptedChain(listOf(response(429), response(200)))
        val interceptor = newInterceptor(sleeper = { sleeps.add(it) })

        val result = interceptor.intercept(chain)

        assertEquals(200, result.code)
        assertEquals(1, sleeps.size)
    }

    @Test
    fun honorsRetryAfterSecondsHeaderOn429() {
        val sleeps = mutableListOf<Long>()
        val chain = scriptedChain(listOf(response(429, retryAfter = "2"), response(200)))
        val interceptor = newInterceptor(sleeper = { sleeps.add(it) })

        interceptor.intercept(chain)

        assertEquals(listOf(2_000L), sleeps)
    }

    @Test
    fun honorsRetryAfterRfc1123HeaderOn429() {
        val sleeps = mutableListOf<Long>()
        val target = Date(System.currentTimeMillis() + 5_000L)
        val header =
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("GMT")
            }.format(target)
        val chain = scriptedChain(listOf(response(429, retryAfter = header), response(200)))
        val interceptor = newInterceptor(sleeper = { sleeps.add(it) })

        interceptor.intercept(chain)

        assertEquals(1, sleeps.size)
        assertTrue("expected ~5s, got ${sleeps[0]}ms", sleeps[0] in 4_000L..6_000L)
    }

    @Test
    fun ignoresRetryAfterDateInThePast() {
        val sleeps = mutableListOf<Long>()
        val past = Date(System.currentTimeMillis() - 60_000L)
        val header =
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("GMT")
            }.format(past)
        val chain = scriptedChain(listOf(response(429, retryAfter = header), response(200)))
        val interceptor = newInterceptor(sleeper = { sleeps.add(it) })

        interceptor.intercept(chain)

        assertEquals(listOf(0L), sleeps)
    }

    @Test
    fun clampsNegativeNumericRetryAfter() {
        // A misbehaving server returning "Retry-After: -1" must not produce a negative
        // backoff; Thread.sleep would throw IllegalArgumentException on a negative value.
        val sleeps = mutableListOf<Long>()
        val chain = scriptedChain(listOf(response(429, retryAfter = "-1"), response(200)))
        val interceptor = newInterceptor(sleeper = { sleeps.add(it) })

        interceptor.intercept(chain)

        assertEquals(listOf(0L), sleeps)
    }

    @Test
    fun doesNotRetryOn4xxOtherThan429() {
        val sleeps = mutableListOf<Long>()
        val chain = scriptedChain(listOf(response(404)))
        val interceptor = newInterceptor(sleeper = { sleeps.add(it) })

        val result = interceptor.intercept(chain)

        assertEquals(404, result.code)
        assertEquals(1, chain.callCount)
        assertEquals(0, sleeps.size)
    }

    @Test
    fun givesUpAfterMaxRetriesAndReturnsLastResponse() {
        val sleeps = mutableListOf<Long>()
        val responses = List(6) { response(503) }
        val chain = scriptedChain(responses)
        val interceptor =
            newInterceptor(
                config = RetryConfiguration(maxRetries = 5, jitterRangeMs = 0L..0L),
                sleeper = { sleeps.add(it) },
            )

        val result = interceptor.intercept(chain)

        assertEquals(503, result.code)
        assertEquals(6, chain.callCount) // 1 initial + 5 retries
        assertEquals(5, sleeps.size)
    }

    @Test
    fun retriesOnIoExceptionAndPropagatesIfStillFailing() {
        val sleeps = mutableListOf<Long>()
        val chain = throwingChain(IOException("boom"), times = Int.MAX_VALUE)
        val interceptor =
            newInterceptor(
                config = RetryConfiguration(maxRetries = 3, jitterRangeMs = 0L..0L),
                sleeper = { sleeps.add(it) },
            )

        assertThrows(IOException::class.java) { interceptor.intercept(chain) }
        assertEquals(4, chain.callCount) // 1 initial + 3 retries
        assertEquals(3, sleeps.size)
    }

    @Test
    fun stopsWhenNextDelayWouldExceedCumulativeBudget() {
        val sleeps = mutableListOf<Long>()
        // initial=10s, jitter=0, cap=15s. attempt0=10s ok, attempt1=20s would push cumulative=30s past 15s.
        val responses = listOf(response(503), response(503), response(503))
        val chain = scriptedChain(responses)
        val interceptor =
            newInterceptor(
                config =
                    RetryConfiguration(
                        initialDelayMs = 10_000L,
                        maxRetries = 5,
                        jitterRangeMs = 0L..0L,
                        maxCumulativeDelayMs = 15_000L,
                    ),
                sleeper = { sleeps.add(it) },
            )

        val result = interceptor.intercept(chain)

        assertEquals(503, result.code)
        assertEquals(2, chain.callCount) // initial + one retry, then budget cap stops it
        assertEquals(listOf(10_000L), sleeps)
    }

    @Test
    fun backoffIsExponentialWithJitter() {
        val sleeps = mutableListOf<Long>()
        // jitter forced to constant by seeded Random
        val seededRandom = Random(42)
        val responses = List(5) { response(503) }
        val chain = scriptedChain(responses)
        val interceptor =
            newInterceptor(
                config =
                    RetryConfiguration(
                        initialDelayMs = 1_000L,
                        maxRetries = 4,
                        jitterRangeMs = 0L..0L,
                        maxCumulativeDelayMs = 10_000_000L,
                    ),
                random = seededRandom,
                sleeper = { sleeps.add(it) },
            )

        interceptor.intercept(chain)

        // 1s, 2s, 4s, 8s with no jitter
        assertEquals(listOf(1_000L, 2_000L, 4_000L, 8_000L), sleeps)
    }

    @Test
    fun doesNotRetryWhenCallIsCanceled() {
        val sleeps = mutableListOf<Long>()
        val chain = throwingChain(IOException("Canceled"), times = Int.MAX_VALUE)
        chain.testCall.cancel()
        val interceptor =
            newInterceptor(
                config = RetryConfiguration(maxRetries = 5, jitterRangeMs = 0L..0L),
                sleeper = { sleeps.add(it) },
            )

        assertThrows(IOException::class.java) { interceptor.intercept(chain) }
        assertEquals(1, chain.callCount)
        assertEquals(0, sleeps.size)
    }

    // ---- helpers ----

    private fun newInterceptor(
        config: RetryConfiguration = RetryConfiguration(jitterRangeMs = 0L..0L),
        random: Random = Random(0),
        sleeper: RetryInterceptor.Sleeper,
    ) = RetryInterceptor(config = config, random = random, sleeper = sleeper)

    private fun response(
        code: Int,
        retryAfter: String? = null
    ): Response {
        val builder =
            Response.Builder()
                .request(dummyRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("test")
                .body("".toResponseBody("text/plain".toMediaType()))
        if (retryAfter != null) builder.header("Retry-After", retryAfter)
        return builder.build()
    }

    private val dummyRequest = Request.Builder().url("https://example.test/").build()

    private class ScriptedChain(private val responses: List<Response>) : TestChain() {
        var callCount = 0

        override fun proceed(request: Request): Response {
            val r = responses[callCount.coerceAtMost(responses.lastIndex)]
            callCount += 1
            return r
        }
    }

    private fun scriptedChain(responses: List<Response>) = ScriptedChain(responses)

    private class ThrowingChain(private val error: Throwable, private val times: Int) : TestChain() {
        var callCount = 0

        override fun proceed(request: Request): Response {
            callCount += 1
            if (callCount <= times) throw error
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("ok")
                .body("".toResponseBody("text/plain".toMediaType()))
                .build()
        }
    }

    private fun throwingChain(
        error: Throwable,
        times: Int
    ) = ThrowingChain(error, times)
}
