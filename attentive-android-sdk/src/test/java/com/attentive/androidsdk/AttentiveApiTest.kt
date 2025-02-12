package com.attentive.androidsdk

import android.util.Log
import com.attentive.androidsdk.AttentiveApi.GetGeoAdjustedDomainCallback
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
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import java.math.BigDecimal
import java.util.Arrays
import java.util.Currency
import java.util.Locale

class AttentiveApiTest {
    lateinit var attentiveApi: AttentiveApi
    lateinit var okHttpClient: OkHttpClient
    lateinit var objectMapper: ObjectMapper

    @Before
    fun setup() {
        okHttpClient = Mockito.mock(OkHttpClient::class.java)

        objectMapper = ObjectMapper()

        attentiveApi = Mockito.spy(AttentiveApi(okHttpClient, objectMapper!!))
    }

    @Test
    @Throws(JsonProcessingException::class)
    fun sendUserIdentifiersCollectedEvent_userIdentifierCollectedEvent_callsOkHttpClientWithCorrectPayload() {
        // Arrange
        givenAttentiveApiGetsGeoAdjustedDomainSuccessfully()
        givenOkHttpClientReturnsSuccessFromEventsEndpoint()

        // Act
        attentiveApi!!.sendUserIdentifiersCollectedEvent(
            DOMAIN,
            ALL_USER_IDENTIFIERS,
            object : AttentiveApiCallback {
                private val tag = "AttentiveApiTest"

                override fun onFailure(message: String?) {
                    Log.e(tag, "Could not send the user identifiers. Error: $message")
                }

                override fun onSuccess() {
                    Log.i(tag, "Successfully sent the user identifiers")
                }
            })

        // Assert
        val requestArgumentCaptor = ArgumentCaptor.forClass(
            Request::class.java
        )
        Mockito.verify(okHttpClient, Mockito.times(1)).newCall(requestArgumentCaptor.capture())
        val userIdentifiersRequest = requestArgumentCaptor.allValues.stream().findFirst()
        Assert.assertTrue(userIdentifiersRequest.isPresent)
        val url = userIdentifiersRequest.get().url

        objectMapper!!.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        verifyCommonEventFields(
            url, "idn", objectMapper!!.readValue(
                url.queryParameter("m"),
                Metadata::class.java
            )
        )
        objectMapper!!.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)

        Assert.assertEquals(
            "[{\"vendor\":\"2\",\"id\":\"someClientUserId\"},{\"vendor\":\"0\",\"id\":\"someShopifyId\"},{\"vendor\":\"1\",\"id\":\"someKlaviyoId\"}]",
            url.queryParameter("evs")
        )
    }

    @Test
    fun sendEvent_validEvent_httpMethodIsPost() {
        // Arrange
        givenAttentiveApiGetsGeoAdjustedDomainSuccessfully()
        givenOkHttpClientReturnsSuccessFromEventsEndpoint()
        // Which event we send for this test doesn't matter - choosing AddToCart randomly
        val addToCartEvent = buildAddToCartEventWithAllFields()

        // Act
        attentiveApi!!.sendEvent(addToCartEvent, ALL_USER_IDENTIFIERS, DOMAIN)

        // Assert
        val requestArgumentCaptor = ArgumentCaptor.forClass(
            Request::class.java
        )
        Mockito.verify(okHttpClient, Mockito.times(1)).newCall(capture(requestArgumentCaptor))
        val addToCartRequest = requestArgumentCaptor.allValues.stream()
            .filter { request: Request -> request.url.toString().contains("t=c") }.findFirst()
        Assert.assertTrue(addToCartRequest.isPresent)
        val request = addToCartRequest.get()
        Assert.assertEquals("POST", request.method.uppercase(Locale.getDefault()))
    }

    @Test
    @Throws(JsonProcessingException::class)
    fun sendEvent_purchaseEventWithOnlyRequiredParams_callsOkHttpClientWithCorrectPayload() {
        // Arrange
        givenAttentiveApiGetsGeoAdjustedDomainSuccessfully()
        givenOkHttpClientReturnsSuccessFromEventsEndpoint()
        val purchaseEvent = buildPurchaseEventWithRequiredFields()

        // Act
        attentiveApi!!.sendEvent(purchaseEvent, ALL_USER_IDENTIFIERS, DOMAIN)

        // Assert
        val requestArgumentCaptor = ArgumentCaptor.forClass(
            Request::class.java
        )
        Mockito.verify(okHttpClient, Mockito.times(2)).newCall(requestArgumentCaptor.capture())
        val purchaseRequest = requestArgumentCaptor.allValues.stream()
            .filter { request: Request -> request.url.toString().contains("t=p") }.findFirst()
        Assert.assertTrue(purchaseRequest.isPresent)
        assertRequestMethodIsPost(purchaseRequest.get())
        val url = purchaseRequest.get().url

        val m = objectMapper!!.readValue(
            url.queryParameter("m"),
            PurchaseMetadataDto::class.java
        )
        verifyCommonEventFields(url, "p", m)
        Assert.assertEquals("USD", m.currency)
        val purchasedItem = purchaseEvent.items[0]!!
        Assert.assertEquals(purchasedItem.price.price.toString(), m.price)
        Assert.assertEquals(purchasedItem.productId, m.productId)
        Assert.assertEquals(purchasedItem.productVariantId, m.subProductId)
        Assert.assertEquals(purchaseEvent.order.orderId, m.orderId)
    }

    @Test
    @Throws(JsonProcessingException::class)
    fun sendEvent_purchaseEventWithAllParams_callsOkHttpClientWithCorrectPayload() {
        // Arrange
        givenAttentiveApiGetsGeoAdjustedDomainSuccessfully()
        givenOkHttpClientReturnsSuccessFromEventsEndpoint()
        val purchaseEvent = buildPurchaseEventWithAllFields()

        // Act
        attentiveApi!!.sendEvent(purchaseEvent, ALL_USER_IDENTIFIERS, DOMAIN)

        // Assert
        val requestArgumentCaptor = ArgumentCaptor.forClass(
            Request::class.java
        )
        Mockito.verify(okHttpClient, Mockito.times(2))?.newCall(requestArgumentCaptor.capture())
        val purchaseRequest = requestArgumentCaptor.allValues.stream()
            .filter { request: Request -> request.url.toString().contains("t=p") }.findFirst()
        Assert.assertTrue(purchaseRequest.isPresent)
        assertRequestMethodIsPost(purchaseRequest.get())
        val url = purchaseRequest.get().url
        val m = objectMapper!!.readValue(
            url.queryParameter("m"),
            PurchaseMetadataDto::class.java
        )
        verifyCommonEventFields(url, "p", m)

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
    }

    @Test
    @Throws(JsonProcessingException::class)
    fun sendEvent_purchaseEventWithAllParams_sendsCorrectOrderConfirmedEvent() {
        // Arrange
        givenAttentiveApiGetsGeoAdjustedDomainSuccessfully()
        givenOkHttpClientReturnsSuccessFromEventsEndpoint()
        val purchaseEvent = buildPurchaseEventWithAllFields()

        // Act
        attentiveApi!!.sendEvent(purchaseEvent, ALL_USER_IDENTIFIERS, DOMAIN)

        // Assert
        val requestArgumentCaptor = ArgumentCaptor.forClass(
            Request::class.java
        )
        Mockito.verify(okHttpClient, Mockito.times(2))?.newCall(requestArgumentCaptor.capture())
        val orderConfirmedRequest = requestArgumentCaptor.allValues.stream()
            .filter { request: Request -> request.url.toString().contains("t=oc") }.findFirst()
        Assert.assertTrue(orderConfirmedRequest.isPresent)
        assertRequestMethodIsPost(orderConfirmedRequest.get())
        val url = orderConfirmedRequest.get().url

        objectMapper!!.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        verifyCommonEventFields(
            url, "oc", objectMapper!!.readValue(
                url.queryParameter("m"),
                Metadata::class.java
            )
        )
        objectMapper!!.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)

        val metadataString = url.queryParameter("m")
        val metadata = objectMapper.readValue(
            metadataString,
            Map::class.java
        )
        Assert.assertEquals(purchaseEvent.order.orderId, metadata["orderId"])
        val expectedItem = purchaseEvent.items[0]!!
        Assert.assertEquals(expectedItem.price.price.toString(), metadata["cartTotal"])
        Assert.assertEquals(
            expectedItem.price.currency.currencyCode,
            metadata["currency"]
        )

        val products = Arrays.asList(
            *objectMapper!!.readValue(
                metadata["products"] as String?,
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
    fun sendEvent_purchaseEventWithTwoProducts_callsEventsEndpointTwiceForPurchasesAndOnceForOrderConfirmed() {
        // Arrange
        givenAttentiveApiGetsGeoAdjustedDomainSuccessfully()
        givenOkHttpClientReturnsSuccessFromEventsEndpoint()
        val purchaseEvent = buildPurchaseEventWithTwoItems()

        // Act
        attentiveApi!!.sendEvent(purchaseEvent, ALL_USER_IDENTIFIERS, DOMAIN)

        // Assert
        val requestArgumentCaptor = ArgumentCaptor.forClass(
            Request::class.java
        )
        Mockito.verify(okHttpClient, Mockito.times(3))?.newCall(requestArgumentCaptor.capture())
        val allValues: List<Request> = ArrayList(requestArgumentCaptor.allValues)
        Assert.assertEquals(3, allValues.size.toLong())

        var purchaseCount = 0
        var orderConfirmedCount = 0
        for (request in allValues) {
            val urlString = request.url.toString()
            if (urlString.contains("t=p")) {
                purchaseCount++
            } else if (urlString.contains("t=oc")) {
                orderConfirmedCount++
            } else {
                Assert.fail("Unknown event type was sent to the server")
            }
        }
        Assert.assertEquals(2, purchaseCount.toLong())
        Assert.assertEquals(1, orderConfirmedCount.toLong())
    }

    @Test
    @Throws(JsonProcessingException::class)
    fun sendEvent_addToCartEventWithAllParams_callsOkHttpClientWithCorrectPayload() {
        // Arrange
        givenAttentiveApiGetsGeoAdjustedDomainSuccessfully()
        givenOkHttpClientReturnsSuccessFromEventsEndpoint()
        val addToCartEvent = buildAddToCartEventWithAllFields()

        // Act
        attentiveApi!!.sendEvent(addToCartEvent, ALL_USER_IDENTIFIERS, DOMAIN)

        // Assert
        val requestArgumentCaptor = ArgumentCaptor.forClass(
            Request::class.java
        )
        Mockito.verify(okHttpClient, Mockito.times(1))?.newCall(requestArgumentCaptor.capture())
        val addToCartRequest = requestArgumentCaptor.allValues.stream()
            .filter { request: Request -> request.url.toString().contains("t=c") }.findFirst()
        Assert.assertTrue(addToCartRequest.isPresent)
        assertRequestMethodIsPost(addToCartRequest.get())
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
    @Throws(JsonProcessingException::class)
    fun sendEvent_productViewEventWithAllParams_callsOkHttpClientWithCorrectPayload() {
        // Arrange
        givenAttentiveApiGetsGeoAdjustedDomainSuccessfully()
        givenOkHttpClientReturnsSuccessFromEventsEndpoint()
        val productViewEvent = buildProductViewEventWithAllFields()

        // Act
        attentiveApi!!.sendEvent(productViewEvent, ALL_USER_IDENTIFIERS, DOMAIN)

        // Assert
        val requestArgumentCaptor = ArgumentCaptor.forClass(
            Request::class.java
        )
        Mockito.verify(okHttpClient, Mockito.times(1))?.newCall(requestArgumentCaptor.capture())
        val addToCartRequest = requestArgumentCaptor.allValues.stream()
            .filter { request: Request -> request.url.toString().contains("t=d") }.findFirst()
        Assert.assertTrue(addToCartRequest.isPresent)
        assertRequestMethodIsPost(addToCartRequest.get())
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
    @Throws(JsonProcessingException::class)
    fun sendEvent_customEventWithAllParams_callsOkHttpClientWithCorrectPayload() {
        // Arrange
        givenAttentiveApiGetsGeoAdjustedDomainSuccessfully()
        givenOkHttpClientReturnsSuccessFromEventsEndpoint()
        val customEvent = buildCustomEventWithAllFields()

        // Act
        attentiveApi!!.sendEvent(customEvent, ALL_USER_IDENTIFIERS, DOMAIN)

        // Assert
        val requestArgumentCaptor = ArgumentCaptor.forClass(
            Request::class.java
        )
        Mockito.verify(okHttpClient, Mockito.times(1))?.newCall(requestArgumentCaptor.capture())
        val customEventRequest = requestArgumentCaptor.allValues.stream()
            .filter { request: Request -> request.url.toString().contains("t=ce") }.findFirst()
        Assert.assertTrue(customEventRequest.isPresent)
        assertRequestMethodIsPost(customEventRequest.get())
        val url = customEventRequest.get().url

        val metadataString = url.queryParameter("m")
        objectMapper!!.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        verifyCommonEventFields(
            url, "ce", objectMapper!!.readValue(
                metadataString,
                Metadata::class.java
            )
        )
        objectMapper!!.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)

        val metadata = objectMapper.readValue(
            metadataString,
            MutableMap::class.java
        )
        val actualProperties = (objectMapper.readValue(
            metadata["properties"] as String?,
            MutableMap::class.java
        ))
        Assert.assertEquals(customEvent.type, metadata["type"])
        Assert.assertEquals(customEvent.properties, actualProperties)
    }

    @Test
    fun sendEvent_multipleEvents_onlyGetsGeoAdjustedDomainOnce() {
        // Arrange
        givenOkHttpClientReturnsGeoAdjustedDomainFromDtagEndpoint()
        givenOkHttpClientReturnsSuccessFromEventsEndpoint()
        val productViewEvent = buildProductViewEventWithAllFields()

        // Act
        attentiveApi!!.sendEvent(productViewEvent, ALL_USER_IDENTIFIERS, DOMAIN)
        attentiveApi!!.sendEvent(productViewEvent, ALL_USER_IDENTIFIERS, DOMAIN)

        // Assert
        val requestArgumentCaptor = ArgumentCaptor.forClass(
            Request::class.java
        )
        Mockito.verify(okHttpClient, Mockito.times(3))?.newCall(requestArgumentCaptor.capture())

        val eventsSentCount =
            requestArgumentCaptor.allValues.stream()
                .filter { request: Request -> request.url.toString().contains("t=d") }
                .count()
        Assert.assertEquals(2, eventsSentCount)

        val geoAdjustedDomainCallsSent =
            requestArgumentCaptor.allValues.stream()
                .filter { request: Request -> request.url.toString() == DTAG_URL }
                .count()
        Assert.assertEquals(1, geoAdjustedDomainCallsSent)
    }

    @Test
    fun sendEvent_geoAdjustedDomainRetrieved_domainValueIsCorrect() {
        // Arrange
        givenOkHttpClientReturnsGeoAdjustedDomainFromDtagEndpoint()
        givenOkHttpClientReturnsSuccessFromEventsEndpoint()
        val productViewEvent = buildProductViewEventWithAllFields()

        Assert.assertNull(attentiveApi!!.cachedGeoAdjustedDomain)

        // Act
        attentiveApi!!.sendEvent(productViewEvent, ALL_USER_IDENTIFIERS, DOMAIN)

        // Assert
        val requestArgumentCaptor = ArgumentCaptor.forClass(
            Request::class.java
        )
        Mockito.verify(okHttpClient, Mockito.times(2))?.newCall(requestArgumentCaptor.capture())

        Assert.assertEquals(GEO_ADJUSTED_DOMAIN, attentiveApi!!.cachedGeoAdjustedDomain)
    }

    private fun buildPurchaseEventWithRequiredFields(): PurchaseEvent {
        return PurchaseEvent.Builder(
            java.util.List.of(
                Item.Builder(
                    "11", "22", Price.Builder(
                        BigDecimal("15.99"), Currency.getInstance("USD")
                    ).build()
                ).build()
            ), Order.Builder("5555").build()
        ).build()
    }

    private fun buildPurchaseEventWithTwoItems(): PurchaseEvent {
        return PurchaseEvent.Builder(
            java.util.List.of(
                Item.Builder(
                    "11",
                    "22",
                    Price.Builder(BigDecimal("15.99"), Currency.getInstance("USD")).build()
                ).build(),
                Item.Builder(
                    "77",
                    "99",
                    Price.Builder(BigDecimal("20.00"), Currency.getInstance("USD")).build()
                ).build()
            ),
            Order.Builder("5555").build()
        ).build()
    }

    private fun buildPurchaseEventWithAllFields(): PurchaseEvent {
        return PurchaseEvent.Builder(
            java.util.List.of(buildItemWithAllFields()),
            Order.Builder("5555").build()
        ).cart(
            Cart.Builder().cartCoupon("cartCoupon").cartId("cartId").build()
        ).build()
    }

    private fun buildAddToCartEventWithAllFields(): AddToCartEvent {
        return AddToCartEvent.Builder().items(java.util.List.of(buildItemWithAllFields())).buildIt()
    }

    private fun buildProductViewEventWithAllFields(): ProductViewEvent {
        return ProductViewEvent.Builder().items(java.util.List.of(buildItemWithAllFields()))
            .buildIt()
    }

    private fun buildCustomEventWithAllFields(): CustomEvent {
        return CustomEvent.Builder(
            "High Fived Friend",
            java.util.Map.of("friendGivenTheHighFive", "Warthog234")
        ).build()
    }

    private fun buildItemWithAllFields(): Item {
        return Item.Builder(
            "11",
            "22",
            Price.Builder(BigDecimal("15.99"), Currency.getInstance("USD")).build()
        ).category("categoryValue").name("nameValue").productImage("imageUrl").build()
    }

    private fun givenOkHttpClientReturnsSuccessFromEventsEndpoint() {
        val call = Mockito.mock(Call::class.java)
        Mockito.doReturn(call).`when`(okHttpClient)
            ?.newCall(ArgumentMatchers.argThat { request: Request ->
                request.url.host == "events.attentivemobile.com"
            })
        Mockito.doAnswer(Answer<Void?> { invocation: InvocationOnMock ->
            val argument = invocation.getArgument(
                0,
                Callback::class.java
            )
            argument.onResponse(call, buildSuccessfulResponseMock())
            null
        }).`when`(call).enqueue(ArgumentMatchers.any())
    }

    private fun givenOkHttpClientReturnsGeoAdjustedDomainFromDtagEndpoint() {
        val call = Mockito.mock(Call::class.java)
        Mockito.doReturn(call).`when`(okHttpClient)
            ?.newCall(ArgumentMatchers.argThat { request: Request ->
                request.url.host == "cdn.attn.tv"
            })
        Mockito.doAnswer(Answer<Void?> { invocation: InvocationOnMock ->
            val argument = invocation.getArgument(
                0,
                Callback::class.java
            )
            val responseBody: ResponseBody =
                ResponseBody.create(null,
                    String.format(
                        "window.__attentive_domain='%s.attn.tv'",
                        GEO_ADJUSTED_DOMAIN
                    )
                )
            val dtagResponse = buildSuccessfulResponseMock()
            Mockito.doReturn(responseBody).`when`(dtagResponse).body
            argument.onResponse(call, dtagResponse)
            null
        }).`when`(call).enqueue(ArgumentMatchers.any())
    }

    private fun buildSuccessfulResponseMock(): Response {
        val mock = Mockito.mock(Response::class.java)
        Mockito.doReturn(true).`when`(mock).isSuccessful
        Mockito.doReturn(200).`when`(mock).code
        return mock
    }

    private fun givenAttentiveApiGetsGeoAdjustedDomainSuccessfully() {
        Mockito.doAnswer(Answer<Void?> { invocation: InvocationOnMock ->
            val argument =
                invocation.getArgument(1, GetGeoAdjustedDomainCallback::class.java)
            argument.onSuccess(GEO_ADJUSTED_DOMAIN)
            null
        }).`when`(attentiveApi).getGeoAdjustedDomainAsync(
            ArgumentMatchers.eq(
                DOMAIN
            ), ArgumentMatchers.any()
        )
    }

    private fun assertRequestMethodIsPost(request: Request) {
        Assert.assertEquals("POST", request.method.uppercase(Locale.getDefault()))
    }

    companion object {
        private const val DOMAIN = "adj"
        private const val GEO_ADJUSTED_DOMAIN = "domain-adj"
        private val DTAG_URL = String.format(AttentiveApi.ATTENTIVE_DTAG_URL, DOMAIN)
        private val ALL_USER_IDENTIFIERS = buildAllUserIdentifiers()

        private fun buildAllUserIdentifiers(): UserIdentifiers {
            return UserIdentifiers.Builder().withClientUserId("someClientUserId")
                .withPhone("+15556667777").withEmail("Youknow@email.com")
                .withShopifyId("someShopifyId").withKlaviyoId("someKlaviyoId")
                .withVisitorId("someVisitorId").build()
        }

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
    }
}