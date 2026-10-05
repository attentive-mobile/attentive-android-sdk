package com.attentive.androidsdk.internal.journeys

import com.attentive.androidsdk.PersistentStorage
import com.attentive.androidsdk.events.AddToCartEvent
import com.attentive.androidsdk.events.Item
import com.attentive.androidsdk.events.Order
import com.attentive.androidsdk.events.Price
import com.attentive.androidsdk.events.ProductViewEvent
import com.attentive.androidsdk.events.PurchaseEvent
import com.attentive.androidsdk.journeys.CartAbandonmentConfig
import com.attentive.androidsdk.journeys.CartAbandonmentState.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Currency
import java.util.concurrent.TimeUnit

class CartAbandonmentTrackerTest {
    private val hour = TimeUnit.HOURS.toMillis(1)
    private var now = 1_000_000L
    private val values = mutableMapOf<String, Any>()
    private lateinit var storage: PersistentStorage
    private val scheduler = FakeScheduler()

    @Before
    fun setUp() {
        storage =
            mock {
                on { read(anyOrNull()) } doAnswer { values[it.getArgument<String>(0)] as String? }
                on { readBoolean(any()) } doAnswer { values[it.getArgument<String>(0)] as Boolean? ?: false }
            }
        whenever(storage.save(anyOrNull(), anyOrNull<String>())).doAnswer {
            values[it.getArgument(0)] = it.getArgument<String>(1)
            Unit
        }
        whenever(storage.save(any(), any<Boolean>())).doAnswer {
            values[it.getArgument(0)] = it.getArgument<Boolean>(1)
            Unit
        }
        whenever(storage.delete(anyOrNull())).doAnswer {
            values.remove(it.getArgument<String>(0))
            Unit
        }
    }

    private fun tracker(config: CartAbandonmentConfig = CartAbandonmentConfig.Builder().build()) =
        CartAbandonmentTracker(config, CartSnapshotStore(storage), scheduler, clock = { now })

    private fun item(id: String, price: String = "30.00", name: String? = null) =
        Item.Builder(id, "$id-variant", Price.Builder().price(BigDecimal(price)).currency(Currency.getInstance("USD")).build())
            .name(name)
            .build()

    private fun addToCart(vararg items: Item) = AddToCartEvent.Builder().items(items.toList()).build()

    private fun purchase() =
        PurchaseEvent.Builder(listOf(item("shirt")), Order.Builder().orderId("order-1").build()).build()

    @Test
    fun addToCart_schedulesCheckAfterDefaultDelay() {
        val tracker = tracker()
        tracker.onEvent(addToCart(item("shirt")))

        assertEquals(listOf(hour), scheduler.scheduled)
        assertEquals(Status.WAITING, tracker.state.value.status)
        assertEquals(now + hour, tracker.state.value.checkAtMillis)
    }

    @Test
    fun repeatedAddToCart_resetsTimerAndMergesItemsByVariant() {
        val tracker = tracker()
        tracker.onEvent(addToCart(item("shirt")))
        now += hour / 2
        tracker.onEvent(addToCart(item("shirt"), item("hat")))

        assertEquals(2, scheduler.scheduled.size)
        assertEquals(now + hour, tracker.state.value.checkAtMillis)
        assertEquals(3, tracker.state.value.itemCount)
    }

    @Test
    fun purchase_cancelsCheckAndClearsCart() {
        val tracker = tracker()
        tracker.onEvent(addToCart(item("shirt")))
        tracker.onEvent(purchase())

        assertEquals(1, scheduler.cancelCount)
        assertEquals(Status.PURCHASED, tracker.state.value.status)
        assertEquals(AbandonmentEvaluation.NoCart, tracker.evaluate(isAppInForeground = false, canNotify = true))
    }

    @Test
    fun evaluate_notifiesOnceWithCartAndConfiguredDeeplink() {
        val tracker = tracker(CartAbandonmentConfig.Builder().cartDeeplink("bonni://cart").build())
        tracker.onEvent(addToCart(item("shirt", name = "Linen Shirt")))
        now += hour

        val evaluation = tracker.evaluate(isAppInForeground = false, canNotify = true)

        assertTrue(evaluation is AbandonmentEvaluation.Notify)
        evaluation as AbandonmentEvaluation.Notify
        assertEquals("bonni://cart", evaluation.deeplink)
        assertEquals("Linen Shirt", evaluation.cart.items.single().name)
        assertEquals(Status.NOTIFIED, tracker.state.value.status)
        assertEquals(AbandonmentEvaluation.AlreadyEvaluated, tracker.evaluate(isAppInForeground = false, canNotify = true))
    }

    @Test
    fun evaluate_belowThreshold_doesNotNotify() {
        val tracker = tracker(CartAbandonmentConfig.Builder().scoreThreshold(0.9).build())
        tracker.onEvent(addToCart(item("shirt")))
        now += hour

        assertTrue(tracker.evaluate(isAppInForeground = false, canNotify = true) is AbandonmentEvaluation.BelowThreshold)
        assertEquals(Status.BELOW_THRESHOLD, tracker.state.value.status)
    }

    @Test
    fun evaluate_withoutPushPermission_doesNotNotify() {
        val tracker = tracker()
        tracker.onEvent(addToCart(item("shirt")))
        now += hour

        assertTrue(tracker.evaluate(isAppInForeground = false, canNotify = false) is AbandonmentEvaluation.PushUnavailable)
        assertEquals(Status.PUSH_UNAVAILABLE, tracker.state.value.status)
    }

    @Test
    fun evaluate_inForeground_defersByAnotherDelay() {
        val tracker = tracker()
        tracker.onEvent(addToCart(item("shirt")))
        now += hour

        assertEquals(AbandonmentEvaluation.Deferred, tracker.evaluate(isAppInForeground = true, canNotify = true))
        assertEquals(listOf(hour, hour), scheduler.scheduled)
        assertEquals(now + hour, tracker.state.value.checkAtMillis)
    }

    @Test
    fun addToCartAfterEvaluation_startsNewCart() {
        val tracker = tracker()
        tracker.onEvent(addToCart(item("shirt")))
        now += hour
        tracker.evaluate(isAppInForeground = false, canNotify = true)

        tracker.onEvent(addToCart(item("hat")))

        assertEquals(Status.WAITING, tracker.state.value.status)
        assertEquals(1, tracker.state.value.itemCount)
    }

    @Test
    fun engagement_raisesLiveScore() {
        val tracker = tracker()
        tracker.onEvent(addToCart(item("shirt")))
        val initial = tracker.state.value.intentScore!!

        tracker.onEvent(ProductViewEvent.Builder().items(listOf(item("shirt"))).build())
        tracker.onAppForegrounded()

        assertTrue(tracker.state.value.intentScore!! > initial)
    }

    @Test
    fun priorPurchase_raisesScoreOfNextCart() {
        val tracker = tracker()
        tracker.onEvent(addToCart(item("shirt")))
        val withoutHistory = tracker.state.value.intentScore!!
        tracker.onEvent(purchase())

        tracker.onEvent(addToCart(item("shirt")))

        assertTrue(tracker.state.value.intentScore!! > withoutHistory)
    }

    @Test
    fun cartSurvivesNewTrackerInstance() {
        tracker().onEvent(addToCart(item("shirt")))

        val restored = tracker()

        assertEquals(Status.WAITING, restored.state.value.status)
        assertTrue(restored.evaluate(isAppInForeground = false, canNotify = true) is AbandonmentEvaluation.Notify)
    }

    @Test
    fun clearUser_cancelsAndForgetsHistory() {
        val tracker = tracker()
        tracker.onEvent(addToCart(item("shirt")))
        tracker.onUserCleared()

        assertEquals(1, scheduler.cancelCount)
        assertEquals(Status.EMPTY, tracker.state.value.status)
        assertNull(tracker.state.value.intentScore)
        assertTrue(values.isEmpty())
    }

    @Test
    fun defaultCopy_namesSingleItem() {
        val cart = com.attentive.androidsdk.journeys.AbandonedCart(listOf(item("shirt", name = "Linen Shirt")), 0.5, now)
        assertEquals(
            "Your Linen Shirt is still waiting. Complete your order before it's gone.",
            CartAbandonmentWorker.defaultCopy(cart).body,
        )
    }

    private class FakeScheduler : AbandonmentScheduler {
        val scheduled = mutableListOf<Long>()
        var cancelCount = 0

        override fun schedule(delayMillis: Long) {
            scheduled += delayMillis
        }

        override fun cancel() {
            cancelCount++
        }
    }
}
