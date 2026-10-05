package com.attentive.androidsdk.internal.journeys

import com.attentive.androidsdk.events.AddToCartEvent
import com.attentive.androidsdk.events.Event
import com.attentive.androidsdk.events.ProductViewEvent
import com.attentive.androidsdk.events.PurchaseEvent
import com.attentive.androidsdk.journeys.AbandonedCart
import com.attentive.androidsdk.journeys.CartAbandonmentConfig
import com.attentive.androidsdk.journeys.CartAbandonmentState
import com.attentive.androidsdk.journeys.CartAbandonmentState.Status
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.TimeUnit

internal sealed interface AbandonmentEvaluation {
    data object NoCart : AbandonmentEvaluation

    data object AlreadyEvaluated : AbandonmentEvaluation

    data object Deferred : AbandonmentEvaluation

    data class BelowThreshold(val score: Double) : AbandonmentEvaluation

    data class PushUnavailable(val score: Double) : AbandonmentEvaluation

    data class Notify(val cart: AbandonedCart, val deeplink: String?) : AbandonmentEvaluation
}

/**
 * Tracks the user's cart from recorded events and decides, when the abandonment check runs,
 * whether to notify. State lives in [CartSnapshotStore] so a check scheduled before process
 * death still sees the cart.
 */
internal class CartAbandonmentTracker(
    val config: CartAbandonmentConfig,
    private val store: CartSnapshotStore,
    private val scheduler: AbandonmentScheduler,
    private val _state: MutableStateFlow<CartAbandonmentState> = MutableStateFlow(CartAbandonmentState()),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    val state: StateFlow<CartAbandonmentState> = _state.asStateFlow()

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

    @Synchronized
    fun onAppForegrounded() {
        val snapshot = store.snapshot ?: return
        if (snapshot.outcome != CartSnapshot.Outcome.PENDING) return
        update(snapshot.copy(appOpensSinceAdd = snapshot.appOpensSinceAdd + 1, lastActivityAtMillis = clock()))
    }

    @Synchronized
    fun onUserCleared() {
        scheduler.cancel()
        store.clear()
        _state.value = CartAbandonmentState(Status.EMPTY)
    }

    @Synchronized
    fun stop() {
        scheduler.cancel()
        _state.value = CartAbandonmentState(Status.DISABLED)
    }

    /**
     * Decides what to do when the abandonment check runs. Records the outcome so the cart is
     * evaluated once per add-to-cart.
     */
    @Synchronized
    fun evaluate(isAppInForeground: Boolean, canNotify: Boolean): AbandonmentEvaluation {
        val snapshot = store.snapshot ?: return AbandonmentEvaluation.NoCart
        if (snapshot.outcome != CartSnapshot.Outcome.PENDING) return AbandonmentEvaluation.AlreadyEvaluated
        if (isAppInForeground) {
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
                )
            }
        }
    }

    private fun onAddToCart(event: AddToCartEvent) {
        val now = clock()
        val existing = store.snapshot?.takeIf { it.outcome == CartSnapshot.Outcome.PENDING }
        val snapshot =
            CartSnapshot(
                items = mergeByVariant(existing?.items.orEmpty() + event.items),
                deeplink = event.deeplink ?: existing?.deeplink,
                lastAddedAtMillis = now,
                lastActivityAtMillis = now,
                checkAtMillis = now + config.delayMillis,
                addToCartCount = (existing?.addToCartCount ?: 0) + 1,
                appOpensSinceAdd = existing?.appOpensSinceAdd ?: 0,
            )
        update(snapshot)
        scheduler.schedule(config.delayMillis)
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
        scheduler.cancel()
        store.snapshot = null
        store.hasPurchased = true
        _state.value = CartAbandonmentState(Status.PURCHASED)
    }

    private fun update(snapshot: CartSnapshot) {
        store.snapshot = snapshot
        _state.value = stateFor(snapshot)
    }

    private fun stateFor(snapshot: CartSnapshot?): CartAbandonmentState {
        if (snapshot == null) return CartAbandonmentState(Status.EMPTY)
        val status =
            when (snapshot.outcome) {
                CartSnapshot.Outcome.PENDING -> Status.WAITING
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
}
