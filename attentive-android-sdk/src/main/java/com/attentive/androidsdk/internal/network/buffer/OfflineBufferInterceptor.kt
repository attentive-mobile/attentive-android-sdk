package com.attentive.androidsdk.internal.network.buffer

import android.content.Context
import androidx.annotation.RestrictTo
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import timber.log.Timber
import java.io.IOException

@RestrictTo(RestrictTo.Scope.LIBRARY)
class OfflineBufferInterceptor(
    private val queue: BufferedRequestQueue,
    private val context: Context,
    private val config: BufferConfiguration = BufferConfiguration(),
    private val scheduleFlush: (Context) -> Unit = { FlushWorker.enqueue(it) },
) : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val isReplay = request.header(BufferConfiguration.REPLAY_HEADER) != null

        if (isReplay) {
            return chain.proceed(request)
        }

        val outcome = runCatching { chain.proceed(request) }
        val response = outcome.getOrNull()
        val error = outcome.exceptionOrNull()

        if (!BufferableEndpoints.shouldBuffer(request)) {
            if (error != null) throw error
            return response!!
        }

        when {
            error is IOException -> {
                tryEnqueue(request)
                throw error
            }
            error != null -> throw error
            response != null && response.code in 500..599 -> {
                tryEnqueue(request)
                return response
            }
            else -> return response!!
        }
    }

    private fun tryEnqueue(request: Request) {
        val body = request.body
        if (body == null) {
            Timber.w("OfflineBuffer: skipping enqueue, request has no body")
            return
        }
        val bytes =
            try {
                val buffer = Buffer()
                body.writeTo(buffer)
                if (buffer.size > config.maxBodyBytes) {
                    Timber.w("OfflineBuffer: skipping enqueue, body too large (%d bytes)", buffer.size)
                    return
                }
                buffer.readByteArray()
            } catch (t: Throwable) {
                Timber.w(t, "OfflineBuffer: failed to read request body, skipping enqueue")
                return
            }

        val contentType = body.contentType()?.toString().orEmpty()
        val entity =
            BufferedRequestEntity(
                url = request.url.toString(),
                method = request.method,
                contentType = contentType,
                body = bytes,
                createdAtMs = System.currentTimeMillis(),
            )

        try {
            runBlocking {
                queue.enqueue(entity, config.maxEntries)
            }
            Timber.i("OfflineBuffer: enqueued %s %s", request.method, request.url.encodedPath)
            scheduleFlush(context)
        } catch (t: Throwable) {
            Timber.w(t, "OfflineBuffer: failed to enqueue request")
        }
    }
}
