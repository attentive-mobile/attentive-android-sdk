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
import com.attentive.androidsdk.internal.util.AppInfo
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale

class AttentiveApiTest {
    private lateinit var attentiveApi: AttentiveApi
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var json: Json

    @Before
    fun setup() {
        okHttpClient = Mockito.mock(OkHttpClient::class.java)
        attentiveApi = Mockito.spy(AttentiveApi(okHttpClient))
        json = Json{ignoreUnknownKeys = true}
    }

    @Test
    fun sendUserIdentifiersCollectedEvent_userIdentifierCollectedEvent_callsOkHttpClientWithCorrectPayload() {
        // Arrange
        givenAttentiveApiGetsGeoAdjustedDomainSuccessfully()
        givenOkHttpClientReturnsResponseBasedOnHost()

        // Act
        attentiveApi.sendUserIdentifiersCollectedEvent(
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
        Mockito.verify(okHttpClient, Mockito.times(1)).newCall(capture(requestArgumentCaptor))
        val userIdentifiersRequest = requestArgumentCaptor.allValues.stream().findFirst()
        Assert.assertTrue(userIdentifiersRequest.isPresent)
        val url = userIdentifiersRequest.get().url

        val decoded = json.decodeFromString<Metadata>(url.queryParameter("m")!!)
        verifyCommonEventFields(
            url, "idn", decoded
        )

        Assert.assertEquals(
            "[{\"vendor\":\"2\",\"id\":\"someClientUserId\"},{\"vendor\":\"0\",\"id\":\"someShopifyId\"},{\"vendor\":\"1\",\"id\":\"someKlaviyoId\"}]",
            url.queryParameter("evs")
        )
    }

    @Test
    fun sendEvent_validEvent_httpMethodIsPost() {
        // Arrange
        givenAttentiveApiGetsGeoAdjustedDomainSuccessfully()
        givenOkHttpClientReturnsResponseBasedOnHost()
        // Which event we send for this test doesn't matter - choosing AddToCart randomly
        val addToCartEvent = buildAddToCartEventWithAllFields()

        // Act
        attentiveApi.sendEvent(addToCartEvent, ALL_USER_IDENTIFIERS, DOMAIN)

        // Assert
        Mockito.verify(okHttpClient, Mockito.times(1)).newCall(capture(requestArgumentCaptor))
        val addToCartRequest = requestArgumentCaptor.allValues.stream()
            .filter { request: Request -> request.url.toString().contains("t=c") }.findFirst()
        Assert.assertTrue(addToCartRequest.isPresent)
        val request = addToCartRequest.get()
        Assert.assertEquals("POST", request.method.uppercase(Locale.getDefault()))
    }

    @Test
    @Throws(SerializationException::class)
    fun sendEvent_purchaseEventWithOnlyRequiredParams_callsOkHttpClientWithCorrectPayload() {
        // Arrange
        givenAttentiveApiGetsGeoAdjustedDomainSuccessfully()
        givenOkHttpClientReturnsResponseBasedOnHost()
        val purchaseEvent = buildPurchaseEventWithRequiredFields()

        // Act
        attentiveApi.sendEvent(purchaseEvent, ALL_USER_IDENTIFIERS, DOMAIN)

        // Assert
        Mockito.verify(okHttpClient, Mockito.times(2)).newCall(capture(requestArgumentCaptor))
        val purchaseRequest = requestArgumentCaptor.allValues.stream()
            .filter { request: Request -> request.url.toString().contains("t=p") }.findFirst()
        Assert.assertTrue(purchaseRequest.isPresent)
        assertRequestMethodIsPost(purchaseRequest.get())
        val url = purchaseRequest.get().url

        val m = json.decodeFromString<PurchaseMetadataDto>(
            url.queryParameter("m")!!
        )
        verifyCommonEventFields(url, "p", m)
        Assert.assertEquals("USD", m.currency)
        val purchasedItem = purchaseEvent.items[0]!!
        Assert.assertEquals(purchasedItem.price.price.toString(), m.price)
        Assert.assertEquals(purchasedItem.productId, m.productId)
        Assert.assertEquals(purchasedItem.productVariantId, m.subProductId)
        Assert.assertEquals(purchaseEvent.order.orderId, m.orderId)
    }

    private val requestArgumentCaptor: ArgumentCaptor<Request> = ArgumentCaptor.forClass(
        Request::class.java
    )

    @Test
    @Throws(SerializationException::class)
    fun sendEvent_purchaseEventWithAllParams_callsOkHttpClientWithCorrectPayload() {
        // Arrange
        givenAttentiveApiGetsGeoAdjustedDomainSuccessfully()
        givenOkHttpClientReturnsResponseBasedOnHost()
        val purchaseEvent = buildPurchaseEventWithAllFields()


        // Act
        attentiveApi.sendEvent(purchaseEvent, ALL_USER_IDENTIFIERS, DOMAIN)

        // Assert
        verify(okHttpClient, times(2)).newCall(capture(requestArgumentCaptor))
        val purchaseRequest = requestArgumentCaptor.allValues.stream()
            .filter { request: Request -> request.url.toString().contains("t=p") }.findFirst()
        Assert.assertTrue(purchaseRequest.isPresent)
        assertRequestMethodIsPost(purchaseRequest.get())
        val url = purchaseRequest.get().url
        val m = json.decodeFromString<PurchaseMetadataDto>(url.queryParameter("m")!!)
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
    @Throws(SerializationException::class)
    fun sendEvent_purchaseEventWithAllParams_sendsCorrectOrderConfirmedEvent() {
        // Arrange
        givenAttentiveApiGetsGeoAdjustedDomainSuccessfully()
        givenOkHttpClientReturnsResponseBasedOnHost()
        val purchaseEvent = buildPurchaseEventWithAllFields()

        // Act
        attentiveApi.sendEvent(purchaseEvent, ALL_USER_IDENTIFIERS, DOMAIN)

        // Assert
        Mockito.verify(okHttpClient, Mockito.times(2))?.newCall(capture(requestArgumentCaptor))
        val orderConfirmedRequest = requestArgumentCaptor.allValues.stream()
            .filter { request: Request -> request.url.toString().contains("t=oc") }.findFirst()
        Assert.assertTrue(orderConfirmedRequest.isPresent)
        assertRequestMethodIsPost(orderConfirmedRequest.get())
        val url = orderConfirmedRequest.get().url

        verifyCommonEventFields(
            url, "oc", json.decodeFromString<Metadata>(
                url.queryParameter("m")!!)
        )

        val metadataString = url.queryParameter("m")!!
        val metadata = json.decodeFromString<Map<String, String>>(metadataString)
        Assert.assertEquals(purchaseEvent.order.orderId, metadata["orderId"])
        val expectedItem = purchaseEvent.items[0]!!
        Assert.assertEquals(expectedItem.price.price.toString(), metadata["cartTotal"])
        Assert.assertEquals(
            expectedItem.price.currency.currencyCode,
            metadata["currency"]
        )

        val products =
            json.decodeFromString<Array<ProductDto>>(
                metadata["products"]!!
            )
        Assert.assertEquals(1, products.size.toLong())
        Assert.assertEquals(expectedItem.price.price.toString(), products[0].price)
        Assert.assertEquals(expectedItem.productId, products[0].productId)
        Assert.assertEquals(expectedItem.productVariantId, products[0].subProductId)
        Assert.assertEquals(expectedItem.category, products[0].category)
        Assert.assertEquals(expectedItem.productImage, products[0].image)
    }

    @Test
    fun sendEvent_purchaseEventWithTwoProducts_callsEventsEndpointTwiceForPurchasesAndOnceForOrderConfirmed() {
        // Arrange
        givenAttentiveApiGetsGeoAdjustedDomainSuccessfully()
        givenOkHttpClientReturnsResponseBasedOnHost()
        val purchaseEvent = buildPurchaseEventWithTwoItems()

        // Act
        attentiveApi.sendEvent(purchaseEvent, ALL_USER_IDENTIFIERS, DOMAIN)

        // Assert
        Mockito.verify(okHttpClient, Mockito.times(3))?.newCall(capture(requestArgumentCaptor))
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
    @Throws(SerializationException::class)
    fun sendEvent_addToCartEventWithAllParams_callsOkHttpClientWithCorrectPayload() {
        // Arrange
        givenAttentiveApiGetsGeoAdjustedDomainSuccessfully()
        givenOkHttpClientReturnsResponseBasedOnHost()
        val addToCartEvent = buildAddToCartEventWithAllFields()

        // Act
        attentiveApi.sendEvent(addToCartEvent, ALL_USER_IDENTIFIERS, DOMAIN)

        // Assert
        Mockito.verify(okHttpClient, Mockito.times(1))?.newCall(capture(requestArgumentCaptor))
        val addToCartRequest = requestArgumentCaptor.allValues.stream()
            .filter { request: Request -> request.url.toString().contains("t=c") }.findFirst()
        Assert.assertTrue(addToCartRequest.isPresent)
        assertRequestMethodIsPost(addToCartRequest.get())
        val url = addToCartRequest.get().url

        val m = json.decodeFromString<AddToCartMetadataDto>(url.queryParameter("m")!!)
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
    @Throws(SerializationException::class)
    fun sendEvent_productViewEventWithAllParams_callsOkHttpClientWithCorrectPayload() {
        // Arrange
        givenAttentiveApiGetsGeoAdjustedDomainSuccessfully()
        givenOkHttpClientReturnsResponseBasedOnHost()
        val productViewEvent = buildProductViewEventWithAllFields()

        // Act
        attentiveApi.sendEvent(productViewEvent, ALL_USER_IDENTIFIERS, DOMAIN)

        // Assert
        Mockito.verify(okHttpClient, Mockito.times(1))?.newCall(capture(requestArgumentCaptor))
        val addToCartRequest = requestArgumentCaptor.allValues.stream()
            .filter { request: Request -> request.url.toString().contains("t=d") }.findFirst()
        Assert.assertTrue(addToCartRequest.isPresent)
        assertRequestMethodIsPost(addToCartRequest.get())
        val url = addToCartRequest.get().url

        val m = json.decodeFromString<ProductViewMetadataDto>(url.queryParameter("m")!!)
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
    @Throws(SerializationException::class)
    fun sendEvent_customEventWithAllParams_callsOkHttpClientWithCorrectPayload() {
        // Arrange
        givenAttentiveApiGetsGeoAdjustedDomainSuccessfully()
        givenOkHttpClientReturnsResponseBasedOnHost()
        val customEvent = buildCustomEventWithAllFields()

        // Act
        attentiveApi.sendEvent(customEvent, ALL_USER_IDENTIFIERS, DOMAIN)

        // Assert
        Mockito.verify(okHttpClient, Mockito.times(1))?.newCall(capture(requestArgumentCaptor))
        val customEventRequest = requestArgumentCaptor.allValues.stream()
            .filter { request: Request -> request.url.toString().contains("t=ce") }.findFirst()
        Assert.assertTrue(customEventRequest.isPresent)
        assertRequestMethodIsPost(customEventRequest.get())
        val url = customEventRequest.get().url

        val metadataString = url.queryParameter("m")!!
        verifyCommonEventFields(
            url, "ce", json.decodeFromString(metadataString)
        )

        val metadata = json.decodeFromString<Map<String, String>>(metadataString)
        val actualProperties = json.decodeFromString<Map<String, String>>(metadata["properties"]!!)
        Assert.assertEquals(customEvent.type, metadata["type"])
        Assert.assertEquals(customEvent.properties, actualProperties)
    }

    @Test
    fun sendEvent_multipleEvents_onlyGetsGeoAdjustedDomainOnce() {
        // Arrange
        givenOkHttpClientReturnsResponseBasedOnHost()
        val productViewEvent = buildProductViewEventWithAllFields()

        // Act
        attentiveApi.sendEvent(productViewEvent, ALL_USER_IDENTIFIERS, DOMAIN)
        attentiveApi.sendEvent(productViewEvent, ALL_USER_IDENTIFIERS, DOMAIN)

        // Assert
        Mockito.verify(okHttpClient, Mockito.times(3))?.newCall(capture(requestArgumentCaptor))

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
        givenOkHttpClientReturnsResponseBasedOnHost()
        val productViewEvent = buildProductViewEventWithAllFields()

        Assert.assertNull(attentiveApi.cachedGeoAdjustedDomain)

        // Act
        attentiveApi.sendEvent(productViewEvent, ALL_USER_IDENTIFIERS, DOMAIN)

        // Assert
        Mockito.verify(okHttpClient, Mockito.times(2))?.newCall(capture(requestArgumentCaptor))

        Assert.assertEquals(GEO_ADJUSTED_DOMAIN, attentiveApi.cachedGeoAdjustedDomain)
    }

    private fun buildPurchaseEventWithRequiredFields(): PurchaseEvent {
        return PurchaseEvent.Builder(
            listOf(
                Item.Builder(
                    "11", "22", Price.Builder().price(BigDecimal("15.99")).currency(Currency.getInstance("USD")).build()
                ).build()
            ), Order.Builder().orderId("5555").build()
        ).build()
    }

    private fun buildPurchaseEventWithTwoItems(): PurchaseEvent {
        return PurchaseEvent.Builder(
            listOf(
                Item.Builder(
                    "11",
                    "22",
                    Price.Builder().price(BigDecimal("15.99")).currency(Currency.getInstance("USD")).build()
                ).build(),
                Item.Builder(
                    "77",
                    "99",
                    Price.Builder().price(BigDecimal("20.00")).currency(Currency.getInstance("USD")).build()
                ).build()
            ),
            Order.Builder().orderId("5555").build()
        ).build()
    }

    private fun buildPurchaseEventWithAllFields(): PurchaseEvent {
        return PurchaseEvent.Builder(
            listOf(buildItemWithAllFields()),
            Order.Builder().orderId("5555").build()
        ).cart(
            Cart.Builder().cartCoupon("cartCoupon").cartId("cartId").build()
        ).build()
    }

    private fun buildAddToCartEventWithAllFields(): AddToCartEvent {
        return AddToCartEvent.Builder().items(listOf(buildItemWithAllFields())).build()
    }

    private fun buildProductViewEventWithAllFields(): ProductViewEvent {
        return ProductViewEvent.Builder().items(listOf(buildItemWithAllFields()))
            .build()
    }

    private fun buildCustomEventWithAllFields(): CustomEvent {
        return CustomEvent.Builder(
            "High Fived Friend",
            mapOf("friendGivenTheHighFive" to "Warthog234")
        ).build()
    }

    private fun buildItemWithAllFields(): Item {
        return Item.Builder(
            "11",
            "22",
            Price.Builder().price(BigDecimal("15.99")).currency(Currency.getInstance("USD")).build())
        .category("categoryValue").name("nameValue").productImage("imageUrl").build()
    }

    private fun givenOkHttpClientReturnsResponseBasedOnHost() {
        whenever(okHttpClient.newCall(any())).doAnswer { invocation: InvocationOnMock ->
            val request = invocation.arguments[0] as Request
            val host = request.url.host
            val call = mock<Call>()

            when (host) {
                "events.attentivemobile.com" -> {
                    whenever(call.enqueue(any())).doAnswer { enqueueInvocation: InvocationOnMock ->
                        val callback = enqueueInvocation.getArgument<Callback>(0)
                        callback.onResponse(call, buildSuccessfulResponseMock())
                    }
                }
                "cdn.attn.tv" -> {
                    val dtagResponse = mock<Response>()
                    val content = String.format(
                        "window.__attentive_domain='%s.attn.tv'",
                        GEO_ADJUSTED_DOMAIN
                    )
                    val responseBody = content.toResponseBody("text/html".toMediaTypeOrNull())

                    whenever(dtagResponse.body).thenReturn(responseBody)
                    whenever(dtagResponse.request).thenReturn(request)
                    whenever(dtagResponse.protocol).thenReturn(Protocol.HTTP_1_1)
                    whenever(dtagResponse.code).thenReturn(200)
                    whenever(dtagResponse.message).thenReturn("OK")
                    whenever(dtagResponse.isSuccessful).thenReturn(true)

                    whenever(call.enqueue(any())).doAnswer { enqueueInvocation: InvocationOnMock ->
                        val callback = enqueueInvocation.getArgument<Callback>(0)
                        callback.onResponse(call, dtagResponse)
                    }
                }
                else -> {
                    throw IllegalArgumentException("Unhandled host: $host")
                }
            }
            call
        }
    }

    private fun buildSuccessfulResponseMock(): Response {
        val mock = Mockito.mock(Response::class.java)
        Mockito.doReturn(true).`when`(mock).isSuccessful
        Mockito.doReturn(200).`when`(mock).code
        return mock
    }

    private fun givenAttentiveApiGetsGeoAdjustedDomainSuccessfully() {
        Mockito.doAnswer { invocation: InvocationOnMock ->
            val argument =
                invocation.getArgument(1, GetGeoAdjustedDomainCallback::class.java)
            argument.onSuccess(GEO_ADJUSTED_DOMAIN)
        }.whenever(attentiveApi).getGeoAdjustedDomainAsync(
            eq(DOMAIN), any()
        )
    }

//    private fun givenAttentiveApiGetsGeoAdjustedDomainSuccessfullyAlternative() {
//        whenever(
//            attentiveApi.getGeoAdjustedDomainAsync(
//                eq(DOMAIN),
//                argThat { true })
//        ) doAnswer { invocation: InvocationOnMock ->
//            val (_, callback: GetGeoAdjustedDomainCallback) = invocation.arguments
//            callback.onSuccess(GEO_ADJUSTED_DOMAIN)
//        }
//    }

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
            Assert.assertEquals(AppInfo.attentiveSDKVersion, url.queryParameter("v"))
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