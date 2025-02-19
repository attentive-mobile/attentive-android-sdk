package com.attentive.androidsdk

import com.attentive.androidsdk.events.AddToCartEvent
import com.attentive.androidsdk.events.Cart
import com.attentive.androidsdk.events.CustomEvent
import com.attentive.androidsdk.events.Item
import com.attentive.androidsdk.events.Order
import com.attentive.androidsdk.events.Price
import com.attentive.androidsdk.events.ProductViewEvent
import com.attentive.androidsdk.events.PurchaseEvent
import com.attentive.androidsdk.internal.network.AddToCartMetadataDto
import com.attentive.androidsdk.internal.network.CustomEventMetadataDto
import com.attentive.androidsdk.internal.network.Metadata
import com.attentive.androidsdk.internal.network.OrderConfirmedMetadataDto
import com.attentive.androidsdk.internal.network.ProductDto
import com.attentive.androidsdk.internal.network.ProductMetadata
import com.attentive.androidsdk.internal.network.ProductViewMetadataDto
import com.attentive.androidsdk.internal.network.PurchaseMetadataDto
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import java.math.BigDecimal
import java.util.Arrays
import java.util.Currency
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AttentiveApiTestIT {
    lateinit var countDownLatch: CountDownLatch
    lateinit var okHttpClient: OkHttpClient
    lateinit var attentiveApi: AttentiveApi
    lateinit var attentiveApiCallback: AttentiveApiCallback
    val metadataModule = SerializersModule {
        polymorphic(Metadata::class) {
            subclass(ProductMetadata::class)
            subclass(OrderConfirmedMetadataDto::class)
            subclass(CustomEventMetadataDto::class)
        }
        polymorphic(ProductMetadata::class) { // Register ProductMetadata subclasses too
            subclass(AddToCartMetadataDto::class)
            subclass(ProductViewMetadataDto::class)
            subclass(PurchaseMetadataDto::class)
        }
    }
    lateinit var json: Json

    private val requestArgumentCaptor: ArgumentCaptor<Request> = ArgumentCaptor.forClass(
        Request::class.java
    )


    @Before
    fun setup() {
        countDownLatch = CountDownLatch(1)
        okHttpClient = Mockito.spy(OkHttpClient())
        attentiveApi = AttentiveApi(okHttpClient)
        json = Json {
            serializersModule = metadataModule
            classDiscriminator = "className" // Helps identify the subclass
            ignoreUnknownKeys = true
        }
        attentiveApiCallback = object : AttentiveApiCallback {
            override fun onFailure(message: String?) {}

            override fun onSuccess() {
                countDownLatch!!.countDown()
            }
        }
    }



    @Test
    @Throws(InterruptedException::class, SerializationException::class)
    fun sendUserIdentifiersCollectedEvent_userIdentifierCollectedWithAllParams_sendsCorrectUserIdentifierCollectedEvent() {
        // Act
        attentiveApi.sendUserIdentifiersCollectedEvent(
            DOMAIN, ALL_USER_IDENTIFIERS,
            attentiveApiCallback
        )
        countDownLatch.await(EVENT_SEND_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)

        // Assert
        Mockito.verify(okHttpClient, Mockito.times(2))?.newCall(capture(requestArgumentCaptor))
        val uicRequest = requestArgumentCaptor.allValues.stream()
            .filter { request: Request -> request.url.toString().contains("t=idn") }.findFirst()
        Assert.assertTrue(uicRequest.isPresent)
        val url = uicRequest.get().url

        val queryParam = url.queryParameter("m")!!
        val m = json.decodeFromString<Metadata>(queryParam)
        verifyCommonEventFields(url, "idn", m)

        Assert.assertEquals(
            "[{\"id\":\"someClientUserId\",\"vendor\":\"2\"},{\"id\":\"someShopifyId\",\"vendor\":\"0\"},{\"id\":\"someKlaviyoId\",\"vendor\":\"1\"},{\"id\":\"value1\",\"name\":\"key1\",\"vendor\":\"6\"},{\"id\":\"value2\",\"name\":\"key2\",\"vendor\":\"6\"}]",
            url.queryParameter("evs")
        )
    }

    @Test
    @Throws(SerializationException::class, InterruptedException::class)
    fun sendEvent_purchaseEventWithAllParams_sendsCorrectPurchaseAndOrderConfirmedEvents() {
        // Arrange
        val purchaseEvent = buildPurchaseEventWithAllFields()

        // Act
        attentiveApi!!.sendEvent(purchaseEvent, ALL_USER_IDENTIFIERS, DOMAIN, attentiveApiCallback)
        countDownLatch!!.await(EVENT_SEND_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)

        // Assert
        Mockito.verify(okHttpClient, Mockito.times(3)).newCall(capture(requestArgumentCaptor))


        // Verify Purchase event
        val purchaseRequest = requestArgumentCaptor.allValues.stream()
            .filter { request: Request -> request.url.toString().contains("t=p") }.findFirst()
        Assert.assertTrue(purchaseRequest.isPresent)
        val purchaseUrl = purchaseRequest.get().url

        val m = json.decodeFromString<PurchaseMetadataDto>(purchaseUrl.queryParameter("m")!!)
        verifyCommonEventFields(purchaseUrl, "p", m)

        Assert.assertEquals("USD", m.currency)
        val purchasedItem = purchaseEvent.items[0]!!
        Assert.assertEquals(purchasedItem.price.price.toString(), m.price)
        Assert.assertEquals(purchasedItem.productId, m.productId)
        Assert.assertEquals(purchasedItem.productImage, m.image)
        Assert.assertEquals(purchasedItem.name, m.name)
        Assert.assertEquals(purchasedItem.quantity.toString(), m.quantity)
        Assert.assertEquals(purchasedItem.category, m.category)
        Assert.assertEquals(purchasedItem.productVariantId, m.subProductId)

        Assert.assertEquals(purchaseEvent.cart!!.cartId, m.cartId)
        Assert.assertEquals(purchaseEvent.cart!!.cartCoupon, m.cartCoupon)
        Assert.assertEquals(purchaseEvent.order.orderId, m.orderId)


        // Verify Order Confirmed event
        val orderConfirmedRequest = requestArgumentCaptor.allValues.stream()
            .filter { request: Request -> request.url.toString().contains("t=oc") }.findFirst()
        Assert.assertTrue(orderConfirmedRequest.isPresent)
        val orderConfirmedUrl = orderConfirmedRequest.get().url

        val metadata = json.decodeFromString<Metadata>(orderConfirmedUrl.queryParameter("m")!!)

        val ocMetadata = json.decodeFromString<Map<String, String>>(orderConfirmedUrl.queryParameter("m")!!)
        verifyCommonEventFields(orderConfirmedUrl, "oc", metadata)

        Assert.assertEquals(purchaseEvent.order.orderId, ocMetadata["orderId"])
        val expectedItem = purchaseEvent.items[0]!!
        Assert.assertEquals(expectedItem.price.price.toString(), ocMetadata["cartTotal"])
        Assert.assertEquals(
            expectedItem.price.currency.currencyCode,
            ocMetadata["currency"]
        )

        val products: List<ProductDto> = json.decodeFromString(ocMetadata["products"]!!)

        Assert.assertEquals(1, products.size.toLong())
        Assert.assertEquals(expectedItem.price.price.toString(), products[0]?.price)
        Assert.assertEquals(expectedItem.productId, products[0]?.productId)
        Assert.assertEquals(expectedItem.productVariantId, products[0]?.subProductId)
        Assert.assertEquals(expectedItem.category, products[0]?.category)
        Assert.assertEquals(expectedItem.productImage, products[0]?.image)
    }

    @Test
    @Throws(SerializationException::class, InterruptedException::class)
    fun sendEvent_productViewEventWithAllParams_sendsCorrectProductViewEvent() {
        // Arrange
        val productViewEvent = buildProductViewEventWithAllFields()
        // Act
        attentiveApi!!.sendEvent(productViewEvent, ALL_USER_IDENTIFIERS, DOMAIN)
        countDownLatch!!.await(EVENT_SEND_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)

        // Assert
        Mockito.verify(okHttpClient, Mockito.times(2)).newCall(capture(requestArgumentCaptor))

        val addToCartRequest = requestArgumentCaptor.allValues.stream()
            .filter { request: Request -> request.url.toString().contains("t=d") }.findFirst()
        Assert.assertTrue(addToCartRequest.isPresent)
        val url = addToCartRequest.get().url

        val m = json.decodeFromString<ProductViewMetadataDto>(
            url.queryParameter("m")!!
        )
        verifyCommonEventFields(url, "d", m)

        Assert.assertEquals("USD", m.currency)
        val addToCartItem = productViewEvent.items[0]
        Assert.assertEquals(addToCartItem.price.price.toString(), m.price)
        Assert.assertEquals(addToCartItem.productId, m.productId)
        Assert.assertEquals(addToCartItem.productImage, m.image)
        Assert.assertEquals(addToCartItem.name, m.name)
        Assert.assertEquals(addToCartItem.category, m.category)
        Assert.assertEquals(addToCartItem.productVariantId, m.subProductId)
    }

    @Test
    @Throws(SerializationException::class, InterruptedException::class)
    fun sendEvent_addToCartEventWithAllParams_sendsCorrectAddToCartEvent() {

        // Arrange
        val addToCartEvent = buildAddToCartEventWithAllFields()

        // Act
        attentiveApi.sendEvent(addToCartEvent, ALL_USER_IDENTIFIERS, DOMAIN)
        countDownLatch.await(EVENT_SEND_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)

        // Assert
        Mockito.verify(okHttpClient, Mockito.times(2)).newCall(capture(requestArgumentCaptor))
        val addToCartRequest = requestArgumentCaptor.allValues.stream()
            .filter { request: Request -> request.url.toString().contains("t=c") }.findFirst()
        Assert.assertTrue(addToCartRequest.isPresent)
        val url = addToCartRequest.get().url

        val m = json.decodeFromString<AddToCartMetadataDto>(
            url.queryParameter("m")!!
        )
        verifyCommonEventFields(url, "c", m)

        Assert.assertEquals("USD", m.currency)
        val addToCartItem = addToCartEvent.items[0]
        Assert.assertEquals(addToCartItem.price.price.toString(), m.price)
        Assert.assertEquals(addToCartItem.productId, m.productId)
        Assert.assertEquals(addToCartItem.productImage, m.image)
        Assert.assertEquals(addToCartItem.name, m.name)
        Assert.assertEquals(addToCartItem.quantity.toString(), m.quantity)
        Assert.assertEquals(addToCartItem.category, m.category)
        Assert.assertEquals(addToCartItem.productVariantId, m.subProductId)
    }

    @Test
    @Throws(SerializationException::class, InterruptedException::class)
    fun sendEvent_customEventWithAllParams_sendsCorrectCustomEvent() {
        // Arrange
        val customEvent = buildCustomEventWithAllFields()

        // Act
        attentiveApi.sendEvent(customEvent, ALL_USER_IDENTIFIERS, DOMAIN)
        countDownLatch.await(EVENT_SEND_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)

        // Assert
        Mockito.verify(okHttpClient, Mockito.times(2)).newCall(capture(requestArgumentCaptor))
        val customEventRequest = requestArgumentCaptor.allValues.stream()
            .filter { request: Request -> request.url.toString().contains("t=ce") }.findFirst()
        Assert.assertTrue(customEventRequest.isPresent)
        val customEventUrl = customEventRequest.get().url

        val metadata = json.decodeFromString<Metadata>(
            customEventUrl.queryParameter("m")!!
        )
        verifyCommonEventFields(customEventUrl, "ce", metadata)

        val customEventMetadata = json.decodeFromString<Map<String, String>>(
            customEventUrl.queryParameter("m")!!
        )

        Assert.assertEquals(customEvent.type, customEventMetadata["type"])
        val properties = json.decodeFromString<Map<String, String>>(
            customEventMetadata["properties"] as String
        )
        Assert.assertEquals(customEvent.properties, properties)
    }

    companion object {
        private const val DOMAIN = "mobileapps"

        // Update this accordingly when running on VPN
        private const val GEO_ADJUSTED_DOMAIN = "mobileapps"
        private const val EVENT_SEND_TIMEOUT_MS = 5000
        private val ALL_USER_IDENTIFIERS = buildAllUserIdentifiers()

        private fun verifyCommonEventFields(url: HttpUrl, eventType: String, m: Metadata) {
            Assert.assertEquals("modern", url.queryParameter("tag"))
            Assert.assertEquals("mobile-app", url.queryParameter("v"))
            Assert.assertEquals("0", url.queryParameter("lt"))
            Assert.assertEquals(GEO_ADJUSTED_DOMAIN, url.queryParameter("c"))
            Assert.assertEquals(eventType, url.queryParameter("t"))
            Assert.assertEquals(ALL_USER_IDENTIFIERS.visitorId, url.queryParameter("u"))

            Assert.assertEquals(ALL_USER_IDENTIFIERS.phone, m.phone)
            Assert.assertEquals(ALL_USER_IDENTIFIERS.email, m.email)
            Assert.assertEquals("msdk", m.source)
        }

        private fun buildAllUserIdentifiers(): UserIdentifiers {
            return UserIdentifiers.Builder()
                .withClientUserId("someClientUserId")
                .withPhone("+14156667777")
                .withEmail("email@gmail.com")
                .withShopifyId("someShopifyId")
                .withKlaviyoId("someKlaviyoId")
                .withCustomIdentifiers(java.util.Map.of("key1", "value1", "key2", "value2"))
                .build()
        }

        private fun buildPurchaseEventWithAllFields(): PurchaseEvent {
            return PurchaseEvent.Builder(
                listOf(buildItemWithAllFields()),
                Order.Builder("5555").build()
            )
                .cart(Cart.Builder().cartCoupon("cartCoupon").cartId("cartId").build())
                .build()
        }

        private fun buildAddToCartEventWithAllFields(): AddToCartEvent {
            return AddToCartEvent.Builder().items(listOf(buildItemWithAllFields())).buildIt()
        }

        private fun buildProductViewEventWithAllFields(): ProductViewEvent {
            return ProductViewEvent.Builder().items(listOf(buildItemWithAllFields())).buildIt()
        }

        private fun buildItemWithAllFields(): Item {
            return Item.Builder(
                "11",
                "22",
                Price.Builder().price(BigDecimal("15.99")).currency(Currency.getInstance("USD")).build()
            )
                .category("categoryValue")
                .name("nameValue")
                .productImage("imageUrl")
                .build()
        }

        private fun buildCustomEventWithAllFields(): CustomEvent {
            return CustomEvent.Builder(
                "typeValue",
                mapOf("propertyKey1" to "propertyValue1")
            ).buildIt()
        }
    }
}
