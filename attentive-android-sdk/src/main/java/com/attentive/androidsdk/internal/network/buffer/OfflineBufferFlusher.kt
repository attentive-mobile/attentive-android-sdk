package com.attentive.androidsdk.internal.network.buffer

import androidx.annotation.RestrictTo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.IOException

@RestrictTo(RestrictTo.Scope.LIBRARY)
class OfflineBufferFlusher(
    private val queue: BufferedRequestQueue,
    private val okHttpClientProvider: () -> OkHttpClient,
    private val config: BufferConfiguration = BufferConfiguration(),
) {
    private val mutex = Mutex()

    /**
     * Drains the buffer in FIFO order. Returns true if the queue is fully drained,
     * false if a request failed (5xx / 429 / IOException) and rows remain — caller
     * should schedule another flush attempt later.
     */
    suspend fun flush(): Boolean {
        mutex.withLock {
            // The connection pool may hold stale routes from the network we just lost.
            // Evict them so replays open fresh connections and re-resolve DNS.
            runCatching { okHttpClientProvider().connectionPool.evictAll() }

            while (true) {
                val batch = queue.peekOldest(config.batchSize)
                if (batch.isEmpty()) return true

                for (entity in batch) {
                    when (replay(entity)) {
                        ReplayOutcome.DELETE -> queue.deleteById(entity.id)
                        ReplayOutcome.STOP -> {
                            queue.incrementAttempt(entity.id)
                            return false
                        }
                    }
                }
            }
            @Suppress("UNREACHABLE_CODE")
            return true
        }
    }

    private fun replay(entity: BufferedRequestEntity): ReplayOutcome {
        val mediaType = entity.contentType.takeIf { it.isNotEmpty() }?.toMediaTypeOrNull()
        val request =
            Request.Builder()
                .url(entity.url)
                .method(entity.method, entity.body.toRequestBody(mediaType))
                .header(BufferConfiguration.REPLAY_HEADER, BufferConfiguration.REPLAY_HEADER_VALUE)
                .build()

        return try {
            okHttpClientProvider().newCall(request).execute().use { response ->
                when {
                    response.isSuccessful -> ReplayOutcome.DELETE
                    response.code == 429 -> ReplayOutcome.STOP
                    response.code in 500..599 -> ReplayOutcome.STOP
                    response.code in 400..499 -> {
                        Timber.w(
                            "OfflineBuffer: dropping entry %d after %d on replay",
                            entity.id,
                            response.code,
                        )
                        ReplayOutcome.DELETE
                    }
                    else -> ReplayOutcome.DELETE
                }
            }
        } catch (e: IOException) {
            Timber.w(e, "OfflineBuffer: replay failed for entry %d, will retry later", entity.id)
            ReplayOutcome.STOP
        }
    }

    private enum class ReplayOutcome { DELETE, STOP }
}
