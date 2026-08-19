package com.attentive.androidsdk

import com.attentive.androidsdk.events.AddToCartEvent
import com.attentive.androidsdk.events.Cart
import com.attentive.androidsdk.events.CustomEvent
import com.attentive.androidsdk.events.Event
import com.attentive.androidsdk.events.Item
import com.attentive.androidsdk.events.Order
import com.attentive.androidsdk.events.Price
import com.attentive.androidsdk.events.ProductViewEvent
import com.attentive.androidsdk.events.PurchaseEvent
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito
import java.math.BigDecimal
import java.net.URLDecoder
import java.util.Collections
import java.util.Currency
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Byte-level parity between the two event paths: legacy `/e` on `events.attentivemobile.com`
 * (query parameters) and `/mobile` on `mobile.attentivemobile.com` (an
 * `application/x-www-form-urlencoded` body carrying the event JSON as `d`).
 *
 * These back [MSDK-473](https://attentivemobile.atlassian.net/browse/MSDK-473): flipping
 * `AttentiveConfig.apiVersion` to `NEW` by default has to be invisible to customers, which means
 * every field a customer's reports depend on must arrive with the same value on both paths.
 *
 * The tests drive the real `AttentiveApi` against a real `OkHttpClient` whose interceptor records
 * the request and short-circuits it, so what is asserted is what would go on the wire — the
 * encoding included.
 */
class AttentiveApiPayloadParityTest {
    private lateinit var base64Mock: MockedStatic<android.util.Base64>

    @Before
    fun setUp() {
        // `unitTests.returnDefaultValues` makes android.util.Base64 return null, which would make
        // every identifier assertion vacuous. Stand in the JDK encoder, which produces the same
        // bytes as Base64.NO_WRAP.
        base64Mock = Mockito.mockStatic(android.util.Base64::class.java)
        base64Mock.`when`<String> {
            android.util.Base64.encodeToString(Mockito.any(), Mockito.anyInt())
        }.thenAnswer { invocation ->
            java.util.Base64.getEncoder().encodeToString(invocation.getArgument<ByteArray>(0))
        }
    }

    @After
    fun tearDown() {
        base64Mock.close()
    }

    // -- Purchase --

    @Test
    fun purchase_sendsTheSameOrderAndProductFieldsOnBothPaths() {
        val event = purchaseEvent(items = listOf(itemWithAllFields(), secondItem()))

        val legacy = legacyMetadata(sendLegacy(event, expectedRequests = 3), "p")
        val orderConfirmed = legacyMetadata(sendLegacy(event, expectedRequests = 3), "oc")
        val modern = modernPayload(sendModern(event))
        val metadata = modern.getAsJsonObject("eventMetadata")

        assertEquals(legacy.string("orderId"), metadata.string("orderId"))
        assertEquals(legacy.string("currency"), metadata.string("currency"))
        // The legacy path splits a purchase into one request per item plus an OrderConfirmed; the
        // /mobile path sends one request and the backend's PurchaseProcessor performs the same
        // split, so the item-level fields are compared against the products array.
        assertEquals(orderConfirmed.string("cartTotal"), metadata.string("orderTotal"))

        val products = metadata.getAsJsonArray("products")
        assertEquals(2, products.size())
        val firstProduct = products[0].asJsonObject
        assertEquals(legacy.string("productId"), firstProduct.string("productId"))
        assertEquals(legacy.string("subProductId"), firstProduct.string("variantId"))
        assertEquals(legacy.string("name"), firstProduct.string("name"))
        assertEquals(legacy.string("image"), firstProduct.string("imageUrl"))
        assertEquals(legacy.string("price"), firstProduct.string("price"))
        assertEquals(legacy.string("quantity"), firstProduct.get("quantity").asString)
        assertEquals(
            legacy.string("category"),
            firstProduct.getAsJsonArray("categories").single().asString,
        )
    }

    @Test
    fun purchase_sendsTheSameCartFieldsOnBothPaths() {
        val event =
            purchaseEvent(
                items = listOf(itemWithAllFields()),
                cart =
                    Cart.Builder()
                        .cartId("cart-1")
                        .cartCoupon("SAVE10")
                        .cartDiscount("1.00")
                        .cartTotal("99.99")
                        .build(),
            )

        val legacy = legacyMetadata(sendLegacy(event, expectedRequests = 2), "p")
        val cart =
            modernPayload(sendModern(event))
                .getAsJsonObject("eventMetadata")
                .getAsJsonObject("cart")

        assertEquals(legacy.string("cartId"), cart.string("cartId"))
        assertEquals(legacy.string("cartCoupon"), cart.string("cartCoupon"))
        assertEquals(legacy.string("cartDiscount"), cart.string("cartDiscount"))
        assertEquals(legacy.string("cartTotal"), cart.string("cartTotal"))
    }

    /**
     * A host-supplied cart total is authoritative on the legacy path — it replaces the summed item
     * prices on both the Purchase and the OrderConfirmed request. The /mobile path used to report
     * the computed sum as `orderTotal` regardless, which understated any order whose total includes
     * shipping or tax.
     */
    @Test
    fun purchase_hostSuppliedCartTotalWinsOverTheComputedSumOnBothPaths() {
        val event =
            purchaseEvent(
                items = listOf(itemWithAllFields()), // 15.99
                cart = Cart.Builder().cartId("cart-1").cartTotal("99.99").build(),
            )

        val legacy = legacyMetadata(sendLegacy(event, expectedRequests = 2), "oc")
        val metadata = modernPayload(sendModern(event)).getAsJsonObject("eventMetadata")

        assertEquals("99.99", legacy.string("cartTotal"))
        assertEquals("99.99", metadata.string("orderTotal"))
        assertEquals("99.99", metadata.getAsJsonObject("cart").string("cartTotal"))
    }

    @Test
    fun purchase_withoutACartTotal_fallsBackToTheSummedItemPricesOnBothPaths() {
        val event = purchaseEvent(items = listOf(itemWithAllFields(), secondItem())) // 15.99 + 20.00

        val legacy = legacyMetadata(sendLegacy(event, expectedRequests = 3), "oc")
        val metadata = modernPayload(sendModern(event)).getAsJsonObject("eventMetadata")

        assertEquals("35.99", legacy.string("cartTotal"))
        assertEquals("35.99", metadata.string("orderTotal"))
    }

    /**
     * `cartId` is optional on iOS and carries no `required` marker in the backend's `Cart` schema,
     * so a cart with only a coupon has to survive both paths rather than being rejected at
     * construction time.
     */
    @Test
    fun purchase_withACartThatHasNoId_isSentOnBothPaths() {
        val event =
            purchaseEvent(
                items = listOf(itemWithAllFields()),
                cart = Cart.Builder().cartCoupon("SAVE10").build(),
            )

        val legacy = legacyMetadata(sendLegacy(event, expectedRequests = 2), "p")
        val cart =
            modernPayload(sendModern(event))
                .getAsJsonObject("eventMetadata")
                .getAsJsonObject("cart")

        assertNull(legacy.string("cartId"))
        assertNull(cart.string("cartId"))
        assertEquals("SAVE10", cart.string("cartCoupon"))
    }

    // -- ProductView / AddToCart --

    @Test
    fun productView_sendsTheSameProductFieldsOnBothPaths() {
        val event = ProductViewEvent.Builder().items(listOf(itemWithAllFields())).build()

        val legacy = legacyMetadata(sendLegacy(event, expectedRequests = 1), "d")
        val metadata = modernPayload(sendModern(event)).getAsJsonObject("eventMetadata")
        val product = metadata.getAsJsonObject("product")

        assertEquals(legacy.string("currency"), metadata.string("currency"))
        assertEquals(legacy.string("productId"), product.string("productId"))
        assertEquals(legacy.string("subProductId"), product.string("variantId"))
        assertEquals(legacy.string("name"), product.string("name"))
        assertEquals(legacy.string("image"), product.string("imageUrl"))
        assertEquals(legacy.string("price"), product.string("price"))
        assertEquals(legacy.string("category"), product.getAsJsonArray("categories").single().asString)
    }

    @Test
    fun addToCart_sendsTheSameProductFieldsOnBothPaths() {
        val event = AddToCartEvent.Builder().items(listOf(itemWithAllFields())).build()

        val legacy = legacyMetadata(sendLegacy(event, expectedRequests = 1), "c")
        val metadata = modernPayload(sendModern(event)).getAsJsonObject("eventMetadata")
        val product = metadata.getAsJsonObject("product")

        assertEquals(legacy.string("currency"), metadata.string("currency"))
        assertEquals(legacy.string("productId"), product.string("productId"))
        assertEquals(legacy.string("subProductId"), product.string("variantId"))
        assertEquals(legacy.string("name"), product.string("name"))
        assertEquals(legacy.string("image"), product.string("imageUrl"))
        assertEquals(legacy.string("price"), product.string("price"))
    }

    /**
     * The legacy path carries the deeplink as the `pd` query parameter, which the backend
     * deserialises straight into the event's `locationHref`. The /mobile path therefore has to put
     * it in `locationHref` and, like the legacy path, send no referrer of its own.
     */
    @Test
    fun productView_deeplinkArrivesAsPdOnLegacyAndLocationHrefOnModern() {
        val event =
            ProductViewEvent.Builder()
                .items(listOf(itemWithAllFields()))
                .deeplink(DEEPLINK)
                .build()

        val legacyUrl = legacyUrl(sendLegacy(event, expectedRequests = 1), "d")
        val modern = modernPayload(sendModern(event))

        assertEquals(DEEPLINK, legacyUrl.queryParameter("pd"))
        assertNull(legacyUrl.queryParameter("r"))
        assertEquals(DEEPLINK, modern.string("locationHref"))
        assertEquals("", modern.string("referrer"))
    }

    @Test
    fun addToCart_deeplinkArrivesAsPdOnLegacyAndLocationHrefOnModern() {
        val event =
            AddToCartEvent.Builder()
                .items(listOf(itemWithAllFields()))
                .deeplink(DEEPLINK)
                .build()

        val legacyUrl = legacyUrl(sendLegacy(event, expectedRequests = 1), "c")
        val modern = modernPayload(sendModern(event))

        assertEquals(DEEPLINK, legacyUrl.queryParameter("pd"))
        assertNull(legacyUrl.queryParameter("r"))
        assertEquals(DEEPLINK, modern.string("locationHref"))
        assertEquals("", modern.string("referrer"))
    }

    @Test
    fun productView_withoutADeeplink_sendsNoPdAndNoLocationHref() {
        val event = ProductViewEvent.Builder().items(listOf(itemWithAllFields())).build()

        val legacyUrl = legacyUrl(sendLegacy(event, expectedRequests = 1), "d")
        val modern = modernPayload(sendModern(event))

        assertNull(legacyUrl.queryParameter("pd"))
        assertTrue(modern.get("locationHref").isJsonNull)
    }

    // -- Custom event --

    /**
     * The backend turns a custom event's declared type into `CustomEvent.customEventType`. On the
     * legacy path it reads the `type` metadata entry; on /mobile it reads the metadata's `type`
     * field, so dropping it would collapse every custom event to the same name.
     */
    @Test
    fun customEvent_sendsTheHostSuppliedTypeOnBothPaths() {
        val event = CustomEvent.Builder("Concert Viewed", mapOf("artist" to "Nils Frahm")).build()

        val legacy = legacyMetadata(sendLegacy(event, expectedRequests = 1), "ce")
        val metadata = modernPayload(sendModern(event)).getAsJsonObject("eventMetadata")

        assertEquals("Concert Viewed", legacy.string("type"))
        assertEquals("Concert Viewed", metadata.string("type"))
        assertEquals("MobileCustomEvent", metadata.string("eventType"))
    }

    @Test
    fun customEvent_sendsTheSamePropertiesOnBothPaths() {
        val properties = mapOf("artist" to "Nils Frahm", "venue" to "Barbican")
        val event = CustomEvent.Builder("Concert Viewed", properties).build()

        val legacy = legacyMetadata(sendLegacy(event, expectedRequests = 1), "ce")
        val metadata = modernPayload(sendModern(event)).getAsJsonObject("eventMetadata")

        val legacyProperties = legacyCustomProperties(legacy)
        val modernProperties = metadata.getAsJsonObject("customProperties")
        for ((key, value) in properties) {
            assertEquals(value, legacyProperties.string(key))
            assertEquals(value, modernProperties.string(key))
        }
    }

    // -- Metadata discriminator --

    /**
     * `EventMetadata` is a sealed hierarchy, so kotlinx writes a polymorphic discriminator into
     * every `eventMetadata` object. It has to be named and valued the way the events backend
     * declares it (`eventType`, short event name) - not kotlinx's default of `type` set to a Kotlin
     * FQCN, which both leaks an off-schema field and takes the `type` key custom events need.
     */
    @Test
    fun modernMetadata_isDiscriminatedByTheShortEventTypeName() {
        val expected =
            mapOf(
                "Purchase" to purchaseEvent(listOf(itemWithAllFields())),
                "ProductView" to ProductViewEvent.Builder().items(listOf(itemWithAllFields())).build(),
                "AddToCart" to AddToCartEvent.Builder().items(listOf(itemWithAllFields())).build(),
                "MobileCustomEvent" to CustomEvent.Builder("Concert Viewed", mapOf("a" to "b")).build(),
            )

        for ((eventType, event) in expected) {
            val metadata = modernPayload(sendModern(event)).getAsJsonObject("eventMetadata")

            assertEquals(eventType, metadata.string("eventType"))
            assertFalse(
                "eventMetadata should not carry a Kotlin class name: $metadata",
                metadata.entrySet().any { (_, value) ->
                    value.isJsonPrimitive && value.asJsonPrimitive.isString &&
                        value.asString.startsWith("com.attentive.")
                },
            )
        }
    }

    // -- Visitor ID, identifiers and tag version --

    @Test
    fun bothPaths_reportTheSameVisitorId() {
        for (event in ALL_EVENT_TYPES) {
            val legacyUrl = sendLegacy(event.event, event.legacyRequestCount)[0].request.url
            val modern = modernPayload(sendModern(event.event))

            assertEquals(VISITOR_ID, legacyUrl.queryParameter("u"))
            assertEquals(VISITOR_ID, modern.string("visitorId"))
        }
    }

    @Test
    fun bothPaths_reportTheSameDomain() {
        for (event in ALL_EVENT_TYPES) {
            val legacyUrl = sendLegacy(event.event, event.legacyRequestCount)[0].request.url
            val modern = modernPayload(sendModern(event.event))

            assertEquals(DOMAIN, legacyUrl.queryParameter("c"))
            assertEquals(DOMAIN, modern.string("attentiveDomain"))
        }
    }

    /**
     * The tag version ends up on the event's `request.sdkVersion`, which several backend services
     * compare against the literal `"mobile-app"` to recognise mobile-app traffic — the
     * purchase-blocking exemption and the app-specific cart link among them. Sending the SDK's
     * semver on /mobile would reclassify those events as web-tag traffic.
     */
    @Test
    fun bothPaths_reportTheSameTagVersion() {
        for (event in ALL_EVENT_TYPES) {
            val legacyUrl = sendLegacy(event.event, event.legacyRequestCount)[0].request.url
            val modern = modernPayload(sendModern(event.event))

            assertEquals("mobile-app", legacyUrl.queryParameter("v"))
            assertEquals("mobile-app", modern.string("version"))
        }
    }

    @Test
    fun bothPaths_carryTheSameContactInfoAndVendorIds() {
        val event = AddToCartEvent.Builder().items(listOf(itemWithAllFields())).build()

        val legacyUrl = legacyUrl(sendLegacy(event, expectedRequests = 1), "c")
        val identifiers = modernPayload(sendModern(event)).getAsJsonObject("identifiers")

        val legacyMetadata = JsonParser.parseString(legacyUrl.queryParameter("m")).asJsonObject
        assertEquals(EMAIL, legacyMetadata.string("email"))
        assertEquals(PHONE, legacyMetadata.string("phone"))
        assertEquals(base64(EMAIL), identifiers.string("encryptedEmail"))
        assertEquals(base64(PHONE), identifiers.string("encryptedPhone"))

        // The legacy path sends the vendor identifiers as `evs`; /mobile sends the same three as
        // typed entries in `otherIdentifiers`.
        val vendorIds = JsonParser.parseString(legacyUrl.queryParameter("evs")).asJsonArray
        assertEquals(3, vendorIds.size())
        val otherIdentifiers = identifiers.getAsJsonArray("otherIdentifiers")
        assertEquals(3, otherIdentifiers.size())
        assertEquals(
            setOf(CLIENT_USER_ID, SHOPIFY_ID, KLAVIYO_ID),
            vendorIds.map { it.asJsonObject.string("id") }.toSet(),
        )
        assertEquals(
            mapOf(
                "ClientUserId" to CLIENT_USER_ID,
                "ShopifyId" to SHOPIFY_ID,
                "KlaviyoId" to KLAVIYO_ID,
            ),
            otherIdentifiers.associate { it.asJsonObject.string("idType")!! to it.asJsonObject.string("value") },
        )
    }

    // -- Encoding --

    /**
     * The class of bug MSDK-441 fixed on iOS: a form-encoded body whose value keeps `&`, `=` or `+`
     * literal is truncated at the first one, silently dropping the event. Retrofit hands the field
     * to OkHttp's `FormBody`, which percent-encodes all three and writes UTF-8, so the whole
     * payload has to survive a strict decode.
     */
    @Test
    fun modernBody_isASingleFormEncodedFieldThatSurvivesSeparatorsAndEmoji() {
        val event = AddToCartEvent.Builder().items(listOf(awkwardItem())).build()

        val captured = sendModern(event)
        val body = captured.body

        assertEquals("application/x-www-form-urlencoded", captured.request.body?.contentType()?.toString())
        assertTrue("body should be the single field d=…, was: $body", body.startsWith("d="))
        // A single field: any `&` or `=` from the payload that leaked through unescaped would split
        // the body and truncate the event server-side. The only `=` allowed is the `d=` separator.
        val encodedValue = body.removePrefix("d=")
        assertEquals(1, body.split("&").size)
        assertEquals(1, encodedValue.split("=").size)

        val json = JsonParser.parseString(URLDecoder.decode(encodedValue, "UTF-8")).asJsonObject
        val product = json.getAsJsonObject("eventMetadata").getAsJsonObject("product")
        assertEquals(AWKWARD_NAME, product.string("name"))
        assertEquals(AWKWARD_CATEGORY, product.getAsJsonArray("categories").single().asString)
        assertEquals(AWKWARD_PRODUCT_ID, product.string("productId"))
    }

    @Test
    fun legacyQuery_survivesTheSameSeparatorsAndEmoji() {
        val event = AddToCartEvent.Builder().items(listOf(awkwardItem())).build()

        val legacyUrl = legacyUrl(sendLegacy(event, expectedRequests = 1), "c")
        val metadata = JsonParser.parseString(legacyUrl.queryParameter("m")).asJsonObject

        // The raw query must not contain the literal separators — OkHttp percent-encodes them, and
        // `queryParameter` reading them back proves the round trip rather than the encoding alone.
        val rawName = legacyUrl.encodedQuery!!
        assertFalse("unescaped '&' in the query would truncate the value", rawName.contains("Grab &"))
        assertEquals(AWKWARD_NAME, metadata.string("name"))
        assertEquals(AWKWARD_CATEGORY, metadata.string("category"))
        assertEquals(AWKWARD_PRODUCT_ID, metadata.string("productId"))
    }

    @Test
    fun customEvent_withSeparatorsAndEmojiInTheTypeAndProperties_survivesBothPaths() {
        // CustomEvent rejects quotes and brackets in `type`, so this exercises the characters a
        // host can actually send: the form-encoding separators and multi-byte text.
        val type = "Grab & Go = 1+1 🛒"
        val properties = mapOf("promo & code" to AWKWARD_NAME)
        val event = CustomEvent.Builder(type, properties).build()

        val legacy = legacyMetadata(sendLegacy(event, expectedRequests = 1), "ce")
        val metadata = modernPayload(sendModern(event)).getAsJsonObject("eventMetadata")

        assertEquals(type, legacy.string("type"))
        assertEquals(type, metadata.string("type"))
        assertEquals(AWKWARD_NAME, legacyCustomProperties(legacy).string("promo & code"))
        assertEquals(AWKWARD_NAME, metadata.getAsJsonObject("customProperties").string("promo & code"))
    }

    // -- Harness --

    private data class CapturedRequest(val request: Request, val body: String)

    private data class EventUnderTest(val event: Event, val legacyRequestCount: Int)

    /**
     * Records every request an [AttentiveApi] built on a recording client produces, short-circuiting
     * each one with a 204 so nothing leaves the test.
     */
    private fun capture(
        expectedRequests: Int,
        send: (AttentiveApi) -> Unit
    ): List<CapturedRequest> {
        val captured = Collections.synchronizedList(mutableListOf<CapturedRequest>())
        val latch = CountDownLatch(expectedRequests)
        val client =
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request()
                    val buffer = Buffer()
                    request.body?.writeTo(buffer)
                    captured.add(CapturedRequest(request, buffer.readUtf8()))
                    latch.countDown()
                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(204)
                        .message("No Content")
                        .body("".toResponseBody())
                        .build()
                }
                .build()

        send(AttentiveApi(client, DOMAIN))

        assertTrue(
            "timed out waiting for $expectedRequests request(s), saw ${captured.size}",
            latch.await(5, TimeUnit.SECONDS),
        )
        return captured.toList()
    }

    private fun sendLegacy(
        event: Event,
        expectedRequests: Int
    ): List<CapturedRequest> = capture(expectedRequests) { api -> api.sendEvent(event, USER_IDENTIFIERS, DOMAIN) }

    private fun sendModern(event: Event): CapturedRequest =
        capture(1) { api ->
            val result = runBlocking { api.recordEvent(event, USER_IDENTIFIERS, DOMAIN) }
            assertTrue("recordEvent failed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        }.single()

    private fun legacyUrl(
        captured: List<CapturedRequest>,
        eventType: String
    ): HttpUrl =
        captured.map { it.request.url }
            .first { it.queryParameter("t") == eventType }

    private fun legacyMetadata(
        captured: List<CapturedRequest>,
        eventType: String
    ): JsonObject = JsonParser.parseString(legacyUrl(captured, eventType).queryParameter("m")).asJsonObject

    private fun modernPayload(captured: CapturedRequest): JsonObject {
        assertEquals(AttentiveApi.ATTENTIVE_MOBILE_ENDPOINT_HOST, captured.request.url.host)
        val encodedJson = captured.body.removePrefix("d=")
        return JsonParser.parseString(URLDecoder.decode(encodedJson, "UTF-8")).asJsonObject
    }

    /**
     * The legacy path nests the custom-event properties as a *stringified* JSON map (see
     * `CustomEventMetadataDto`), where /mobile sends `customProperties` as a real object. Both
     * shapes are what the backend expects on each path, so the parity check is on the entries.
     */
    private fun legacyCustomProperties(metadata: JsonObject): JsonObject =
        JsonParser.parseString(metadata.string("properties")).asJsonObject

    private fun JsonObject.string(name: String): String? = get(name)?.takeUnless { it.isJsonNull }?.asString

    private fun base64(value: String): String = java.util.Base64.getEncoder().encodeToString(value.toByteArray())

    private fun purchaseEvent(
        items: List<Item>,
        cart: Cart? = null
    ): PurchaseEvent {
        val builder = PurchaseEvent.Builder(items, Order.Builder().orderId("order-1").build())
        cart?.let { builder.cart(it) }
        return builder.build()
    }

    private fun itemWithAllFields(): Item =
        Item.Builder("prod-1", "variant-1", price("15.99"))
            .name("Sweater")
            .category("Tops")
            .productImage("https://example.com/sweater.png")
            .quantity(2)
            .build()

    private fun secondItem(): Item =
        Item.Builder("prod-2", "variant-2", price("20.00"))
            .name("Scarf")
            .category("Accessories")
            .build()

    private fun awkwardItem(): Item =
        Item.Builder(AWKWARD_PRODUCT_ID, "variant & 1", price("15.99"))
            .name(AWKWARD_NAME)
            .category(AWKWARD_CATEGORY)
            .build()

    private fun price(amount: String): Price = Price.Builder().price(BigDecimal(amount)).currency(Currency.getInstance("USD")).build()

    private val ALL_EVENT_TYPES: List<EventUnderTest>
        get() =
            listOf(
                EventUnderTest(purchaseEvent(listOf(itemWithAllFields())), legacyRequestCount = 2),
                EventUnderTest(
                    ProductViewEvent.Builder().items(listOf(itemWithAllFields())).build(),
                    legacyRequestCount = 1,
                ),
                EventUnderTest(
                    AddToCartEvent.Builder().items(listOf(itemWithAllFields())).build(),
                    legacyRequestCount = 1,
                ),
                EventUnderTest(
                    CustomEvent.Builder("Concert Viewed", mapOf("artist" to "Nils Frahm")).build(),
                    legacyRequestCount = 1,
                ),
            )

    private fun JsonArray.single() = get(0)

    companion object {
        private const val DOMAIN = "someDomain"
        private const val VISITOR_ID = "someVisitorId"
        private const val EMAIL = "Youknow@email.com"
        private const val PHONE = "+15556667777"
        private const val CLIENT_USER_ID = "someClientUserId"
        private const val SHOPIFY_ID = "someShopifyId"
        private const val KLAVIYO_ID = "someKlaviyoId"
        private const val DEEPLINK = "myapp://products/sweater?colour=blue&size=M"

        // Every character that breaks a naively encoded form body, plus multi-byte text.
        private const val AWKWARD_NAME = "Grab & Go = 1+1 'best' \"deal\" 🛒🎉"
        private const val AWKWARD_CATEGORY = "Tops & Knits"
        private const val AWKWARD_PRODUCT_ID = "prod=1&2+3"

        private val USER_IDENTIFIERS =
            UserIdentifiers.Builder()
                .withVisitorId(VISITOR_ID)
                .withClientUserId(CLIENT_USER_ID)
                .withShopifyId(SHOPIFY_ID)
                .withKlaviyoId(KLAVIYO_ID)
                .withEmail(EMAIL)
                .withPhone(PHONE)
                .build()
    }
}
