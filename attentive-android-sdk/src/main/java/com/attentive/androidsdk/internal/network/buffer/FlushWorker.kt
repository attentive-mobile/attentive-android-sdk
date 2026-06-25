package com.attentive.androidsdk.internal.network.buffer

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.attentive.androidsdk.ClassFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

@RestrictTo(RestrictTo.Scope.LIBRARY)
class FlushWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = doFlush(ClassFactory.bufferQueue, ClassFactory.bufferOkHttpClient)

    companion object {
        const val UNIQUE_NAME = "com.attentive.androidsdk.flush"

        internal suspend fun doFlush(
            queue: BufferedRequestQueue?,
            client: okhttp3.OkHttpClient?,
        ): ListenableWorker.Result {
            if (queue == null || client == null) {
                // Cold-start: WM may have launched our process before AttentiveSdk.initialize
                // ran. Back off and retry — the host app's normal startup will populate these.
                Timber.i("OfflineBuffer: FlushWorker waiting for SDK initialization")
                return ListenableWorker.Result.retry()
            }
            val drained = OfflineBufferFlusher(queue, { client }).flush()
            return if (drained) ListenableWorker.Result.success() else ListenableWorker.Result.retry()
        }

        // Single supervisor scope for fire-and-forget recovery work; survives the call
        // site (typically Application.onCreate). Failures in one recovery attempt don't
        // cancel the scope. Tests don't observe this scope directly — they exercise the
        // queue methods.
        private val recoveryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /**
         * Called once on SDK init. Off-loads to [Dispatchers.IO] (no main-thread DB work).
         * Uses [BufferedRequestQueue.markAllReadyOlderThan] with the SDK-init wall-clock
         * time as cutoff so we only flip rows that predate this process — rows preflighted
         * by in-flight calls in this process stay `pending=true` and aren't replayed by
         * the worker until their original call gives up.
         */
        @JvmStatic
        fun recoverOrphansAndSchedule(context: Context) {
            val queue = ClassFactory.bufferQueue ?: return
            val cutoffMs = ClassFactory.bufferCutoffMs
            recoveryScope.launch {
                val ready =
                    try {
                        queue.markAllReadyOlderThan(cutoffMs)
                        queue.countReady()
                    } catch (t: Throwable) {
                        Timber.w(
                            "OfflineBuffer: failed to read queue count: %s",
                            t.message ?: t.javaClass.simpleName,
                        )
                        return@launch
                    }
                if (ready > 0) {
                    Timber.i("OfflineBuffer: %d row(s) found at launch — scheduling recovery flush", ready)
                    enqueue(context)
                }
            }
        }

        @JvmStatic
        fun enqueue(context: Context) {
            val request =
                OneTimeWorkRequestBuilder<FlushWorker>()
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                    )
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        WorkRequest.MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS,
                    )
                    .build()
            try {
                WorkManager.getInstance(context.applicationContext)
                    .enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.KEEP, request)
            } catch (t: Throwable) {
                // WorkManager not initialized (typical in unit tests without
                // WorkManagerTestInitHelper) or context not yet attached. Best-effort.
                Timber.w("OfflineBuffer: failed to enqueue FlushWorker: %s", t.message ?: t.javaClass.simpleName)
            }
        }
    }
}
