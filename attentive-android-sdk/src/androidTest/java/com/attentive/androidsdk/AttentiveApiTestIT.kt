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
import com.attentive.androidsdk.internal.network.Metadata
import com.attentive.androidsdk.internal.network.ProductDto
import com.attentive.androidsdk.internal.network.ProductViewMetadataDto
import com.attentive.androidsdk.internal.network.PurchaseMetadataDto
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
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
import java.util.List
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AttentiveApiTestIT {
    lateinit var countDownLatch: CountDownLatch
    lateinit var objectMapper: ObjectMapper
    lateinit var okHttpClient: OkHttpClient
    lateinit var attentiveApi: AttentiveApi
    lateinit var attentiveApiCallback: AttentiveApiCallback

    private val requestArgumentCaptor: ArgumentCaptor<Request> = ArgumentCaptor.forClass(
        Request::class.java
    )


    @Before
    fun setup() {
        countDownLatch = CountDownLatch(1)
        okHttpClient = Mockito.spy(OkHttpClient())
        objectMapper = ObjectMapper()
        attentiveApi = AttentiveApi(okHttpClient, objectMapper!!)
        attentiveApiCallback = object : AttentiveApiCallback {
            override fun onFailure(message: String?) {}

            override fun onSuccess() {
                countDownLatch!!.countDown()
            }
        }
    }



    @Test
    @Throws(InterruptedException::class, JsonProcessingException::class)
    fun sendUserIdentifiersCollectedEvent_userIdentifierCollectedWithAllParams_sendsCorrectUserIdentifierCollectedEvent() {
        // Act
        attentiveApi!!.sendUserIdentifiersCollectedEvent(
            DOMAIN, ALL_USER_IDENTIFIERS,
            attentiveApiCallback!!
        )
        countDownLatch!!.await(EVENT_SEND_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)

        // Assert
        Mockito.verify(okHttpClient, Mockito.times(2))?.newCall(capture(requestArgumentCaptor))
        val uicRequest = requestArgumentCaptor.allValues.stream()
            .filter { request: Request -> request.url.toString().contains("t=idn") }.findFirst()
        Assert.assertTrue(uicRequest.isPresent)
        val url = uicRequest.get().url

        val m = objectMapper!!.readValue(
            url.queryParameter("m"),
            Metadata::class.java
        )
        verifyCommonEventFields(url, "idn", m)

        Assert.assertEquals(
            "[{\"id\":\"someClientUserId\",\"vendor\":\"2\"},{\"id\":\"someShopifyId\",\"vendor\":\"0\"},{\"id\":\"someKlaviyoId\",\"vendor\":\"1\"},{\"id\":\"value1\",\"name\":\"key1\",\"vendor\":\"6\"},{\"id\":\"value2\",\"name\":\"key2\",\"vendor\":\"6\"}]",
            url.queryParameter("evs")
        )
    }

    @Test
    @Throws(JsonProcessingException::class, InterruptedException::class)
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

        val m = objectMapper!!.readValue(
            purchaseUrl.queryParameter("m"),
            PurchaseMetadataDto::class.java
        )
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

        objectMapper!!.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val metadata = objectMapper!!.readValue(
            orderConfirmedUrl.queryParameter("m"),
            Metadata::class.java
        )
        objectMapper!!.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
        verifyCommonEventFields(orderConfirmedUrl, "oc", metadata)

        // Can't convert directly to a OrderConfirmedMetadataDto because of the special serialization for products
        val ocMetadata = objectMapper.readValue(
            orderConfirmedUrl.queryParameter("m"),
            Map::class.java
        )

//        Map<String, Object> ocMetadata = objectMapper.readValue(orderConfirmedUrl.queryParameter("m"), Map.class);


        Assert.assertEquals(purchaseEvent.order.orderId, ocMetadata["orderId"])
        val expectedItem = purchaseEvent.items[0]!!
        Assert.assertEquals(expectedItem.price.price.toString(), ocMetadata["cartTotal"])
        Assert.assertEquals(
            expectedItem.price.currency.currencyCode,
            ocMetadata["currency"]
        )

        val products = Arrays.asList(
            *objectMapper!!.readValue(
                ocMetadata["products"] as String?,
                Array<ProductDto>::class.java
            ) as Array<ProductDto?>
        )

        Assert.assertEquals(1, products.size.toLong())
        Assert.assertEquals(expectedItem.price.price.toString(), products[0]?.price)
        Assert.assertEquals(expectedItem.productId, products[0]?.productId)
        Assert.assertEquals(expectedItem.productVariantId, products[0]?.subProductId)
        Assert.assertEquals(expectedItem.category, products[0]?.category)
        Assert.assertEquals(expectedItem.productImage, products[0]?.image)
    }

    @Test
    @Throws(JsonProcessingException::class, InterruptedException::class)
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

        val m = objectMapper!!.readValue(
            url.queryParameter("m"),
            ProductViewMetadataDto::class.java
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
    @Throws(JsonProcessingException::class, InterruptedException::class)
    fun sendEvent_addToCartEventWithAllParams_sendsCorrectAddToCartEvent() {
        // Arrange
        val addToCartEvent = buildAddToCartEventWithAllFields()

        // Act
        attentiveApi!!.sendEvent(addToCartEvent, ALL_USER_IDENTIFIERS, DOMAIN)
        countDownLatch!!.await(EVENT_SEND_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)

        // Assert
        Mockito.verify(okHttpClient, Mockito.times(2)).newCall(capture(requestArgumentCaptor))
        val addToCartRequest = requestArgumentCaptor.allValues.stream()
            .filter { request: Request -> request.url.toString().contains("t=c") }.findFirst()
        Assert.assertTrue(addToCartRequest.isPresent)
        val url = addToCartRequest.get().url

        val m = objectMapper!!.readValue(
            url.queryParameter("m"),
            AddToCartMetadataDto::class.java
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
    @Throws(JsonProcessingException::class, InterruptedException::class)
    fun sendEvent_customEventWithAllParams_sendsCorrectCustomEvent() {
        // Arrange
        val customEvent = buildCustomEventWithAllFields()

        // Act
        attentiveApi!!.sendEvent(customEvent, ALL_USER_IDENTIFIERS, DOMAIN)
        countDownLatch!!.await(EVENT_SEND_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)

        // Assert
        Mockito.verify(okHttpClient, Mockito.times(2)).newCall(capture(requestArgumentCaptor))
        val customEventRequest = requestArgumentCaptor.allValues.stream()
            .filter { request: Request -> request.url.toString().contains("t=ce") }.findFirst()
        Assert.assertTrue(customEventRequest.isPresent)
        val customEventUrl = customEventRequest.get().url

        objectMapper!!.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val metadata = objectMapper!!.readValue(
            customEventUrl.queryParameter("m"),
            Metadata::class.java
        )
        objectMapper!!.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
        verifyCommonEventFields(customEventUrl, "ce", metadata)

        // Can't convert directly to a CustomEventMetadataDto because of the special serialization for properties
        val customEventMetadata = objectMapper.readValue(
            customEventUrl.queryParameter("m"),
            MutableMap::class.java
        )

        Assert.assertEquals(customEvent.type, customEventMetadata["type"])
        val properties = objectMapper.readValue(
            customEventMetadata["properties"] as String?,
            MutableMap::class.java
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
                List.of(buildItemWithAllFields()),
                Order.Builder("5555").build()
            )
                .cart(Cart.Builder().cartCoupon("cartCoupon").cartId("cartId").build())
                .build()
        }

        private fun buildAddToCartEventWithAllFields(): AddToCartEvent {
            return AddToCartEvent.Builder(List.of(buildItemWithAllFields())).build()
        }

        private fun buildProductViewEventWithAllFields(): ProductViewEvent {
            return ProductViewEvent.Builder(List.of(buildItemWithAllFields())).build()
        }

        private fun buildItemWithAllFields(): Item {
            return Item.Builder(
                "11",
                "22",
                Price.Builder(BigDecimal("15.99"), Currency.getInstance("USD")).build()
            )
                .category("categoryValue")
                .name("nameValue")
                .productImage("imageUrl")
                .build()
        }

        private fun buildCustomEventWithAllFields(): CustomEvent {
            return CustomEvent.Builder(
                "typeValue",
                java.util.Map.of("propertyKey1", "propertyValue1")
            ).build()
        }
    }
}
