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
import timber.log.Timber
import java.util.concurrent.TimeUnit

@RestrictTo(RestrictTo.Scope.LIBRARY)
class FlushWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result =
        doFlush(ClassFactory.bufferQueue, ClassFactory.bufferOkHttpClient)

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
            val flusher = OfflineBufferFlusher(queue, { client })
            return try {
                if (flusher.flush()) ListenableWorker.Result.success() else ListenableWorker.Result.retry()
            } catch (t: Throwable) {
                Timber.w(t, "OfflineBuffer: FlushWorker threw")
                ListenableWorker.Result.retry()
            }
        }

        @JvmStatic
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<FlushWorker>()
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
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}
