package com.attentive.androidsdk.internal.journeys

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.AttentiveSdk
import com.attentive.androidsdk.journeys.AbandonedCart
import com.attentive.androidsdk.journeys.AbandonmentCopyProvider
import com.attentive.androidsdk.journeys.NotificationCopy
import com.attentive.androidsdk.push.AttentivePush
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.concurrent.TimeUnit

internal interface AbandonmentScheduler {
    fun schedule(delayMillis: Long)

    fun cancel()
}

internal class WorkManagerAbandonmentScheduler(private val context: Context) : AbandonmentScheduler {
    override fun schedule(delayMillis: Long) {
        val request =
            OneTimeWorkRequestBuilder<CartAbandonmentWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build()
        WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
    }

    companion object {
        const val UNIQUE_NAME = "com.attentive.androidsdk.cartAbandonment"
    }
}

/** Runs the cart abandonment check scheduled by the most recent add-to-cart. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class CartAbandonmentWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val tracker = AttentiveSdk.cartAbandonmentTracker
        if (tracker == null) {
            Timber.i("Skipping cart abandonment check: cart abandonment is not enabled")
            return Result.success()
        }
        val isForeground =
            withContext(Dispatchers.Main) {
                ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            }
        val canNotify =
            AttentiveEventTracker.instance.isPushEnabled() &&
                AttentiveSdk.isPushPermissionGranted(applicationContext)

        when (val evaluation = tracker.evaluate(isForeground, canNotify)) {
            is AbandonmentEvaluation.Notify -> notify(evaluation, tracker.config.copyProvider)
            else -> Timber.d("Cart abandonment check: $evaluation")
        }
        return Result.success()
    }

    private suspend fun notify(evaluation: AbandonmentEvaluation.Notify, copyProvider: AbandonmentCopyProvider?) {
        val cart = evaluation.cart
        val copy = copyProvider?.let { customCopy(it, cart) } ?: defaultCopy(cart)
        val dataMap = evaluation.deeplink?.let { mapOf(AttentivePush.ATTENTIVE_DEEP_LINK_KEY to it) } ?: emptyMap()
        AttentivePush.getInstance().sendNotification(
            messageTitle = copy.title,
            messageBody = copy.body,
            dataMap = dataMap,
            imageUrl = cart.items.firstNotNullOfOrNull { it.productImage },
            context = applicationContext,
        )
    }

    private suspend fun customCopy(provider: AbandonmentCopyProvider, cart: AbandonedCart): NotificationCopy? =
        try {
            withTimeoutOrNull(COPY_TIMEOUT_MILLIS) { provider.createCopy(cart) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Cart abandonment copy provider failed; using default copy")
            null
        }

    companion object {
        private const val COPY_TIMEOUT_MILLIS = 15_000L

        internal fun defaultCopy(cart: AbandonedCart): NotificationCopy {
            val itemCount = cart.items.sumOf { it.quantity }
            val singleName = cart.items.singleOrNull()?.takeIf { it.quantity == 1 }?.name
            val body =
                when {
                    singleName != null -> "Your $singleName is still waiting. Complete your order before it's gone."
                    itemCount == 1 -> "Your item is still waiting. Complete your order before it's gone."
                    else -> "Your $itemCount items are still waiting. Complete your order before they're gone."
                }
            return NotificationCopy(title = "You left something in your cart", body = body)
        }
    }
}
