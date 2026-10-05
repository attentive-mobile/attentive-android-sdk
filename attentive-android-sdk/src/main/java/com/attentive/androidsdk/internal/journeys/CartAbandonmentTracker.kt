package com.attentive.androidsdk.internal.journeys

import com.attentive.androidsdk.events.AddToCartEvent
import com.attentive.androidsdk.events.Event
import com.attentive.androidsdk.events.Item
import com.attentive.androidsdk.events.ProductViewEvent
import com.attentive.androidsdk.events.PurchaseEvent
import com.attentive.androidsdk.journeys.AbandonedCart
import com.attentive.androidsdk.journeys.CartAbandonmentConfig
import com.attentive.androidsdk.journeys.CartAbandonmentState
import com.attentive.androidsdk.journeys.CartAbandonmentState.Status
import com.attentive.androidsdk.journeys.NotificationCopy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.concurrent.TimeUnit

internal sealed interface AbandonmentEvaluation {
    data object NoCart : AbandonmentEvaluation

    data object AlreadyEvaluated : AbandonmentEvaluation

    data object Deferred : AbandonmentEvaluation

    data class BelowThreshold(val score: Double) : AbandonmentEvaluation

    data class PushUnavailable(val score: Double) : AbandonmentEvaluation

    /** @property copy Copy prepared while the app was in the foreground, if any. */
    data class Notify(val cart: AbandonedCart, val deeplink: String?, val copy: NotificationCopy?) : AbandonmentEvaluation
}

/**
 * Tracks the user's cart from recorded events and [syncCart], and decides, when the abandonment
 * check runs, whether to notify. State lives in [CartSnapshotStore] so a check scheduled before
 * process death still sees the cart.
 *
 * Copy from [CartAbandonmentConfig.copyProvider] is prepared while the app is in the foreground,
 * after the cart changes, because on-device models such as Gemini Nano refuse background use.
 */
internal class CartAbandonmentTracker(
    val config: CartAbandonmentConfig,
    private val store: CartSnapshotStore,
    private val scheduler: AbandonmentScheduler,
    private val isAppInForeground: () -> Boolean,
    private val _state: MutableStateFlow<CartAbandonmentState> = MutableStateFlow(CartAbandonmentState()),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    val state: StateFlow<CartAbandonmentState> = _state.asStateFlow()

    /** Set once the host reports cart contents, after which add-to-cart events no longer change items. */
    @Volatile
    private var cartSyncedByHost = false

    private var copyJob: Job? = null

    init {
        _state.value = stateFor(store.snapshot)
    }

    @Synchronized
    fun onEvent(event: Event) {
        when (event) {
            is AddToCartEvent -> onAddToCart(event)
            is ProductViewEvent -> onProductView(event)
            is PurchaseEvent -> onPurchase()
            else -> Unit
        }
    }

    /** Replaces the tracked items with the cart's current contents. An empty cart cancels the check. */
    @Synchronized
    fun syncCart(items: List<Item>) {
        cartSyncedByHost = true
        val existing = store.snapshot
        if (items.isEmpty()) {
            if (existing == null) return
            scheduler.cancel()
            store.snapshot = null
            _state.value = CartAbandonmentState(Status.EMPTY)
            return
        }
        val now = clock()
        update(
            existing?.copy(items = mergeByVariant(items))
                ?: CartSnapshot(mergeByVariant(items), lastAddedAtMillis = now, lastActivityAtMillis = now, addToCartCount = 0),
        )
        prepareCopy()
    }

    @Synchronized
    fun onAppForegrounded() {
        val snapshot = store.snapshot ?: return
        if (snapshot.outcome == CartSnapshot.Outcome.PENDING && snapshot.checkAtMillis != null) {
            update(snapshot.copy(appOpensSinceAdd = snapshot.appOpensSinceAdd + 1, lastActivityAtMillis = clock()))
        }
        prepareCopy()
    }

    @Synchronized
    fun onUserCleared() {
        copyJob?.cancel()
        scheduler.cancel()
        store.clear()
        _state.value = CartAbandonmentState(Status.EMPTY)
    }

    @Synchronized
    fun stop() {
        copyJob?.cancel()
        scheduler.cancel()
        _state.value = CartAbandonmentState(Status.DISABLED)
    }

    /**
     * Decides what to do when the abandonment check runs. Records the outcome so the cart is
     * evaluated once per add-to-cart.
     */
    @Synchronized
    fun evaluate(canNotify: Boolean): AbandonmentEvaluation {
        val snapshot = store.snapshot ?: return AbandonmentEvaluation.NoCart
        if (snapshot.outcome != CartSnapshot.Outcome.PENDING || snapshot.checkAtMillis == null) {
            return AbandonmentEvaluation.AlreadyEvaluated
        }
        if (isAppInForeground()) {
            Timber.d("Cart abandonment check deferred: app is in the foreground")
            update(snapshot.copy(checkAtMillis = clock() + config.delayMillis))
            scheduler.schedule(config.delayMillis)
            return AbandonmentEvaluation.Deferred
        }

        val score = score(snapshot)
        Timber.i("Cart abandoned with purchase intent score %.2f (threshold %.2f)", score, config.scoreThreshold)
        return when {
            score < config.scoreThreshold -> {
                update(snapshot.copy(outcome = CartSnapshot.Outcome.BELOW_THRESHOLD))
                AbandonmentEvaluation.BelowThreshold(score)
            }
            !canNotify -> {
                update(snapshot.copy(outcome = CartSnapshot.Outcome.PUSH_UNAVAILABLE))
                AbandonmentEvaluation.PushUnavailable(score)
            }
            else -> {
                update(snapshot.copy(outcome = CartSnapshot.Outcome.NOTIFIED))
                AbandonmentEvaluation.Notify(
                    AbandonedCart(snapshot.items, score, snapshot.lastAddedAtMillis),
                    snapshot.deeplink ?: config.cartDeeplink,
                    snapshot.currentCopy?.let { NotificationCopy(it.title, it.body) },
                )
            }
        }
    }

    private fun onAddToCart(event: AddToCartEvent) {
        val now = clock()
        val existing = store.snapshot
        // A new add re-arms the check for a cart that was already evaluated; its items stay in the cart.
        val pending = existing?.takeIf { it.outcome == CartSnapshot.Outcome.PENDING }
        val items = if (cartSyncedByHost && existing != null) existing.items else mergeByVariant(existing?.items.orEmpty() + event.items)
        update(
            CartSnapshot(
                items = items,
                deeplink = event.deeplink ?: existing?.deeplink,
                lastAddedAtMillis = now,
                lastActivityAtMillis = now,
                checkAtMillis = now + config.delayMillis,
                addToCartCount = (pending?.addToCartCount ?: 0) + 1,
                appOpensSinceAdd = pending?.appOpensSinceAdd ?: 0,
                copy = existing?.copy,
            ),
        )
        scheduler.schedule(config.delayMillis)
        prepareCopy()
    }

    private fun onProductView(event: ProductViewEvent) {
        val views = store.productViews.toMutableMap()
        event.items.forEach { item ->
            // Re-inserting moves the product to the end so the cap drops the least recent.
            val count = (views.remove(item.productId) ?: 0) + 1
            views[item.productId] = count
        }
        store.productViews = views
        store.snapshot?.takeIf { it.outcome == CartSnapshot.Outcome.PENDING }?.let {
            update(it.copy(lastActivityAtMillis = clock()))
        }
    }

    private fun onPurchase() {
        copyJob?.cancel()
        scheduler.cancel()
        store.snapshot = null
        store.hasPurchased = true
        _state.value = CartAbandonmentState(Status.PURCHASED)
    }

    /** Asks the copy provider for copy matching the current cart, once the cart stops changing. */
    private fun prepareCopy() {
        val provider = config.copyProvider ?: return
        val snapshot = store.snapshot ?: return
        if (snapshot.currentCopy != null || !isAppInForeground()) return
        copyJob?.cancel()
        copyJob =
            scope.launch {
                delay(COPY_DEBOUNCE_MILLIS)
                val cartKey = snapshot.cartKey
                val copy =
                    try {
                        withTimeoutOrNull(COPY_TIMEOUT_MILLIS) {
                            provider.createCopy(AbandonedCart(snapshot.items, score(snapshot), snapshot.lastAddedAtMillis))
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.w(e, "Cart abandonment copy provider failed")
                        null
                    } ?: return@launch
                synchronized(this@CartAbandonmentTracker) {
                    val current = store.snapshot?.takeIf { it.cartKey == cartKey } ?: return@launch
                    store.snapshot = current.copy(copy = CartSnapshot.CachedCopy(cartKey, copy.title, copy.body))
                    Timber.d("Prepared cart abandonment copy: ${copy.title} / ${copy.body}")
                }
            }
    }

    private fun update(snapshot: CartSnapshot) {
        store.snapshot = snapshot
        _state.value = stateFor(snapshot)
    }

    private fun stateFor(snapshot: CartSnapshot?): CartAbandonmentState {
        if (snapshot == null) return CartAbandonmentState(Status.EMPTY)
        val status =
            when (snapshot.outcome) {
                CartSnapshot.Outcome.PENDING -> if (snapshot.checkAtMillis == null) Status.IDLE else Status.WAITING
                CartSnapshot.Outcome.NOTIFIED -> Status.NOTIFIED
                CartSnapshot.Outcome.BELOW_THRESHOLD -> Status.BELOW_THRESHOLD
                CartSnapshot.Outcome.PUSH_UNAVAILABLE -> Status.PUSH_UNAVAILABLE
            }
        return CartAbandonmentState(
            status = status,
            itemCount = snapshot.itemCount,
            intentScore = score(snapshot),
            checkAtMillis = snapshot.checkAtMillis.takeIf { status == Status.WAITING },
        )
    }

    private fun score(snapshot: CartSnapshot): Double {
        val views = store.productViews
        val cartProductIds = snapshot.items.map { it.productId }.toSet()
        return PurchaseIntentScorer.score(
            IntentSignals(
                addToCartCount = snapshot.addToCartCount,
                cartValue = snapshot.items.sumOf { it.price.price.toDouble() * it.quantity },
                productViewsOfCartItems = cartProductIds.sumOf { views[it] ?: 0 },
                appOpensSinceAdd = snapshot.appOpensSinceAdd,
                hoursSinceLastActivity = (clock() - snapshot.lastActivityAtMillis).toDouble() / TimeUnit.HOURS.toMillis(1),
                hasPurchasedBefore = store.hasPurchased,
            ),
        )
    }

    companion object {
        internal const val COPY_DEBOUNCE_MILLIS = 1_500L
        internal const val COPY_TIMEOUT_MILLIS = 15_000L
    }
}
