package com.attentive.androidsdk

import android.util.Log
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
import com.google.gson.JsonParser
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AttentiveApiTest {
    private lateinit var attentiveApi: AttentiveApi
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var json: Json

    @Before
    fun setup() {
        okHttpClient = Mockito.mock(OkHttpClient::class.java)
        attentiveApi = Mockito.spy(AttentiveApi(okHttpClient, "games"))
        json = Json { ignoreUnknownKeys = true }
    }

    @Test
    fun sendUserIdentifiersCollectedEvent_userIdentifierCollectedEvent_callsOkHttpClientWithCorrectPayload() {
        // Arrange

        givenOkHttpClientReturnsSuccessResponse()

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
            },
        )

        // Assert
        Mockito.verify(okHttpClient, Mockito.times(1)).newCall(capture(requestArgumentCaptor))
        val userIdentifiersRequest = requestArgumentCaptor.allValues.stream().findFirst()
        Assert.assertTrue(userIdentifiersRequest.isPresent)
        val url = userIdentifiersRequest.get().url

        val decoded = json.decodeFromString<Metadata>(url.queryParameter("m")!!)
        verifyCommonEventFields(
            url,
            "idn",
            decoded,
        )

        Assert.assertEquals(
            "[{\"vendor\":\"2\",\"id\":\"someClientUserId\"},{\"vendor\":\"0\",\"id\":\"someShopifyId\"},{\"vendor\":\"1\",\"id\":\"someKlaviyoId\"}]",
            url.queryParameter("evs"),
        )
    }

    @Test
    fun sendEvent_validEvent_httpMethodIsPost() {
        // Arrange

        givenOkHttpClientReturnsSuccessResponse()
        // Which event we send for this test doesn't matter - choosing AddToCart randomly
        val addToCartEvent = buildAddToCartEventWithAllFields()

        // Act
        attentiveApi.sendEvent(addToCartEvent, ALL_USER_IDENTIFIERS, DOMAIN)

        // Assert
        Mockito.verify(okHttpClient, Mockito.times(1)).newCall(capture(requestArgumentCaptor))
        val addToCartRequest =
            requestArgumentCaptor.allValues.stream()
                .filter { request: Request -> request.url.toString().contains("t=c") }.findFirst()
        Assert.assertTrue(addToCartRequest.isPresent)
        val request = addToCartRequest.get()
        Assert.assertEquals("POST", request.method.uppercase(Locale.getDefault()))
    }

    @Test
    @Throws(SerializationException::class)
    fun sendEvent_purchaseEventWithOnlyRequiredParams_callsOkHttpClientWithCorrectPayload() {
        // Arrange

        givenOkHttpClientReturnsSuccessResponse()
        val purchaseEvent = buildPurchaseEventWithRequiredFields()

        // Act
        attentiveApi.sendEvent(purchaseEvent, ALL_USER_IDENTIFIERS, DOMAIN)

        // Assert
        Mockito.verify(okHttpClient, Mockito.times(2)).newCall(capture(requestArgumentCaptor))
        val purchaseRequest =
            requestArgumentCaptor.allValues.stream()
                .filter { request: Request -> request.url.toString().contains("t=p") }.findFirst()
        Assert.assertTrue(purchaseRequest.isPresent)
        assertRequestMethodIsPost(purchaseRequest.get())
        val url = purchaseRequest.get().url

        val m =
            json.decodeFromString<PurchaseMetadataDto>(
                url.queryParameter("m")!!,
            )
        verifyCommonEventFields(url, "p", m)
        Assert.assertEquals("USD", m.currency)
        val purchasedItem = purchaseEvent.items[0]!!
        Assert.assertEquals(purchasedItem.price.price.toString(), m.price)
        Assert.assertEquals(purchasedItem.productId, m.productId)
        Assert.assertEquals(purchasedItem.productVariantId, m.subProductId)
        Assert.assertEquals(purchaseEvent.order.orderId, m.orderId)
    }

    private val requestArgumentCaptor: ArgumentCaptor<Request> =
        ArgumentCaptor.forClass(
            Request::class.java,
        )

    @Test
    @Throws(SerializationException::class)
    fun sendEvent_purchaseEventWithAllParams_callsOkHttpClientWithCorrectPayload() {
        // Arrange

        givenOkHttpClientReturnsSuccessResponse()
        val purchaseEvent = buildPurchaseEventWithAllFields()

        // Act
        attentiveApi.sendEvent(purchaseEvent, ALL_USER_IDENTIFIERS, DOMAIN)

        // Assert
        verify(okHttpClient, times(2)).newCall(capture(requestArgumentCaptor))
        val purchaseRequest =
            requestArgumentCaptor.allValues.stream()
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

        givenOkHttpClientReturnsSuccessResponse()
        val purchaseEvent = buildPurchaseEventWithAllFields()

        // Act
        attentiveApi.sendEvent(purchaseEvent, ALL_USER_IDENTIFIERS, DOMAIN)

        // Assert
        Mockito.verify(okHttpClient, Mockito.times(2))?.newCall(capture(requestArgumentCaptor))
        val orderConfirmedRequest =
            requestArgumentCaptor.allValues.stream()
                .filter { request: Request -> request.url.toString().contains("t=oc") }.findFirst()
        Assert.assertTrue(orderConfirmedRequest.isPresent)
        assertRequestMethodIsPost(orderConfirmedRequest.get())
        val url = orderConfirmedRequest.get().url

        verifyCommonEventFields(
            url,
            "oc",
            json.decodeFromString<Metadata>(
                url.queryParameter("m")!!,
            ),
        )

        val metadataString = url.queryParameter("m")!!
        val metadata = json.decodeFromString<Map<String, String>>(metadataString)
        Assert.assertEquals(purchaseEvent.order.orderId, metadata["orderId"])
        val expectedItem = purchaseEvent.items[0]!!
        Assert.assertEquals(expectedItem.price.price.toString(), metadata["cartTotal"])
        Assert.assertEquals(
            expectedItem.price.currency.currencyCode,
            metadata["currency"],
        )

        val products =
            json.decodeFromString<Array<ProductDto>>(
                metadata["products"]!!,
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

        givenOkHttpClientReturnsSuccessResponse()
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

        givenOkHttpClientReturnsSuccessResponse()
        val addToCartEvent = buildAddToCartEventWithAllFields()

        // Act
        attentiveApi.sendEvent(addToCartEvent, ALL_USER_IDENTIFIERS, DOMAIN)

        // Assert
        Mockito.verify(okHttpClient, Mockito.times(1))?.newCall(capture(requestArgumentCaptor))
        val addToCartRequest =
            requestArgumentCaptor.allValues.stream()
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

        givenOkHttpClientReturnsSuccessResponse()
        val productViewEvent = buildProductViewEventWithAllFields()

        // Act
        attentiveApi.sendEvent(productViewEvent, ALL_USER_IDENTIFIERS, DOMAIN)

        // Assert
        Mockito.verify(okHttpClient, Mockito.times(1))?.newCall(capture(requestArgumentCaptor))
        val addToCartRequest =
            requestArgumentCaptor.allValues.stream()
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

        givenOkHttpClientReturnsSuccessResponse()
        val customEvent = buildCustomEventWithAllFields()

        // Act
        attentiveApi.sendEvent(customEvent, ALL_USER_IDENTIFIERS, DOMAIN)

        // Assert
        Mockito.verify(okHttpClient, Mockito.times(1))?.newCall(capture(requestArgumentCaptor))
        val customEventRequest =
            requestArgumentCaptor.allValues.stream()
                .filter { request: Request -> request.url.toString().contains("t=ce") }.findFirst()
        Assert.assertTrue(customEventRequest.isPresent)
        assertRequestMethodIsPost(customEventRequest.get())
        val url = customEventRequest.get().url

        val metadataString = url.queryParameter("m")!!
        verifyCommonEventFields(
            url,
            "ce",
            json.decodeFromString(metadataString),
        )

        val metadata = json.decodeFromString<Map<String, String>>(metadataString)
        val actualProperties = json.decodeFromString<Map<String, String>>(metadata["properties"]!!)
        Assert.assertEquals(customEvent.type, metadata["type"])
        Assert.assertEquals(customEvent.properties, actualProperties)
    }

    private fun buildPurchaseEventWithRequiredFields(): PurchaseEvent {
        return PurchaseEvent.Builder(
            listOf(
                Item.Builder(
                    "11",
                    "22",
                    Price.Builder().price(BigDecimal("15.99")).currency(Currency.getInstance("USD")).build(),
                ).build(),
            ),
            Order.Builder().orderId("5555").build(),
        ).build()
    }

    private fun buildPurchaseEventWithTwoItems(): PurchaseEvent {
        return PurchaseEvent.Builder(
            listOf(
                Item.Builder(
                    "11",
                    "22",
                    Price.Builder().price(BigDecimal("15.99")).currency(Currency.getInstance("USD")).build(),
                ).build(),
                Item.Builder(
                    "77",
                    "99",
                    Price.Builder().price(BigDecimal("20.00")).currency(Currency.getInstance("USD")).build(),
                ).build(),
            ),
            Order.Builder().orderId("5555").build(),
        ).build()
    }

    private fun buildPurchaseEventWithAllFields(): PurchaseEvent {
        return PurchaseEvent.Builder(
            listOf(buildItemWithAllFields()),
            Order.Builder().orderId("5555").build(),
        ).cart(
            Cart.Builder().cartCoupon("cartCoupon").cartId("cartId").build(),
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
            mapOf("friendGivenTheHighFive" to "Warthog234"),
        ).build()
    }

    private fun buildItemWithAllFields(): Item {
        return Item.Builder(
            "11",
            "22",
            Price.Builder().price(BigDecimal("15.99")).currency(Currency.getInstance("USD")).build(),
        )
            .category("categoryValue").name("nameValue").productImage("imageUrl").build()
    }

    private fun givenOkHttpClientReturnsSuccessResponse() {
        whenever(okHttpClient.newCall(any())).doAnswer { invocation: InvocationOnMock ->
            val call = mock<Call>()
            whenever(call.enqueue(any())).doAnswer { enqueueInvocation: InvocationOnMock ->
                val callback = enqueueInvocation.getArgument<Callback>(0)
                callback.onResponse(call, buildSuccessfulResponseMock())
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

    // -- Retrofit endpoint tests (registerPushToken, sendDirectOpenStatus) --

    @Test
    fun registerPushToken_callsTokenEndpoint_withCorrectRequestFields() {
        // Arrange
        val capturedRequests = mutableListOf<CapturedApiRequest>()
        val requestLatch = CountDownLatch(1)
        val interceptorClient = buildInterceptorClient(capturedRequests, requestLatch)
        val testApi = AttentiveApi(interceptorClient, "games")

        // Act
        testApi.registerPushToken(
            token = "test-push-token",
            permissionGranted = true,
            userIdentifiers = ALL_USER_IDENTIFIERS,
            domain = DOMAIN,
        )
        requestLatch.await(5, TimeUnit.SECONDS)

        // Assert
        Assert.assertEquals(1, capturedRequests.size)
        val captured = capturedRequests[0]

        // Verify endpoint and method
        Assert.assertTrue(captured.request.url.encodedPath.endsWith("/token"))
        Assert.assertEquals("POST", captured.request.method)

        // Verify headers
        Assert.assertEquals("1", captured.request.header("x-datadog-sampling-priority"))

        // Verify JSON body fields match @SerializedName annotations
        val body = JsonParser.parseString(captured.bodyJson).asJsonObject
        Assert.assertEquals(DOMAIN, body.get("c").asString)
        Assert.assertTrue(body.get("v").asString.startsWith("mobile-app-"))
        Assert.assertEquals(ALL_USER_IDENTIFIERS.visitorId, body.get("u").asString)
        Assert.assertEquals("test-push-token", body.get("pt").asString)
        Assert.assertEquals("true", body.get("st").asString)
        Assert.assertEquals("fcm", body.get("tp").asString)

        // Verify metadata (m) contains phone and email
        val metadata = body.getAsJsonObject("m")
        Assert.assertEquals(ALL_USER_IDENTIFIERS.phone, metadata.get("phone").asString)
        Assert.assertEquals(ALL_USER_IDENTIFIERS.email, metadata.get("email").asString)

        // Verify external vendor IDs (evs) are serialized
        val evs = body.getAsJsonArray("evs")
        Assert.assertEquals(3, evs.size())
    }

    @Test
    fun sendDirectOpenStatus_callsMtctrlEndpoint_withCorrectRequestFields() {
        // Arrange
        val capturedRequests = mutableListOf<CapturedApiRequest>()
        val requestLatch = CountDownLatch(1)
        val interceptorClient = buildInterceptorClient(capturedRequests, requestLatch)
        val testApi = AttentiveApi(interceptorClient, "games")

        val callbackMap = mapOf(
            "attentive_open_action_url" to "https://example.com/deep-link",
            "someKey" to "someValue",
        )

        // Act - use DIRECT_OPEN launch type ("o")
        testApi.sendDirectOpenStatus(
            AttentiveApi.LaunchType.DIRECT_OPEN,
            "test-push-token",
            callbackMap,
            true,
            ALL_USER_IDENTIFIERS,
            DOMAIN,
        )
        requestLatch.await(5, TimeUnit.SECONDS)

        // Assert
        Assert.assertEquals(1, capturedRequests.size)
        val captured = capturedRequests[0]

        // Verify endpoint and method
        Assert.assertTrue(captured.request.url.encodedPath.endsWith("/mtctrl"))
        Assert.assertEquals("POST", captured.request.method)

        // Verify headers
        Assert.assertEquals("1", captured.request.header("x-datadog-sampling-priority"))

        // Verify JSON body structure
        val body = JsonParser.parseString(captured.bodyJson).asJsonObject

        // Verify events array
        val events = body.getAsJsonArray("events")
        Assert.assertTrue(events.size() >= 1)
        val firstEvent = events[0].asJsonObject
        Assert.assertEquals("o", firstEvent.get("ist").asString)  // DIRECT_OPEN value
        val eventData = firstEvent.getAsJsonObject("data")
        Assert.assertEquals("https://example.com/deep-link", eventData.get("attentive_open_action_url").asString)

        // Verify device info
        val device = body.getAsJsonObject("device")
        Assert.assertEquals(DOMAIN, device.get("c").asString)
        Assert.assertTrue(device.get("v").asString.startsWith("mobile-app-"))
        Assert.assertEquals(ALL_USER_IDENTIFIERS.visitorId, device.get("u").asString)
        Assert.assertEquals("test-push-token", device.get("pt").asString)
        Assert.assertEquals("true", device.get("st").asString)
        Assert.assertEquals("fcm", device.get("tp").asString)
        Assert.assertEquals("https://example.com/deep-link", device.get("pd").asString)

        // Verify metadata in device
        val metadata = device.getAsJsonObject("m")
        Assert.assertEquals(ALL_USER_IDENTIFIERS.phone, metadata.get("phone").asString)
        Assert.assertEquals(ALL_USER_IDENTIFIERS.email, metadata.get("email").asString)

        // Verify external vendor IDs in device
        val evs = device.getAsJsonArray("evs")
        Assert.assertEquals(3, evs.size())
    }

    @Test
    fun sendDirectOpenStatus_directOpen_alsoIncludesAppLaunchedEvent() {
        // Arrange
        val capturedRequests = mutableListOf<CapturedApiRequest>()
        val requestLatch = CountDownLatch(1)
        val interceptorClient = buildInterceptorClient(capturedRequests, requestLatch)
        val testApi = AttentiveApi(interceptorClient, "games")

        // Act
        testApi.sendDirectOpenStatus(
            AttentiveApi.LaunchType.DIRECT_OPEN,
            "test-push-token",
            mapOf("key" to "value"),
            true,
            ALL_USER_IDENTIFIERS,
            DOMAIN,
        )
        requestLatch.await(5, TimeUnit.SECONDS)

        // Assert - DIRECT_OPEN should include both the direct open event and an APP_LAUNCHED event
        val body = JsonParser.parseString(capturedRequests[0].bodyJson).asJsonObject
        val events = body.getAsJsonArray("events")
        Assert.assertEquals(2, events.size())
        Assert.assertEquals("o", events[0].asJsonObject.get("ist").asString)   // DIRECT_OPEN
        Assert.assertEquals("al", events[1].asJsonObject.get("ist").asString)  // APP_LAUNCHED
    }

    private data class CapturedApiRequest(
        val request: Request,
        val bodyJson: String,
    )

    private fun buildInterceptorClient(
        capturedRequests: MutableList<CapturedApiRequest>,
        latch: CountDownLatch,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                if (request.url.host == "mobile.attentivemobile.com") {
                    val buffer = okio.Buffer()
                    request.body?.writeTo(buffer)
                    capturedRequests.add(
                        CapturedApiRequest(
                            request = request,
                            bodyJson = buffer.readUtf8(),
                        ),
                    )
                    latch.countDown()
                }
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(204)
                    .message("No Content")
                    .body("".toResponseBody())
                    .build()
            }
            .build()
    }

    private fun assertRequestMethodIsPost(request: Request) {
        Assert.assertEquals("POST", request.method.uppercase(Locale.getDefault()))
    }

    companion object {
        private const val DOMAIN = "adj"
        private val ALL_USER_IDENTIFIERS = buildAllUserIdentifiers()

        private fun buildAllUserIdentifiers(): UserIdentifiers {
            return UserIdentifiers.Builder().withClientUserId("someClientUserId")
                .withPhone("+15556667777").withEmail("Youknow@email.com")
                .withShopifyId("someShopifyId").withKlaviyoId("someKlaviyoId")
                .withVisitorId("someVisitorId").build()
        }

        private fun verifyCommonEventFields(
            url: HttpUrl,
            eventType: String?,
            m: Metadata,
        ) {
            Assert.assertEquals("modern", url.queryParameter("tag"))
            if (eventType != null) {
                Assert.assertEquals("mobile-app", url.queryParameter("v"))
            } else {
                Assert.assertNull("mobile-app-${AppInfo.attentiveSDKVersion}", url.queryParameter("v"))
            }
            Assert.assertEquals("0", url.queryParameter("lt"))
            Assert.assertEquals(DOMAIN, url.queryParameter("c"))
            Assert.assertEquals(eventType, url.queryParameter("t"))
            Assert.assertEquals(ALL_USER_IDENTIFIERS.visitorId, url.queryParameter("u"))

            Assert.assertEquals(ALL_USER_IDENTIFIERS.phone, m.phone)
            Assert.assertEquals(ALL_USER_IDENTIFIERS.email, m.email)
            Assert.assertEquals("msdk", m.source)
        }
    }
}
