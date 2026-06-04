package com.attentive.androidsdk.internal.network.buffer

import androidx.annotation.RestrictTo
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
    /**
     * Drains the buffer in FIFO order. Returns true if the queue is fully drained,
     * false if a request failed (5xx / 429 / IOException) and rows remain — caller
     * should schedule another flush attempt later.
     */
    suspend fun flush(): Boolean {
        while (true) {
            val batch = queue.peekOldest(config.batchSize)
            if (batch.isEmpty()) return true

            val drained = mutableListOf<Long>()
            var stopped = false
            for (entity in batch) {
                if (replay(entity)) {
                    drained.add(entity.id)
                } else {
                    stopped = true
                    break
                }
            }
            if (drained.isNotEmpty()) queue.deleteByIds(drained)
            if (stopped) return false
        }
    }

    /** Returns true if the entry should be deleted, false if the flush should stop. */
    private fun replay(entity: BufferedRequestEntity): Boolean {
        val mediaType = entity.contentType.takeIf { it.isNotEmpty() }?.toMediaTypeOrNull()
        val request =
            Request.Builder()
                .url(entity.url)
                .method(entity.method, entity.body.toRequestBody(mediaType))
                .tag(ReplayMarker::class.java, ReplayMarker)
                .build()

        return try {
            okHttpClientProvider().newCall(request).execute().use { response ->
                when {
                    response.isSuccessful -> true
                    response.code == 429 -> false
                    response.code in 500..599 -> false
                    response.code in 400..499 -> {
                        Timber.w(
                            "OfflineBuffer: dropping entry %d after %d on replay",
                            entity.id,
                            response.code,
                        )
                        true
                    }
                    else -> true
                }
            }
        } catch (e: IOException) {
            Timber.w(e, "OfflineBuffer: replay failed for entry %d, will retry later", entity.id)
            false
        }
    }
}
