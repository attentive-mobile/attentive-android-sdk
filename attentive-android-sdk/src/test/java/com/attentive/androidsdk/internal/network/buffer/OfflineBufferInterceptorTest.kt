package com.attentive.androidsdk.internal.network.buffer

import android.content.Context
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Timeout
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
        interceptor = OfflineBufferInterceptor(
            queue = queue,
            context = context,
            config = BufferConfiguration(maxEntries = 3),
            scheduleFlush = { scheduleFlushCount += 1 },
        )
    }

    @Test
    fun enqueuesOnIOExceptionForBufferableEndpoint() {
        val request = postRequest("https://example.test/e", "hello".toByteArray())
        val chain = ThrowingChain(request, IOException("network down"))

        assertThrows(IOException::class.java) { interceptor.intercept(chain) }
        assertEquals(1, queue.entries.size)
        assertEquals("https://example.test/e", queue.entries[0].url)
    }

    @Test
    fun enqueuesOn5xxForBufferableEndpoint() {
        val request = postRequest("https://example.test/token", "{}".toByteArray())
        val chain = ScriptedChain(request, response(request, 503))

        val result = interceptor.intercept(chain)

        assertEquals(503, result.code)
        assertEquals(1, queue.entries.size)
    }

    @Test
    fun doesNotEnqueueOn4xx() {
        val request = postRequest("https://example.test/e", "{}".toByteArray())
        val chain = ScriptedChain(request, response(request, 400))

        interceptor.intercept(chain)

        assertEquals(0, queue.entries.size)
    }

    @Test
    fun doesNotEnqueueOn429() {
        val request = postRequest("https://example.test/e", "{}".toByteArray())
        val chain = ScriptedChain(request, response(request, 429))

        interceptor.intercept(chain)

        assertEquals(0, queue.entries.size)
    }

    @Test
    fun doesNotEnqueueOn2xx() {
        val request = postRequest("https://example.test/e", "{}".toByteArray())
        val chain = ScriptedChain(request, response(request, 200))

        interceptor.intercept(chain)

        assertEquals(0, queue.entries.size)
    }

    @Test
    fun bypassesNonBufferableEndpoint() {
        val request = postRequest("https://example.test/creatives", "{}".toByteArray())
        val chain = ThrowingChain(request, IOException("nope"))

        assertThrows(IOException::class.java) { interceptor.intercept(chain) }
        assertEquals(0, queue.entries.size)
    }

    @Test
    fun bypassesGetRequests() {
        val request = Request.Builder().url("https://example.test/e").get().build()
        val chain = ThrowingChain(request, IOException("nope"))

        assertThrows(IOException::class.java) { interceptor.intercept(chain) }
        assertEquals(0, queue.entries.size)
    }

    @Test
    fun replayTagShortCircuits() {
        val request =
            postRequest("https://example.test/e", "{}".toByteArray())
                .newBuilder()
                .header(BufferConfiguration.REPLAY_HEADER, BufferConfiguration.REPLAY_HEADER_VALUE)
                .build()
        val chain = ThrowingChain(request, IOException("nope"))

        assertThrows(IOException::class.java) { interceptor.intercept(chain) }
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
        val chain = ThrowingChain(request, IOException("down"))

        assertThrows(IOException::class.java) { interceptor.intercept(chain) }
        assertEquals(3, queue.entries.size)
        assertEquals(listOf("u2", "u3", "https://example.test/e"), queue.entries.map { it.url })
    }

    @Test
    fun oversizeBodySkipsEnqueue() {
        val small = OfflineBufferInterceptor(
            queue = queue,
            context = context,
            config = BufferConfiguration(maxBodyBytes = 4),
            scheduleFlush = { scheduleFlushCount += 1 },
        )
        val request = postRequest("https://example.test/e", "12345".toByteArray())
        val chain = ThrowingChain(request, IOException("down"))

        assertThrows(IOException::class.java) { small.intercept(chain) }
        assertEquals(0, queue.entries.size)
        assertEquals(0, scheduleFlushCount)
    }

    @Test
    fun schedulesFlushOnEnqueue() {
        val request = postRequest("https://example.test/e", "hi".toByteArray())
        val chain = ThrowingChain(request, IOException("down"))

        assertThrows(IOException::class.java) { interceptor.intercept(chain) }
        assertEquals(1, queue.entries.size)
        assertEquals(1, scheduleFlushCount)
    }

    @Test
    fun doesNotScheduleFlushWhenNotEnqueued() {
        val request = postRequest("https://example.test/e", "{}".toByteArray())
        val chain = ScriptedChain(request, response(request, 400))

        interceptor.intercept(chain)

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
    ) : Interceptor.Chain {
        override fun request(): Request = request

        override fun proceed(request: Request): Response = response

        override fun connection() = null

        override fun call(): Call = StubCall(request)

        override fun connectTimeoutMillis() = 0

        override fun withConnectTimeout(
            timeout: Int,
            unit: java.util.concurrent.TimeUnit
        ) = throw UnsupportedOperationException()

        override fun readTimeoutMillis() = 0

        override fun withReadTimeout(
            timeout: Int,
            unit: java.util.concurrent.TimeUnit
        ) = throw UnsupportedOperationException()

        override fun writeTimeoutMillis() = 0

        override fun withWriteTimeout(
            timeout: Int,
            unit: java.util.concurrent.TimeUnit
        ) = throw UnsupportedOperationException()
    }

    private class ThrowingChain(
        private val request: Request,
        private val error: Throwable,
    ) : Interceptor.Chain {
        override fun request(): Request = request

        override fun proceed(request: Request): Response = throw error

        override fun connection() = null

        override fun call(): Call = StubCall(request)

        override fun connectTimeoutMillis() = 0

        override fun withConnectTimeout(
            timeout: Int,
            unit: java.util.concurrent.TimeUnit
        ) = throw UnsupportedOperationException()

        override fun readTimeoutMillis() = 0

        override fun withReadTimeout(
            timeout: Int,
            unit: java.util.concurrent.TimeUnit
        ) = throw UnsupportedOperationException()

        override fun writeTimeoutMillis() = 0

        override fun withWriteTimeout(
            timeout: Int,
            unit: java.util.concurrent.TimeUnit
        ) = throw UnsupportedOperationException()
    }

    private class StubCall(private val req: Request) : Call {
        override fun cancel() = Unit

        override fun isCanceled(): Boolean = false

        override fun clone(): Call = this

        override fun execute(): Response = throw UnsupportedOperationException()

        override fun enqueue(responseCallback: Callback) = throw UnsupportedOperationException()

        override fun isExecuted(): Boolean = false

        override fun request(): Request = req

        override fun timeout(): Timeout = Timeout.NONE
    }
}
