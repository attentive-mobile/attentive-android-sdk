package com.attentive.androidsdk.internal.context

import com.attentive.androidsdk.events.AddToCartEvent
import com.attentive.androidsdk.events.Item
import com.attentive.androidsdk.events.Order
import com.attentive.androidsdk.events.Price
import com.attentive.androidsdk.events.ProductViewEvent
import com.attentive.androidsdk.events.PurchaseEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import timber.log.Timber
import java.math.BigDecimal
import java.util.Currency

class PersistentContextStoreTest {
    private val storage = InMemoryStorage()
    private var now = 0L
    private val store = PersistentContextStore(storage.mock, "com.example.shop") { now }

    @Before
    fun setUp() {
        Timber.uprootAll()
    }

    @Test
    fun onScreenView_longName_truncatesTo64Characters() {
        store.onScreenView("a".repeat(80), 0)

        assertEquals("a".repeat(64), store.screen.currentScreenName)
    }

    @Test
    fun onScreenView_beforeForeground_startsSessionAndTimer() {
        store.onScreenView("home", 0)
        store.onScreenView("cart", 2500)

        assertEquals(2500L, store.screen.previousScreenDurationMs)
        assertEquals(2, store.screen.screenIndex)
    }

    @Test
    fun onForeground_repeated_doesNotResetTimer() {
        store.onForeground(0)
        store.onScreenView("home", 0)
        store.onForeground(1000)
        store.onScreenView("cart", 3000)

        assertEquals(3000L, store.screen.previousScreenDurationMs)
    }

    @Test
    fun onSessionStart_resetsScreensAndLaunchButKeepsProductsAndUtms() {
        store.onForeground(0)
        store.onLaunch(LaunchSource.DEEP_LINK, mapOf(UtmKey.SOURCE to "sms"), "myshop://home")
        store.onScreenView("home", 0)
        store.onProductView(listOf(product("a")))

        store.onSessionStart()

        assertNull(store.screen.currentScreenName)
        assertEquals(0, store.screen.screenIndex)
        assertEquals(LaunchSource.ORGANIC, store.launch.source)
        assertNull(store.launch.landingUrl)
        assertEquals("sms", store.launch.utmParams.getValue(UtmKey.SOURCE).value)
        assertEquals(listOf("a"), store.products.recentlyViewed.map { it.productId })
    }

    @Test
    fun onLaunch_withoutLandingUrl_keepsPreviousLandingUrl() {
        store.onForeground(0)
        store.onLaunch(LaunchSource.DEEP_LINK, emptyMap(), "myshop://product/1")
        store.onLaunch(LaunchSource.PUSH, emptyMap(), null)

        assertEquals(LaunchSource.PUSH, store.launch.source)
        assertEquals("myshop://product/1", store.launch.landingUrl)
    }

    @Test
    fun onAddToCart_manyItems_keepsNewest20() {
        repeat(25) { store.onAddToCart(listOf(product("p$it"))) }

        val cart = store.products.recentlyAddedToCart
        assertEquals(20, cart.size)
        assertEquals("p24", cart.first().productId)
    }

    @Test
    fun update_unchangedState_doesNotWrite() {
        store.onPurchase()
        store.onPurchase()

        assertEquals(0, storage.saveCount)
    }

    @Test
    fun load_unreadableState_startsEmpty() {
        storage.values[PersistentContextStore.STORAGE_KEY] = "not json"

        val reloaded = PersistentContextStore(storage.mock, "com.example.shop") { now }

        assertEquals(0, reloaded.screen.screenIndex)
        assertEquals(emptyList<ContextProduct>(), reloaded.products.recentlyViewed)
    }

    @Test
    fun screenUrl_withoutPackageName_isNull() {
        val noPackage = PersistentContextStore(InMemoryStorage().mock, null) { now }
        noPackage.onScreenView("home", 0)

        assertNull(noPackage.screen.currentScreenUrl)
    }

    @Test
    fun recordEvent_commerceEvents_updateProductHistory() {
        val item = Item.Builder("sku-1", "variant-1", Price(BigDecimal("19.99"), Currency.getInstance("USD")))
            .name("Shirt")
            .productImage("https://cdn.example.com/shirt.png")
            .build()

        store.recordEvent(ProductViewEvent.Builder().items(listOf(item)).build(), 10)
        store.recordEvent(AddToCartEvent.Builder().items(listOf(item)).build(), 20)

        assertEquals(
            ContextProduct("sku-1", "variant-1", "Shirt", "19.99", "USD", "https://cdn.example.com/shirt.png", 10),
            store.products.recentlyViewed.single(),
        )
        assertEquals(20L, store.products.recentlyAddedToCart.single().observedAtMs)

        store.recordEvent(PurchaseEvent.Builder(listOf(item), Order.Builder().orderId("o-1").build()).build(), 30)

        assertEquals(emptyList<ContextProduct>(), store.products.recentlyAddedToCart)
    }

    private fun product(id: String) = ContextProduct(id, null, null, null, null, null, now)
}
