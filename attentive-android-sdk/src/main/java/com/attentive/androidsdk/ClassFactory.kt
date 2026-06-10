package com.attentive.androidsdk

import android.content.Context
import com.attentive.androidsdk.internal.network.AttentiveHttpLogger
import com.attentive.androidsdk.internal.network.DatadogTracePriorityInterceptor
import com.attentive.androidsdk.internal.network.RetryInterceptor
import com.attentive.androidsdk.internal.network.UserAgentInterceptor
import com.attentive.androidsdk.internal.network.buffer.BufferDatabase
import com.attentive.androidsdk.internal.network.buffer.BufferedRequestQueue
import com.attentive.androidsdk.internal.network.buffer.OfflineBufferInterceptor
import com.attentive.androidsdk.internal.network.buffer.RoomBufferedRequestQueue
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

object ClassFactory {
    /**
     * The OkHttpClient used for offline buffer replays. Set when [buildOkHttpClient] runs
     * with a non-null context (host-app SDK init). [com.attentive.androidsdk.internal.network.buffer.FlushWorker]
     * reads this; if null on doWork start, the worker returns Result.retry() so WorkManager
     * waits for SDK init before draining the queue.
     */
    @Volatile
    @JvmStatic
    internal var bufferOkHttpClient: OkHttpClient? = null

    @Volatile
    @JvmStatic
    internal var bufferQueue: BufferedRequestQueue? = null

    /**
     * Wall-clock time captured the moment the buffer queue is wired (before any request
     * can be preflighted by this process). Used by
     * [com.attentive.androidsdk.internal.network.buffer.FlushWorker.recoverOrphansAndSchedule]
     * as the cutoff for orphan detection: any pending row with `createdAtMs < cutoff` is
     * by definition from a prior process. Rows preflighted by this process always have
     * `createdAtMs >= cutoff`, so they're never flipped by recovery.
     */
    @Volatile
    @JvmStatic
    internal var bufferCutoffMs: Long = 0L

    @JvmStatic
    fun buildPersistentStorage(context: Context): PersistentStorage {
        return PersistentStorage(context)
    }

    @JvmStatic
    fun buildVisitorService(persistentStorage: PersistentStorage): VisitorService {
        return VisitorService(persistentStorage)
    }

    @JvmStatic
    fun buildOkHttpClient(
        logLevel: AttentiveLogLevel?,
        interceptor: Interceptor,
        context: Context? = null,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor(AttentiveHttpLogger())
        if (logLevel == AttentiveLogLevel.VERBOSE) {
            logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        } else if (logLevel == AttentiveLogLevel.STANDARD) {
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC)
        }
        val builder =
            OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .addInterceptor(DatadogTracePriorityInterceptor())
        if (context != null) {
            // OfflineBuffer must wrap Retry so each retry attempt does NOT re-enter the buffer.
            // Only the final retry-exhausted IOException reaches OfflineBufferInterceptor.
            // Capture the cutoff BEFORE installing the interceptor so any row this process
            // ever preflights has createdAtMs >= bufferCutoffMs and is excluded from
            // orphan recovery.
            bufferCutoffMs = System.currentTimeMillis()
            val queue = RoomBufferedRequestQueue(BufferDatabase.getInstance(context))
            builder.addInterceptor(OfflineBufferInterceptor(queue, context))
            bufferQueue = queue
        }
        val client = builder
            .addInterceptor(RetryInterceptor())
            .addInterceptor(logging)
            .build()
        if (context != null) {
            bufferOkHttpClient = client
        }
        return client
    }

    fun buildUserAgentInterceptor(context: Context?): Interceptor {
        return UserAgentInterceptor(context)
    }

    @JvmStatic
    internal fun buildAttentiveApi(
        okHttpClient: OkHttpClient,
        domain: String,
    ): AttentiveApi {
        return AttentiveApi(okHttpClient, domain)
    }

    fun buildSettingsService(persistentStorage: PersistentStorage): SettingsService {
        return SettingsService(persistentStorage)
    }
}
