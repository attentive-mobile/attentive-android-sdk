package com.attentive.androidsdk.internal.network.buffer

import androidx.work.ListenableWorker
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import timber.log.Timber

class FlushWorkerTest {

    @Before
    fun uprootTimber() {
        // Other tests in this module plant Trees that hit Log.w(String, String, Throwable),
        // which is missing from the unit-test android.jar stub.
        Timber.uprootAll()
    }

    @Test
    fun returnsRetryWhenStaticsNull() = runTest {
        val result = FlushWorker.doFlush(queue = null, client = null)
        assertTrue(result is ListenableWorker.Result.Retry)
    }

    @Test
    fun returnsSuccessWhenQueueDrains() = runTest {
        val queue = FakeBufferedRequestQueue()
        queue.enqueue(entity("https://example.test/e", "x"), 100)
        val client = clientReturning { req -> response(req, 200) }

        val result = FlushWorker.doFlush(queue, client)

        assertTrue(result is ListenableWorker.Result.Success)
    }

    @Test
    fun returnsRetryWhenFlushStops() = runTest {
        val queue = FakeBufferedRequestQueue()
        queue.enqueue(entity("https://example.test/e", "x"), 100)
        val client = clientReturning { req -> response(req, 503) }

        val result = FlushWorker.doFlush(queue, client)

        assertTrue(result is ListenableWorker.Result.Retry)
    }

    private fun entity(url: String, body: String): BufferedRequestEntity =
        BufferedRequestEntity(
            url = url,
            method = "POST",
            contentType = "application/json",
            body = body.toByteArray(),
            createdAtMs = 0,
        )

    private fun response(request: Request, code: Int): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("test")
            .body("".toResponseBody("text/plain".toMediaType()))
            .build()

    private fun clientReturning(handler: (Request) -> Response): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain -> handler(chain.request()) })
            .build()
}
