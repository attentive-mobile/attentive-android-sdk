package com.attentive.androidsdk

import com.attentive.androidsdk.events.AddToCartEvent
import com.attentive.androidsdk.events.Cart
import com.attentive.androidsdk.events.CustomEvent
import com.attentive.androidsdk.events.Item
import com.attentive.androidsdk.events.Order
import com.attentive.androidsdk.events.Price
import com.attentive.androidsdk.events.ProductViewEvent
import com.attentive.androidsdk.events.PurchaseEvent
import com.attentive.androidsdk.internal.network.events.*
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.math.BigDecimal
import java.util.Currency

/**
 * Tests for the new BaseEventRequest mapping functions.
 * Note: These tests verify the transformation logic only and skip Base64 encoding verification
 * which requires Android runtime.
 */
class BaseEventRequestMapperTest {
    private lateinit var attentiveApi: AttentiveApi
    private lateinit var okHttpClient: OkHttpClient

    @Before
    fun setup() {
        okHttpClient = OkHttpClient()
        attentiveApi = AttentiveApi(okHttpClient, "test-domain")
    }

    // Test mapPurchaseEvent
    @Test
    fun mapPurchaseEvent_withRequiredFields_createsValidBaseEventRequest() {
        // Arrange
        val purchaseEvent = buildPurchaseEventWithRequiredFields()
        val userIdentifiers = buildAllUserIdentifiers()
        val domain = "test-domain.com"

        // Act
        val result = invokeGetBaseEventRequestsFromEvent(purchaseEvent, userIdentifiers, domain)

        // Assert
        assertEquals(1, result.size)
        val request = result[0]

        assertEquals(userIdentifiers.visitorId, request.visitorId)
        assertEquals(domain, request.attentiveDomain)
        assertEquals(EventType.Purchase, request.eventType)
        assertEquals(SourceType.mobile, request.sourceType)
        assertNotNull(request.timestamp)

        // Verify metadata
        assertTrue(request.eventMetadata is PurchaseMetadata)
        val metadata = request.eventMetadata as PurchaseMetadata
        assertEquals("ORDER123", metadata.orderId)
        assertEquals("USD", metadata.currency)
        assertEquals("15.99", metadata.orderTotal)
        assertEquals(1, metadata.products?.size)
    }

    @Test
    fun mapPurchaseEvent_withAllFields_includesCartInformation() {
        // Arrange
        val purchaseEvent = buildPurchaseEventWithAllFields()
        val userIdentifiers = buildAllUserIdentifiers()
        val domain = "test-domain.com"

        // Act
        val result = invokeGetBaseEventRequestsFromEvent(purchaseEvent, userIdentifiers, domain)

        // Assert
        assertEquals(1, result.size)
        val metadata = result[0].eventMetadata as PurchaseMetadata

        assertNotNull(metadata.cart)
        assertEquals("CART123", metadata.cart?.cartId)
        assertEquals("SUMMER20", metadata.cart?.cartCoupon)

        assertNotNull(metadata.products)
        assertEquals(1, metadata.products?.size)

        val product = metadata.products?.get(0)
        assertEquals("PROD123", product?.productId)
        assertEquals("VARIANT456", product?.variantId)
        assertEquals("Test Product", product?.name)
        assertEquals("15.99", product?.price)
        assertEquals(2, product?.quantity)
    }

    @Test
    fun mapPurchaseEvent_withMultipleItems_calculatesCorrectTotal() {
        // Arrange
        val purchaseEvent = buildPurchaseEventWithTwoItems()
        val userIdentifiers = buildAllUserIdentifiers()
        val domain = "test-domain.com"

        // Act
        val result = invokeGetBaseEventRequestsFromEvent(purchaseEvent, userIdentifiers, domain)

        // Assert
        assertEquals(1, result.size)
        val metadata = result[0].eventMetadata as PurchaseMetadata

        // 15.99 + 25.00 = 40.99
        assertEquals("40.99", metadata.orderTotal)
        assertEquals(2, metadata.products?.size)
    }

    @Test
    fun mapPurchaseEvent_withEmptyItems_returnsEmptyList() {
        // Mock Android Log to avoid NoSuchMethodError when Timber tries to log
        val logMock = Mockito.mockStatic(android.util.Log::class.java)
        try {
            logMock.`when`<Int> {
                android.util.Log.w(Mockito.anyString(), Mockito.anyString())
            }.thenReturn(0)

            // Arrange
            val purchaseEvent = PurchaseEvent.Builder(
                emptyList(),
                Order.Builder().orderId("ORDER123").build()
            ).build()
            val userIdentifiers = buildAllUserIdentifiers()

            // Act
            val result = invokeGetBaseEventRequestsFromEvent(purchaseEvent, userIdentifiers, "test")

            // Assert
            assertTrue(result.isEmpty())
        } finally {
            logMock.close()
        }
    }

    // Test mapProductViewEvent
    @Test
    fun mapProductViewEvent_withSingleItem_createsOneRequest() {
        // Arrange
        val productViewEvent = buildProductViewEventWithAllFields()
        val userIdentifiers = buildAllUserIdentifiers()
        val domain = "test-domain.com"

        // Act
        val result = invokeGetBaseEventRequestsFromEvent(productViewEvent, userIdentifiers, domain)

        // Assert
        assertEquals(1, result.size)
        val request = result[0]

        assertEquals(EventType.ProductView, request.eventType)
        assertTrue(request.eventMetadata is ProductViewMetadata)

        val metadata = request.eventMetadata as ProductViewMetadata
        assertEquals("USD", metadata.currency)
        assertNotNull(metadata.product)
        assertEquals("PROD123", metadata.product?.productId)
    }

    @Test
    fun mapProductViewEvent_withMultipleItems_createsMultipleRequests() {
        // Arrange
        val item1 = buildItemWithAllFields("PROD1", "VAR1")
        val item2 = buildItemWithAllFields("PROD2", "VAR2")
        val productViewEvent = ProductViewEvent.Builder()
            .items(listOf(item1, item2))
            .build()
        val userIdentifiers = buildAllUserIdentifiers()

        // Act
        val result = invokeGetBaseEventRequestsFromEvent(productViewEvent, userIdentifiers, "test")

        // Assert
        assertEquals(2, result.size)

        val metadata1 = result[0].eventMetadata as ProductViewMetadata
        val metadata2 = result[1].eventMetadata as ProductViewMetadata

        assertEquals("PROD1", metadata1.product?.productId)
        assertEquals("PROD2", metadata2.product?.productId)
    }

    @Test
    fun mapProductViewEvent_withDeeplink_includesDeeplinkInRequest() {
        // Arrange
        val productViewEvent = ProductViewEvent.Builder()
            .items(listOf(buildItemWithAllFields()))
            .deeplink("app://product/123")
            .build()
        val userIdentifiers = buildAllUserIdentifiers()

        // Act
        val result = invokeGetBaseEventRequestsFromEvent(productViewEvent, userIdentifiers, "test")

        // Assert
        assertEquals(1, result.size)
        assertEquals("app://product/123", result[0].referrer)
        assertEquals("app://product/123", result[0].locationHref)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mapProductViewEvent_withEmptyItems_throwsException() {
        // Act & Assert - should throw IllegalArgumentException
        ProductViewEvent.Builder().items(emptyList()).build()
    }

    // Test mapAddToCartEvent
    @Test
    fun mapAddToCartEvent_withSingleItem_createsOneRequest() {
        // Arrange
        val addToCartEvent = buildAddToCartEventWithAllFields()
        val userIdentifiers = buildAllUserIdentifiers()
        val domain = "test-domain.com"

        // Act
        val result = invokeGetBaseEventRequestsFromEvent(addToCartEvent, userIdentifiers, domain)

        // Assert
        assertEquals(1, result.size)
        val request = result[0]

        assertEquals(EventType.AddToCart, request.eventType)
        assertTrue(request.eventMetadata is AddToCartMetadata)

        val metadata = request.eventMetadata as AddToCartMetadata
        assertEquals("USD", metadata.currency)
        assertNotNull(metadata.product)
        assertEquals("PROD123", metadata.product?.productId)
        assertEquals("15.99", metadata.product?.price)
    }

    @Test
    fun mapAddToCartEvent_withMultipleItems_createsMultipleRequests() {
        // Arrange
        val item1 = buildItemWithAllFields("PROD1", "VAR1")
        val item2 = buildItemWithAllFields("PROD2", "VAR2")
        val addToCartEvent = AddToCartEvent.Builder()
            .items(listOf(item1, item2))
            .build()
        val userIdentifiers = buildAllUserIdentifiers()

        // Act
        val result = invokeGetBaseEventRequestsFromEvent(addToCartEvent, userIdentifiers, "test")

        // Assert
        assertEquals(2, result.size)

        val metadata1 = result[0].eventMetadata as AddToCartMetadata
        val metadata2 = result[1].eventMetadata as AddToCartMetadata

        assertEquals("PROD1", metadata1.product?.productId)
        assertEquals("PROD2", metadata2.product?.productId)
    }

    @Test
    fun mapAddToCartEvent_withDeeplink_includesDeeplinkInRequest() {
        // Arrange
        val addToCartEvent = AddToCartEvent.Builder()
            .items(listOf(buildItemWithAllFields()))
            .deeplink("app://cart")
            .build()
        val userIdentifiers = buildAllUserIdentifiers()

        // Act
        val result = invokeGetBaseEventRequestsFromEvent(addToCartEvent, userIdentifiers, "test")

        // Assert
        assertEquals(1, result.size)
        assertEquals("app://cart", result[0].referrer)
        assertEquals("app://cart", result[0].locationHref)
    }

    // Test mapCustomEvent
    @Test
    fun mapCustomEvent_withProperties_createsValidRequest() {
        // Arrange
        val customEvent = buildCustomEventWithAllFields()
        val userIdentifiers = buildAllUserIdentifiers()
        val domain = "test-domain.com"

        // Act
        val result = invokeGetBaseEventRequestsFromEvent(customEvent, userIdentifiers, domain)

        // Assert
        assertEquals(1, result.size)
        val request = result[0]

        assertEquals(EventType.MobileCustomEvent, request.eventType)
        assertTrue(request.eventMetadata is MobileCustomEventMetadata)

        val metadata = request.eventMetadata as MobileCustomEventMetadata
        assertNotNull(metadata.customProperties)
        assertEquals("bar", metadata.customProperties?.get("foo"))
        assertEquals("world", metadata.customProperties?.get("hello"))
    }

    @Test
    fun mapCustomEvent_withEmptyProperties_createsRequestWithNoProperties() {
        // Arrange
        val customEvent = CustomEvent.Builder("TestEvent", emptyMap()).build()
        val userIdentifiers = buildAllUserIdentifiers()

        // Act
        val result = invokeGetBaseEventRequestsFromEvent(customEvent, userIdentifiers, "test")

        // Assert
        assertEquals(1, result.size)
        val metadata = result[0].eventMetadata as MobileCustomEventMetadata
        assertTrue(metadata.customProperties?.isEmpty() ?: true)
    }

    // Test buildIdentifiers
    @Test
    fun buildIdentifiers_withAllIdentifiers_mapsAllFields() {
        // Mock Base64 to return encoded strings
        val base64Mock = Mockito.mockStatic(android.util.Base64::class.java)
        try {
            base64Mock.`when`<String> {
                android.util.Base64.encodeToString(Mockito.any(), Mockito.anyInt())
            }.thenAnswer { invocation ->
                // Return a mock base64 string based on the input bytes
                val bytes = invocation.getArgument<ByteArray>(0)
                "base64_${String(bytes)}"
            }

            // Arrange
            val userIdentifiers = buildAllUserIdentifiers()

            // Act
            val result = invokeBuildIdentifiers(userIdentifiers)

            // Assert
            assertNotNull(result.encryptedEmail)
            assertNotNull(result.encryptedPhone)
            assertNotNull(result.otherIdentifiers)

            val otherIds = result.otherIdentifiers!!
            assertTrue(otherIds.any { it.idType == IdType.ClientUserId && it.value == "clientUser123" })
            assertTrue(otherIds.any { it.idType == IdType.ShopifyId && it.value == "shopify456" })
            assertTrue(otherIds.any { it.idType == IdType.KlaviyoId && it.value == "klaviyo789" })
        } finally {
            base64Mock.close()
        }
    }

    @Test
    fun buildIdentifiers_withCustomIdentifiers_mapsCustomIds() {
        // Arrange
        val userIdentifiers = UserIdentifiers.Builder()
            .withVisitorId("visitor123")
            .withCustomIdentifiers(mapOf("myCustomKey" to "myCustomValue"))
            .build()

        // Act
        val result = invokeBuildIdentifiers(userIdentifiers)

        // Assert
        assertNotNull(result.otherIdentifiers)
        val customId = result.otherIdentifiers!!.find { it.idType == IdType.CustomId }
        assertNotNull(customId)
        assertEquals("myCustomValue", customId?.value)
        assertEquals("myCustomKey", customId?.name)
    }

    @Test
    fun buildIdentifiers_withNoOptionalIdentifiers_returnsMinimalIdentifiers() {
        // Arrange
        val userIdentifiers = UserIdentifiers.Builder()
            .withVisitorId("visitor123")
            .build()

        // Act
        val result = invokeBuildIdentifiers(userIdentifiers)

        // Assert
        assertNull(result.encryptedEmail)
        assertNull(result.encryptedPhone)
        assertNull(result.otherIdentifiers)
    }

    @Test
    fun buildIdentifiers_encodesEmailAndPhoneAsBase64() {
        // Mock Base64 to return encoded strings
        val base64Mock = Mockito.mockStatic(android.util.Base64::class.java)
        try {
            base64Mock.`when`<String> {
                android.util.Base64.encodeToString(Mockito.any(), Mockito.anyInt())
            }.thenAnswer { invocation ->
                // Return a mock base64 string based on the input bytes
                val bytes = invocation.getArgument<ByteArray>(0)
                "base64_${String(bytes)}"
            }

            // Arrange
            val userIdentifiers = UserIdentifiers.Builder()
                .withVisitorId("visitor123")
                .withEmail("test@example.com")
                .withPhone("+15551234567")
                .build()

            // Act
            val result = invokeBuildIdentifiers(userIdentifiers)

            // Assert
            assertNotNull(result.encryptedEmail)
            assertNotNull(result.encryptedPhone)

            // Verify the encoding was called (Base64 mocking is working)
            assertEquals("base64_test@example.com", result.encryptedEmail)
            assertEquals("base64_+15551234567", result.encryptedPhone)
        } finally {
            base64Mock.close()
        }
    }

    // Test itemToProduct
    @Test
    fun itemToProduct_withAllFields_mapsAllProperties() {
        // Arrange
        val item = buildItemWithAllFields()

        // Act
        val result = invokeItemToProduct(item)

        // Assert
        assertEquals("PROD123", result.productId)
        assertEquals("VARIANT456", result.variantId)
        assertEquals("Test Product", result.name)
        assertEquals("https://example.com/image.jpg", result.imageUrl)
        assertEquals("15.99", result.price)
        assertEquals(2, result.quantity)
        assertNotNull(result.categories)
        assertEquals("Electronics", result.categories?.get(0))
    }

    @Test
    fun itemToProduct_withNullOptionalFields_handlesNullsCorrectly() {
        // Arrange
        val item = Item.Builder(
            "PROD123",
            "VARIANT456",
            Price.Builder().price(BigDecimal("10.00")).currency(Currency.getInstance("USD")).build()
        ).build()

        // Act
        val result = invokeItemToProduct(item)

        // Assert
        assertEquals("PROD123", result.productId)
        assertEquals("VARIANT456", result.variantId)
        assertNull(result.name)
        assertNull(result.imageUrl)
        assertNull(result.categories)
    }

    // Test cartToCartModel
    @Test
    fun cartToCartModel_withAllFields_mapsCorrectly() {
        // Arrange
        val cart = Cart.Builder()
            .cartId("CART123")
            .cartCoupon("SUMMER20")
            .build()

        // Act
        val result = invokeCartToCartModel(cart)

        // Assert
        assertEquals("CART123", result.cartId)
        assertEquals("SUMMER20", result.cartCoupon)
        assertNull(result.cartTotal)
        assertNull(result.cartDiscount)
    }

    // Test calculateCartTotal
    @Test
    fun calculateCartTotal_withMultipleItems_calculatesCorrectSum() {
        // Arrange
        val items = listOf(
            buildItemWithPrice(BigDecimal("15.99")),
            buildItemWithPrice(BigDecimal("25.00")),
            buildItemWithPrice(BigDecimal("9.50"))
        )

        // Act
        val result = invokeCalculateCartTotal(items)

        // Assert
        assertEquals("50.49", result)
    }

    @Test
    fun calculateCartTotal_withSingleItem_returnsItemPrice() {
        // Arrange
        val items = listOf(buildItemWithPrice(BigDecimal("15.99")))

        // Act
        val result = invokeCalculateCartTotal(items)

        // Assert
        assertEquals("15.99", result)
    }

    @Test
    fun calculateCartTotal_roundsToTwoDecimals() {
        // Arrange
        val items = listOf(
            buildItemWithPrice(BigDecimal("10.999")),
            buildItemWithPrice(BigDecimal("5.999"))
        )

        // Act
        val result = invokeCalculateCartTotal(items)

        // Assert
        assertEquals("16.98", result) // Should round down
    }

    // Helper methods to call internal functions
    // Since these are marked internal, we can call them directly from tests in the same module
    private fun invokeGetBaseEventRequestsFromEvent(
        event: com.attentive.androidsdk.events.Event,
        userIdentifiers: UserIdentifiers,
        domain: String
    ): List<BaseEventRequest> {
        return attentiveApi.getBaseEventRequestsFromEvent(event, userIdentifiers, domain)
    }

    private fun invokeBuildIdentifiers(userIdentifiers: UserIdentifiers): Identifiers {
        return attentiveApi.buildIdentifiers(userIdentifiers)
    }

    private fun invokeItemToProduct(item: Item): Product {
        return attentiveApi.itemToProduct(item)
    }

    private fun invokeCartToCartModel(cart: Cart): com.attentive.androidsdk.internal.network.events.Cart {
        return attentiveApi.cartToCartModel(cart)
    }

    private fun invokeCalculateCartTotal(items: List<Item>): String {
        return attentiveApi.calculateCartTotal(items)
    }

    // Builder helper methods
    private fun buildPurchaseEventWithRequiredFields(): PurchaseEvent {
        return PurchaseEvent.Builder(
            listOf(buildItemWithAllFields()),
            Order.Builder().orderId("ORDER123").build()
        ).build()
    }

    private fun buildPurchaseEventWithAllFields(): PurchaseEvent {
        return PurchaseEvent.Builder(
            listOf(buildItemWithAllFields()),
            Order.Builder().orderId("ORDER123").build()
        ).cart(
            Cart.Builder()
                .cartId("CART123")
                .cartCoupon("SUMMER20")
                .build()
        ).build()
    }

    private fun buildPurchaseEventWithTwoItems(): PurchaseEvent {
        return PurchaseEvent.Builder(
            listOf(
                buildItemWithPrice(BigDecimal("15.99")),
                buildItemWithPrice(BigDecimal("25.00"))
            ),
            Order.Builder().orderId("ORDER123").build()
        ).build()
    }

    private fun buildProductViewEventWithAllFields(): ProductViewEvent {
        return ProductViewEvent.Builder()
            .items(listOf(buildItemWithAllFields()))
            .build()
    }

    private fun buildAddToCartEventWithAllFields(): AddToCartEvent {
        return AddToCartEvent.Builder()
            .items(listOf(buildItemWithAllFields()))
            .build()
    }

    private fun buildCustomEventWithAllFields(): CustomEvent {
        return CustomEvent.Builder(
            "TestEvent",
            mapOf("foo" to "bar", "hello" to "world")
        ).build()
    }

    private fun buildItemWithAllFields(
        productId: String = "PROD123",
        variantId: String = "VARIANT456"
    ): Item {
        return Item.Builder(
            productId,
            variantId,
            Price.Builder()
                .price(BigDecimal("15.99"))
                .currency(Currency.getInstance("USD"))
                .build()
        )
            .name("Test Product")
            .productImage("https://example.com/image.jpg")
            .category("Electronics")
            .quantity(2)
            .build()
    }

    private fun buildItemWithPrice(price: BigDecimal): Item {
        return Item.Builder(
            "PROD123",
            "VARIANT456",
            Price.Builder()
                .price(price)
                .currency(Currency.getInstance("USD"))
                .build()
        ).build()
    }

    private fun buildAllUserIdentifiers(): UserIdentifiers {
        return UserIdentifiers.Builder()
            .withVisitorId("visitor123")
            .withEmail("test@example.com")
            .withPhone("+15551234567")
            .withClientUserId("clientUser123")
            .withShopifyId("shopify456")
            .withKlaviyoId("klaviyo789")
            .build()
    }
}
