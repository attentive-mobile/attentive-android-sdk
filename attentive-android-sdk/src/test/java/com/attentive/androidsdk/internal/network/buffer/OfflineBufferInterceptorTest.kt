package com.attentive.androidsdk.internal.network.buffer

import android.content.Context
import com.attentive.androidsdk.internal.network.TestChain
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import java.io.IOException

class OfflineBufferInterceptorTest {
    private lateinit var queue: FakeBufferedRequestQueue
    private lateinit var interceptor: OfflineBufferInterceptor
    private lateinit var context: Context
    private var scheduleFlushCount: Int = 0

    @Before
    fun setUp() {
        queue = FakeBufferedRequestQueue()
        context = mock(Context::class.java)
        scheduleFlushCount = 0
        interceptor =
            OfflineBufferInterceptor(
                queue = queue,
                context = context,
                config = BufferConfiguration(maxEntries = 3),
                scheduleFlush = { scheduleFlushCount += 1 },
            )
    }

    @Test
    fun preflightInsertedRowIsPendingNotReady() {
        val request = postRequest("https://example.test/e", "hi".toByteArray())
        val chain =
            object : TestChain() {
                override fun request(): Request = request

                override fun proceed(request: Request): Response {
                    // At this moment the row exists but must be pending=true so the
                    // FlushWorker doesn't peek and replay an in-flight request.
                    assertEquals(1, queue.entries.size)
                    assertTrue("Row must be pending while in-flight", queue.entries[0].pending)
                    return response(request, 204)
                }
            }

        interceptor.intercept(chain)
        assertEquals(0, queue.entries.size)
    }

    @Test
    fun ioExceptionMarksRowReady() {
        val request = postRequest("https://example.test/e", "hi".toByteArray())

        assertThrows(IOException::class.java) {
            interceptor.intercept(ThrowingChain(request, IOException("network down")))
        }
        assertEquals(1, queue.entries.size)
        assertTrue("Row must be flipped to ready after retry exhaustion", !queue.entries[0].pending)
        assertEquals(1, scheduleFlushCount)
    }

    @Test
    fun serverError5xxMarksRowReady() {
        val request = postRequest("https://example.test/e", "hi".toByteArray())

        interceptor.intercept(ScriptedChain(request, response(request, 503)))

        assertEquals(1, queue.entries.size)
        assertTrue(!queue.entries[0].pending)
        assertEquals(1, scheduleFlushCount)
    }

    @Test
    fun preflightEnqueuesBeforeProceed() {
        val request = postRequest("https://example.test/e", "hi".toByteArray())
        // Throwing chain that asserts the row is already present at proceed time.
        val chain =
            object : TestChain() {
                override fun request(): Request = request

                override fun proceed(request: Request): Response {
                    assertEquals(1, queue.entries.size)
                    throw IOException("network down")
                }
            }

        assertThrows(IOException::class.java) { interceptor.intercept(chain) }
        assertEquals(1, queue.entries.size)
        assertEquals(1, scheduleFlushCount)
    }

    @Test
    fun success2xxDeletesRow() {
        val request = postRequest("https://example.test/e", "hi".toByteArray())

        val response = interceptor.intercept(ScriptedChain(request, response(request, 204)))

        assertEquals(204, response.code)
        assertEquals(0, queue.entries.size)
        // No flush scheduled on the happy path — that would race the worker against the
        // in-progress original call and double-send. SDK init handles cold-start drain.
        assertEquals(0, scheduleFlushCount)
    }

    @Test
    fun clientError4xxDeletesRow() {
        for (code in listOf(400, 401, 404)) {
            queue.entries.clear()
            scheduleFlushCount = 0
            val request = postRequest("https://example.test/e", "hi".toByteArray())

            val response = interceptor.intercept(ScriptedChain(request, response(request, code)))

            assertEquals("code=$code response", code, response.code)
            assertEquals("code=$code should delete row", 0, queue.entries.size)
            assertEquals("code=$code should not schedule flush", 0, scheduleFlushCount)
        }
    }

    @Test
    fun rateLimit429KeepsRowAndSchedulesFlush() {
        val request = postRequest("https://example.test/e", "hi".toByteArray())

        val response = interceptor.intercept(ScriptedChain(request, response(request, 429)))

        assertEquals(429, response.code)
        assertEquals(1, queue.entries.size)
        assertEquals(1, scheduleFlushCount)
    }

    @Test
    fun serverError5xxKeepsRowAndSchedulesFlush() {
        val request = postRequest("https://example.test/token", "{}".toByteArray())

        val result = interceptor.intercept(ScriptedChain(request, response(request, 503)))

        assertEquals(503, result.code)
        assertEquals(1, queue.entries.size)
        assertEquals(1, scheduleFlushCount)
    }

    @Test
    fun ioExceptionKeepsRowAndSchedulesFlush() {
        val request = postRequest("https://example.test/e", "hi".toByteArray())

        assertThrows(IOException::class.java) {
            interceptor.intercept(ThrowingChain(request, IOException("network down")))
        }
        assertEquals(1, queue.entries.size)
        assertEquals(1, scheduleFlushCount)
    }

    @Test
    fun cancelsCallDoesNotMarkReady() {
        // Mirrors RetryInterceptor's doesNotRetryWhenCallIsCanceled. When the OkHttp Call is
        // canceled mid-flight (Call.cancel() surfaces as IOException), the buffer must not
        // mark the row ready or schedule a flush — otherwise FlushWorker replays a request the
        // user explicitly canceled. The preflighted row is dropped to keep the queue clean.
        val request = postRequest("https://example.test/e", "hi".toByteArray())
        val chain =
            object : TestChain() {
                override fun request(): Request = request

                override fun proceed(request: Request): Response {
                    testCall.cancel()
                    throw IOException("Canceled")
                }
            }

        assertThrows(IOException::class.java) { interceptor.intercept(chain) }
        assertEquals("Canceled call must drop the preflighted row", 0, queue.entries.size)
        assertEquals("Canceled call must not schedule a flush", 0, scheduleFlushCount)
    }

    @Test
    fun replayTagBypassesPreflight() {
        val request =
            postRequest("https://example.test/e", "{}".toByteArray())
                .newBuilder()
                .tag(ReplayMarker::class.java, ReplayMarker)
                .build()

        assertThrows(IOException::class.java) { interceptor.intercept(ThrowingChain(request, IOException("nope"))) }
        assertEquals(0, queue.entries.size)
        assertEquals(0, scheduleFlushCount)
    }

    @Test
    fun nonBufferableUrlBypassesPreflight() {
        val request = postRequest("https://example.test/creatives", "{}".toByteArray())

        assertThrows(IOException::class.java) { interceptor.intercept(ThrowingChain(request, IOException("nope"))) }
        assertEquals(0, queue.entries.size)
        assertEquals(0, scheduleFlushCount)
    }

    @Test
    fun bypassesGetRequests() {
        val request = Request.Builder().url("https://example.test/e").get().build()

        assertThrows(IOException::class.java) { interceptor.intercept(ThrowingChain(request, IOException("nope"))) }
        assertEquals(0, queue.entries.size)
        assertEquals(0, scheduleFlushCount)
    }

    @Test
    fun capEnforcedOnInsert() {
        runBlocking {
            queue.enqueue(BufferedRequestEntity(url = "u1", method = "POST", contentType = "", body = ByteArray(0), createdAtMs = 0), 3)
            queue.enqueue(BufferedRequestEntity(url = "u2", method = "POST", contentType = "", body = ByteArray(0), createdAtMs = 1), 3)
            queue.enqueue(BufferedRequestEntity(url = "u3", method = "POST", contentType = "", body = ByteArray(0), createdAtMs = 2), 3)
        }
        val request = postRequest("https://example.test/e", "fresh".toByteArray())

        assertThrows(IOException::class.java) { interceptor.intercept(ThrowingChain(request, IOException("down"))) }
        assertEquals(3, queue.entries.size)
        assertEquals(listOf("u2", "u3", "https://example.test/e"), queue.entries.map { it.url })
    }

    @Test
    fun oversizeBodySkipsPreflightButRequestProceeds() {
        val small =
            OfflineBufferInterceptor(
                queue = queue,
                context = context,
                config = BufferConfiguration(maxBodyBytes = 4),
                scheduleFlush = { scheduleFlushCount += 1 },
            )
        val request = postRequest("https://example.test/e", "12345".toByteArray())

        // The chain still runs (and here, throws). The interceptor should let the IOException
        // propagate without buffering — and without scheduling a flush, since there's no row.
        assertThrows(IOException::class.java) { small.intercept(ThrowingChain(request, IOException("down"))) }
        assertEquals(0, queue.entries.size)
        assertEquals(0, scheduleFlushCount)
    }

    @Test
    fun killBeforeProceedReturnsLeavesRowForFlush() {
        // Simulate a process kill mid-retry by short-circuiting before chain.proceed completes.
        // We can't actually kill the process in a unit test; instead, verify the row is present
        // *at* the moment proceed is called, since that's the state a kill would leave on disk.
        var rowCountAtProceed = -1
        val request = postRequest("https://example.test/e", "hi".toByteArray())
        val chain =
            object : TestChain() {
                override fun request(): Request = request

                override fun proceed(request: Request): Response {
                    rowCountAtProceed = queue.entries.size
                    throw IOException("simulating kill mid-retry")
                }
            }

        assertThrows(IOException::class.java) { interceptor.intercept(chain) }
        assertEquals(1, rowCountAtProceed)
        assertTrue("Row must persist for FlushWorker to drain after kill", queue.entries.size == 1)
    }

    @Test
    fun enqueueFailureDoesNotBlockRequest() {
        val throwingQueue =
            object : BufferedRequestQueue {
                override suspend fun enqueue(
                    entity: BufferedRequestEntity,
                    maxEntries: Int
                ): Long {
                    throw IllegalStateException("DB down")
                }

                override suspend fun peekOldestReady(limit: Int): List<BufferedRequestEntity> = emptyList()

                override suspend fun deleteByIds(ids: List<Long>) = Unit

                override suspend fun markReady(id: Long) = Unit

                override suspend fun markAllReadyOlderThan(cutoffMs: Long): Int = 0

                override suspend fun countReady(): Int = 0
            }
        val tolerant =
            OfflineBufferInterceptor(
                queue = throwingQueue,
                context = context,
                config = BufferConfiguration(),
                scheduleFlush = { scheduleFlushCount += 1 },
            )
        val request = postRequest("https://example.test/e", "hi".toByteArray())

        val response = tolerant.intercept(ScriptedChain(request, response(request, 204)))

        assertEquals(204, response.code)
        assertEquals(0, scheduleFlushCount)
    }

    private fun postRequest(
        url: String,
        body: ByteArray
    ): Request =
        Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

    private fun response(
        request: Request,
        code: Int
    ): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("test")
            .body("".toResponseBody("text/plain".toMediaType()))
            .build()

    private class ScriptedChain(
        private val request: Request,
        private val response: Response,
    ) : TestChain() {
        override fun request(): Request = request

        override fun proceed(request: Request): Response = response
    }

    private class ThrowingChain(
        private val request: Request,
        private val error: Throwable,
    ) : TestChain() {
        override fun request(): Request = request

        override fun proceed(request: Request): Response = throw error
    }
}
