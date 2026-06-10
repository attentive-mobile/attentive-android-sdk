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

/**
 * Persists bufferable POST bodies to Room **before** the request enters the retry/network stack,
 * and removes the row only on a terminal non-replayable outcome (2xx, 4xx non-429). If the
 * process dies during [com.attentive.androidsdk.internal.network.RetryInterceptor]'s exponential-
 * backoff sleep (up to 5 minutes), the row stays in place and [FlushWorker] replays it on next
 * launch.
 */
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
        if (request.tag(ReplayMarker::class.java) != null) {
            return chain.proceed(request)
        }

        val rowId =
            if (BufferableEndpoints.shouldBuffer(request)) preflightEnqueue(request) else null

        val response =
            try {
                chain.proceed(request)
            } catch (e: IOException) {
                if (rowId != null) {
                    markReadyForReplay(rowId)
                    scheduleFlush(context)
                }
                throw e
            }

        if (rowId != null) {
            when {
                response.code in 500..599 || response.code == 429 -> {
                    markReadyForReplay(rowId)
                    scheduleFlush(context)
                }
                else -> deleteRow(rowId)
            }
        }
        return response
    }

    private fun markReadyForReplay(id: Long) {
        try {
            runBlocking { queue.markReady(id) }
        } catch (t: Throwable) {
            Timber.w("OfflineBuffer: failed to mark row %d ready: %s", id, t.message ?: t.javaClass.simpleName)
        }
    }

    /** Captures body bytes and inserts a row. Returns the row id, or null if buffering was skipped. */
    private fun preflightEnqueue(request: Request): Long? {
        val body = request.body
        if (body == null) {
            Timber.w("OfflineBuffer: skipping enqueue, request has no body")
            return null
        }
        val bytes =
            try {
                val buffer = Buffer()
                body.writeTo(buffer)
                if (buffer.size > config.maxBodyBytes) {
                    Timber.w("OfflineBuffer: skipping enqueue, body too large (%d bytes)", buffer.size)
                    return null
                }
                buffer.readByteArray()
            } catch (t: Throwable) {
                Timber.w(t, "OfflineBuffer: failed to read request body, skipping enqueue")
                return null
            }

        val entity =
            BufferedRequestEntity(
                url = request.url.toString(),
                method = request.method,
                contentType = body.contentType()?.toString().orEmpty(),
                body = bytes,
                createdAtMs = System.currentTimeMillis(),
            )

        return try {
            val id = runBlocking { queue.enqueue(entity, config.maxEntries) }
            Timber.i("OfflineBuffer: enqueued %s %s", request.method, request.url.encodedPath)
            id
        } catch (t: Throwable) {
            Timber.w(t, "OfflineBuffer: failed to enqueue request")
            null
        }
    }

    private fun deleteRow(id: Long) {
        try {
            runBlocking { queue.deleteByIds(listOf(id)) }
        } catch (t: Throwable) {
            Timber.w(t, "OfflineBuffer: failed to delete buffered row %d", id)
        }
    }
}
