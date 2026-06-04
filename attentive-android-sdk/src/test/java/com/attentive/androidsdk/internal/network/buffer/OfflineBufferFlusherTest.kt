package com.attentive.androidsdk.internal.network.buffer

import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class OfflineBufferFlusherTest {
    @Before
    fun uprootTimber() {
        // Other tests in this module plant Trees that hit Log.w(String, String, Throwable),
        // which is missing from the unit-test android.jar stub. Strip them.
        Timber.uprootAll()
    }

    @Test
    fun flushesQueueInFifoOrder() =
        runTest {
            val queue = FakeBufferedRequestQueue()
            queue.enqueue(entity("https://example.test/e", "first"), 100)
            queue.enqueue(entity("https://example.test/e", "second"), 100)
            queue.enqueue(entity("https://example.test/e", "third"), 100)

            val seen = mutableListOf<String>()
            val client =
                clientReturning { req ->
                    seen.add(req.body!!.readBytes())
                    response(req, 200)
                }
            val flusher = OfflineBufferFlusher(queue, { client })

            flusher.flush()

            assertEquals(listOf("first", "second", "third"), seen)
            assertEquals(0, queue.entries.size)
        }

    @Test
    fun stopsOn5xxAndIncrementsAttempt() =
        runTest {
            val queue = FakeBufferedRequestQueue()
            queue.enqueue(entity("https://example.test/e", "a"), 100)
            queue.enqueue(entity("https://example.test/e", "b"), 100)
            queue.enqueue(entity("https://example.test/e", "c"), 100)

            val callCount = AtomicInteger()
            val client =
                clientReturning { req ->
                    val n = callCount.getAndIncrement()
                    if (n == 1) response(req, 503) else response(req, 200)
                }
            val flusher = OfflineBufferFlusher(queue, { client })

            flusher.flush()

            // First entry succeeded → deleted. Second hit 503 → stays. Third never attempted.
            assertEquals(2, queue.entries.size)
            assertEquals("b", String(queue.entries[0].body))
            assertEquals("c", String(queue.entries[1].body))
        }

    @Test
    fun dropsEntryOn4xx() =
        runTest {
            val queue = FakeBufferedRequestQueue()
            queue.enqueue(entity("https://example.test/e", "bad"), 100)

            val client = clientReturning { req -> response(req, 400) }
            val flusher = OfflineBufferFlusher(queue, { client })

            flusher.flush()

            assertEquals(0, queue.entries.size)
        }

    @Test
    fun stopsOnIoExceptionAndKeepsEntry() =
        runTest {
            val queue = FakeBufferedRequestQueue()
            queue.enqueue(entity("https://example.test/e", "x"), 100)

            val client = clientReturning { _ -> throw IOException("network down") }
            val flusher = OfflineBufferFlusher(queue, { client })

            flusher.flush()

            assertEquals(1, queue.entries.size)
        }

    @Test
    fun replayRequestsCarryReplayTag() =
        runTest {
            val queue = FakeBufferedRequestQueue()
            queue.enqueue(entity("https://example.test/e", "x"), 100)

            var taggedAsReplay = false
            val client =
                clientReturning { req ->
                    taggedAsReplay = req.tag(ReplayMarker::class.java) != null
                    response(req, 200)
                }
            val flusher = OfflineBufferFlusher(queue, { client })

            flusher.flush()

            assertTrue(taggedAsReplay)
        }

    private fun entity(
        url: String,
        body: String
    ): BufferedRequestEntity =
        BufferedRequestEntity(
            url = url,
            method = "POST",
            contentType = "application/json",
            body = body.toByteArray(),
            createdAtMs = 0,
        )

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

    private fun okhttp3.RequestBody.readBytes(): String {
        val buffer = okio.Buffer()
        writeTo(buffer)
        return buffer.readByteString().utf8()
    }

    private fun clientReturning(handler: (Request) -> Response): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(
                Interceptor { chain ->
                    val req = chain.request()
                    handler(req)
                },
            )
            .build()
}
