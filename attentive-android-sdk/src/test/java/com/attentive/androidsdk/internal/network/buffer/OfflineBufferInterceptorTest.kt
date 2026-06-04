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
    fun enqueuesOnIOExceptionForBufferableEndpoint() {
        val request = postRequest("https://example.test/e", "hello".toByteArray())

        assertThrows(IOException::class.java) { interceptor.intercept(ThrowingChain(request, IOException("network down"))) }
        assertEquals(1, queue.entries.size)
        assertEquals("https://example.test/e", queue.entries[0].url)
        assertEquals(1, scheduleFlushCount)
    }

    @Test
    fun enqueuesOn5xxForBufferableEndpoint() {
        val request = postRequest("https://example.test/token", "{}".toByteArray())

        val result = interceptor.intercept(ScriptedChain(request, response(request, 503)))

        assertEquals(503, result.code)
        assertEquals(1, queue.entries.size)
        assertEquals(1, scheduleFlushCount)
    }

    @Test
    fun doesNotEnqueueOnNonRetryableResponses() {
        for (code in listOf(200, 400, 401, 429)) {
            queue.entries.clear()
            scheduleFlushCount = 0
            val request = postRequest("https://example.test/e", "{}".toByteArray())

            interceptor.intercept(ScriptedChain(request, response(request, code)))

            assertEquals("code=$code should not enqueue", 0, queue.entries.size)
            assertEquals("code=$code should not schedule flush", 0, scheduleFlushCount)
        }
    }

    @Test
    fun bypassesNonBufferableEndpoint() {
        val request = postRequest("https://example.test/creatives", "{}".toByteArray())

        assertThrows(IOException::class.java) { interceptor.intercept(ThrowingChain(request, IOException("nope"))) }
        assertEquals(0, queue.entries.size)
    }

    @Test
    fun bypassesGetRequests() {
        val request = Request.Builder().url("https://example.test/e").get().build()

        assertThrows(IOException::class.java) { interceptor.intercept(ThrowingChain(request, IOException("nope"))) }
        assertEquals(0, queue.entries.size)
    }

    @Test
    fun replayTagShortCircuits() {
        val request =
            postRequest("https://example.test/e", "{}".toByteArray())
                .newBuilder()
                .tag(ReplayMarker::class.java, ReplayMarker)
                .build()

        assertThrows(IOException::class.java) { interceptor.intercept(ThrowingChain(request, IOException("nope"))) }
        assertEquals(0, queue.entries.size)
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
    fun oversizeBodySkipsEnqueue() {
        val small =
            OfflineBufferInterceptor(
                queue = queue,
                context = context,
                config = BufferConfiguration(maxBodyBytes = 4),
                scheduleFlush = { scheduleFlushCount += 1 },
            )
        val request = postRequest("https://example.test/e", "12345".toByteArray())

        assertThrows(IOException::class.java) { small.intercept(ThrowingChain(request, IOException("down"))) }
        assertEquals(0, queue.entries.size)
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
