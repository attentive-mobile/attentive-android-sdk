package com.attentive.androidsdk

import androidx.annotation.VisibleForTesting
import com.attentive.androidsdk.events.AddToCartEvent
import com.attentive.androidsdk.events.CustomEvent
import com.attentive.androidsdk.events.Event
import com.attentive.androidsdk.events.Item
import com.attentive.androidsdk.events.ProductViewEvent
import com.attentive.androidsdk.events.PurchaseEvent
import com.attentive.androidsdk.internal.events.InfoEvent
import com.attentive.androidsdk.internal.network.AddToCartMetadataDto
import com.attentive.androidsdk.internal.network.CustomEventMetadataDto
import com.attentive.androidsdk.internal.network.Metadata
import com.attentive.androidsdk.internal.network.OrderConfirmedMetadataDto
import com.attentive.androidsdk.internal.network.ProductDto
import com.attentive.androidsdk.internal.network.ProductMetadata
import com.attentive.androidsdk.internal.network.ProductViewMetadataDto
import com.attentive.androidsdk.internal.network.PurchaseMetadataDto
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.OkHttpClient.Builder.*
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import java.util.regex.Pattern

import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass



class AttentiveApi(private val httpClient: OkHttpClient) {
    @get:VisibleForTesting
    var cachedGeoAdjustedDomain: String? = null
        private set



    val metadataModule = SerializersModule {
        polymorphic(Metadata::class) {
            subclass(Metadata::class)
            subclass(ProductMetadata::class)
            subclass(OrderConfirmedMetadataDto::class)
            subclass(CustomEventMetadataDto::class)
            subclass(AddToCartMetadataDto::class)
            subclass(ProductViewMetadataDto::class)
            subclass(PurchaseMetadataDto::class)
        }
    }
        val json = Json {
            serializersModule = metadataModule
            classDiscriminator = "className" // Helps identify the subclass
            ignoreUnknownKeys = true
        }

    // TODO refactor to use the 'sendEvent' method
    fun sendUserIdentifiersCollectedEvent(
        domain: String,
        userIdentifiers: UserIdentifiers,
        callback: AttentiveApiCallback
    ) {
        // first get the geo-adjusted domain, and then call the events endpoint
        getGeoAdjustedDomainAsync(domain, object : GetGeoAdjustedDomainCallback {
            override fun onFailure(reason: String?) {
                callback.onFailure(reason)
            }

            override fun onSuccess(geoAdjustedDomain: String) {
                internalSendUserIdentifiersCollectedEventAsync(
                    geoAdjustedDomain,
                    userIdentifiers,
                    callback
                )
            }
        })
    }

    @JvmOverloads
    fun sendEvent(
        event: Event,
        userIdentifiers: UserIdentifiers,
        domain: String,
        callback: AttentiveApiCallback? = null
    ) {
        Timber.d("sendEvent called with event: %s", event.javaClass.name)
        getGeoAdjustedDomainAsync(domain, object : GetGeoAdjustedDomainCallback {
            override fun onFailure(reason: String?) {
                Timber.w("Could not get geo-adjusted domain. Trying to use the original domain.")
                sendEvent(event, userIdentifiers, domain)
            }

            override fun onSuccess(geoAdjustedDomain: String) {
                sendEvent(event, userIdentifiers, geoAdjustedDomain)
            }

            fun sendEvent(event: Event, userIdentifiers: UserIdentifiers, domain: String) {
                sendEventInternalAsync(
                    getEventRequestsFromEvent(event),
                    userIdentifiers,
                    domain,
                    callback
                )
            }
        })
    }

    @VisibleForTesting
    internal interface GetGeoAdjustedDomainCallback {
        fun onFailure(reason: String?)

        fun onSuccess(geoAdjustedDomain: String)
    }

    @VisibleForTesting
    internal fun getGeoAdjustedDomainAsync(domain: String, callback: GetGeoAdjustedDomainCallback) {
        if (cachedGeoAdjustedDomain != null) {
            callback.onSuccess(cachedGeoAdjustedDomain!!)
            return
        }

        val url = String.format(ATTENTIVE_DTAG_URL, domain)
        val request: Request = Request.Builder().url(url).build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure("Getting geo-adjusted domain failed: " + e.message)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                // Check explicitly for 200 (instead of response.isSuccessful()) because the response only has the tag in the body when its code is 200
                if (response.code != 200) {
                    callback.onFailure(
                        String.format(
                            Locale.getDefault(),
                            "Getting geo-adjusted domain returned invalid code: '%d', message: '%s'",
                            response.code,
                            response.message
                        )
                    )
                    return
                }

                val body = response.body

                if (body == null) {
                    callback.onFailure("Getting geo-adjusted domain returned no body")
                    return
                }

                var fullTag: String? = null
                if (response.body != null) {
                    fullTag = response.body!!.string()
                }
                val geoAdjustedDomain = parseAttentiveDomainFromTag(fullTag!!)

                if (geoAdjustedDomain == null) {
                    callback.onFailure("Could not parse the domain from the full tag")
                    return
                }

                cachedGeoAdjustedDomain = geoAdjustedDomain
                callback.onSuccess(geoAdjustedDomain)
            }
        })
    }

    // TODO replace with the generic 'sendEvent' code
    private fun internalSendUserIdentifiersCollectedEventAsync(
        geoAdjustedDomain: String,
        userIdentifiers: UserIdentifiers,
        callback: AttentiveApiCallback
    ) {
        val externalVendorIdsJson: String
        try {
            val externalVendorIds = buildExternalVendorIds(userIdentifiers)
            externalVendorIdsJson = json.encodeToString(externalVendorIds)
        } catch (e: SerializationException) {
            callback.onFailure(
                String.format(
                    "Could not serialize the UserIdentifiers. Message: '%s'",
                    e.message
                )
            )
            return
        }

        val metadataJson: String
        try {
            val metadata = buildMetadata(userIdentifiers)
            metadataJson = json.encodeToString(metadata)
        } catch (e: SerializationException) {
            callback.onFailure(
                String.format(
                    "Could not serialize metadata. Message: '%s'",
                    e.message
                )
            )
            return
        }

        val urlBuilder = httpUrlEventsEndpointBuilder
            .addQueryParameter("tag", "modern")
            .addQueryParameter("v", "mobile-app")
            .addQueryParameter("c", geoAdjustedDomain)
            .addQueryParameter("t", "idn")
            .addQueryParameter("evs", externalVendorIdsJson)
            .addQueryParameter("m", metadataJson)
            .addQueryParameter("lt", "0")

        if (userIdentifiers.visitorId != null) {
            urlBuilder.addQueryParameter("u", userIdentifiers.visitorId)
        }

        val url: HttpUrl = urlBuilder.build()

        val request: Request = Request.Builder().url(url).post(buildEmptyRequest()).build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure(
                    String.format(
                        "Error when calling the event endpoint: '%s'",
                        e.message
                    )
                )
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    callback.onFailure(
                        String.format(
                            Locale.getDefault(),
                            "Invalid response code when calling the event endpoint: '%d', message: '%s'",
                            response.code,
                            response.message
                        )
                    )
                    return
                }

                callback.onSuccess()
            }
        })
    }

    private val httpUrlEventsEndpointBuilder: HttpUrl.Builder
        get() = HttpUrl.Builder()
            .scheme("https")
            .host(ATTENTIVE_EVENTS_ENDPOINT_HOST)
            .addPathSegment("e")

    private fun parseAttentiveDomainFromTag(tag: String): String? {
        val pattern = Pattern.compile("='([a-z0-9-]+)[.]attn[.]tv'")
        val matcher = pattern.matcher(tag)
        if (matcher.find()) {
            if (matcher.groupCount() == 1) {
                return matcher.group(1)
            }
        }

        return null
    }

    private fun buildMetadata(userIdentifiers: UserIdentifiers): Metadata {
        val metadata = Metadata()

        if (userIdentifiers.phone != null) {
            metadata.phone = userIdentifiers.phone
        }

        if (userIdentifiers.email != null) {
            metadata.email = userIdentifiers.email
        }

        return metadata
    }

    private fun buildExternalVendorIds(userIdentifiers: UserIdentifiers): List<ExternalVendorId> {
        val externalVendorIdList: MutableList<ExternalVendorId> = ArrayList()

        if (userIdentifiers.clientUserId != null) {
            externalVendorIdList.add(
                object : ExternalVendorId() {
                    init {
                        vendor = Vendor.CLIENT_USER
                        id = userIdentifiers.clientUserId
                    }
                })
        }

        if (userIdentifiers.shopifyId != null) {
            externalVendorIdList.add(object : ExternalVendorId() {
                init {
                    vendor = Vendor.SHOPIFY
                    id = userIdentifiers.shopifyId
                }
            })
        }

        if (userIdentifiers.klaviyoId != null) {
            externalVendorIdList.add(object : ExternalVendorId() {
                init {
                    vendor = Vendor.KLAVIYO
                    id = userIdentifiers.klaviyoId
                }
            })
        }

        for ((key, value) in userIdentifiers.customIdentifiers) {
            externalVendorIdList.add(object : ExternalVendorId() {
                init {
                    vendor = Vendor.CUSTOM_USER
                    id = value
                    name = key
                }
            })
        }

        return externalVendorIdList
    }

    private class EventRequest @JvmOverloads constructor(
        val metadata: Metadata,
        val type: Type,
        val extraParameters: Map<String, String>? = null
    ) {
        enum class Type(val abbreviation: String) {
            PURCHASE("p"),
            USER_IDENTIFIER_COLLECTED("idn"),
            ORDER_CONFIRMED("oc"),
            PRODUCT_VIEW("d"),
            ADD_TO_CART("c"),
            INFO("i"),
            CUSTOM_EVENT("ce")
        }
    }

    // One event can produce multiple requests (e.g. one PurchaseEvent with multiple items should be broken into separate Purchase requests)
    private fun getEventRequestsFromEvent(event: Event): List<EventRequest> {
        val eventRequests: MutableList<EventRequest> = ArrayList()
        if (event is PurchaseEvent) {
            val purchaseEvent = event

            if (purchaseEvent.items.isEmpty()) {
                Timber.w(javaClass.name, "Purchase event has no items. Skipping.")
                return listOf()
            }

            var cartTotal = BigDecimal.ZERO
            for (item in purchaseEvent.items) {
                cartTotal = cartTotal.add(item?.price?.price)
            }
            val cartTotalString = cartTotal.setScale(2, RoundingMode.DOWN).toPlainString()

            // Create Purchase requests
            for (item in purchaseEvent.items) {
                val purchaseMetadataDto = PurchaseMetadataDto()
                purchaseMetadataDto.currency = item?.price?.currency?.currencyCode
                purchaseMetadataDto.price = item?.price?.price?.toPlainString()
                purchaseMetadataDto.name = item?.name
                purchaseMetadataDto.image = item?.productImage
                purchaseMetadataDto.productId = item?.productId
                purchaseMetadataDto.subProductId = item?.productVariantId
                purchaseMetadataDto.category = item?.category
                purchaseMetadataDto.quantity = item?.quantity?.toString()
                purchaseMetadataDto.orderId = purchaseEvent.order.orderId // Assuming orderId is non-nullable
                purchaseMetadataDto.cartTotal = cartTotalString // Assuming cartTotalString is non-nullable

                if (purchaseEvent.cart != null) {
                    purchaseMetadataDto.cartId = purchaseEvent.cart.cartId
                    purchaseMetadataDto.cartCoupon = purchaseEvent.cart.cartCoupon
                }
                eventRequests.add(EventRequest(purchaseMetadataDto, EventRequest.Type.PURCHASE))
            }

            // Create OrderConfirmed request
            val ocMetadata = OrderConfirmedMetadataDto()
            ocMetadata.orderId = purchaseEvent.order.orderId
            ocMetadata.currency = purchaseEvent.items[0]?.price?.currency?.currencyCode
            ocMetadata.cartTotal = cartTotalString
            val products: MutableList<ProductDto> = ArrayList()
            for (item in purchaseEvent.items) {
                val product = ProductDto()
                product.productId = item?.productId
                product.subProductId = item?.productVariantId
                product.currency = item?.price?.currency?.currencyCode
                product.category = item?.category
                product.quantity = item?.quantity.toString()
                product.name = item?.name
                product.price = item?.price?.price?.toPlainString()
                product.image = item?.productImage
                products.add(product)
            }
            ocMetadata.products = products
            eventRequests.add(EventRequest(ocMetadata, EventRequest.Type.ORDER_CONFIRMED))
        } else if (event is ProductViewEvent) {
            val productViewEvent = event

            if (productViewEvent.items.isEmpty()) {
                Timber.w(javaClass.name, "Product View event has no items. Skipping.")
                return listOf()
            }

            for (item in productViewEvent.items) {
                val productViewMetadata = getProductViewMetadataDto(item)

                eventRequests.add(
                    EventRequest(
                        productViewMetadata,
                        EventRequest.Type.PRODUCT_VIEW,
                        buildExtraParametersWithDeeplink(event.deeplink)
                    )
                )
            }
        } else if (event is AddToCartEvent) {
            val addToCartEvent = event

            if (addToCartEvent.items.isEmpty()) {
                Timber.w(javaClass.name, "Add to Cart event has no items. Skipping.")
                return listOf()
            }

            for (item in addToCartEvent.items) {
                val addToCartMetadataDto = AddToCartMetadataDto()
                addToCartMetadataDto.currency = item.price.currency.currencyCode
                addToCartMetadataDto.price = item.price.price.toPlainString()
                addToCartMetadataDto.name = item.name
                addToCartMetadataDto.image = item.productImage
                addToCartMetadataDto.productId = item.productId
                addToCartMetadataDto.subProductId = item.productVariantId
                addToCartMetadataDto.category = item.category
                addToCartMetadataDto.quantity = item.quantity.toString()
                eventRequests.add(
                    EventRequest(
                        addToCartMetadataDto,
                        EventRequest.Type.ADD_TO_CART,
                        buildExtraParametersWithDeeplink(addToCartEvent.deeplink)
                    )
                )
            }
        } else if (event is InfoEvent) {
            eventRequests.add(EventRequest(Metadata(), EventRequest.Type.INFO))
        } else if (event is CustomEvent) {
            val customEvent = event

            val metadataDto = CustomEventMetadataDto()
            metadataDto.type = customEvent.type
            metadataDto.properties = customEvent.properties

            eventRequests.add(EventRequest(metadataDto, EventRequest.Type.CUSTOM_EVENT))
        } else {
            val error = "Unknown Event type: " + event.javaClass.name
            Timber.e(javaClass.name, error)
        }

        return eventRequests
    }

    private fun sendEventInternalAsync(
        eventRequests: List<EventRequest>,
        userIdentifiers: UserIdentifiers,
        domain: String,
        callback: AttentiveApiCallback?
    ) {
        for (eventRequest in eventRequests) {
            sendEventInternalAsync(eventRequest, userIdentifiers, domain, callback)
        }
    }

    private fun sendEventInternalAsync(
        eventRequest: EventRequest,
        userIdentifiers: UserIdentifiers,
        domain: String,
        callback: AttentiveApiCallback?
    ) {
        val metadata: Metadata = eventRequest.metadata
        metadata.enrichWithIdentifiers(userIdentifiers)

        var externalVendorIdsJson: String?
        try {
            val externalVendorIds = buildExternalVendorIds(userIdentifiers)
            externalVendorIdsJson = json.encodeToString(externalVendorIds)
        } catch (e: SerializationException) {
            Timber.w(
                "Could not serialize external vendor ids. Using empty array. Error: %s",
                e.message
            )
            externalVendorIdsJson = "[]"
        }

        val urlBuilder = httpUrlEventsEndpointBuilder
            .addQueryParameter("v", "mobile-app")
            .addQueryParameter("lt", "0")
            .addQueryParameter("tag", "modern")
            .addQueryParameter("evs", externalVendorIdsJson)
            .addQueryParameter("c", domain)
            .addQueryParameter("t", eventRequest.type.abbreviation)
            .addQueryParameter("u", userIdentifiers.visitorId)
            .addQueryParameter("m", json.encodeToString(PolymorphicSerializer(Metadata::class), metadata))

        if (eventRequest.extraParameters != null) {
            for ((key, value) in eventRequest.extraParameters.entries) {
                urlBuilder.addQueryParameter(key, value)
            }
        }

        val url: HttpUrl = urlBuilder.build()
        Timber.i("Send event url: %s", url.toString())

        val request: Request = Request.Builder().url(url).post(buildEmptyRequest()).build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                val error = "Could not send the request. Error: " + e.message
                Timber.e(error)
                callback?.onFailure(error)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    val error =
                        ("Could not send the request. Invalid response code: " + response.code + ", message: "
                                + response.message)
                    Timber.e(error)
                    callback?.onFailure(error)
                    return
                }

                Timber.i("Sent the '${eventRequest.type}' request successfully.")
            }
        })
    }

    private fun serialize(`object`: Any): String? {
        try {
            return Json.encodeToString(`object`)
        } catch (e: SerializationException) {
            throw RuntimeException("Could not serialize. Error: " + e.message, e)
        } catch (e: IllegalArgumentException) {
            throw RuntimeException(
                """
                    Could not serialize. If it's due to no default constructor, consider changing proguard rules to avoid changing the class that's causing the error. If you are not sure how to do so, consider adding the following:
                    -keep class com.attentive.androidsdk.** { *; }
                    Error: ${e.message}
                    """.trimIndent(), e
            )
        }
    }

    private fun buildEmptyRequest(): RequestBody {
        return RequestBody.create(null, ByteArray(0))
    }

    companion object {
        const val ATTENTIVE_EVENTS_ENDPOINT_HOST: String = "events.attentivemobile.com"
        const val ATTENTIVE_DTAG_URL: String = "https://cdn.attn.tv/%s/dtag.js"

        private fun getProductViewMetadataDto(item: Item): ProductViewMetadataDto {
            val productViewMetadata = ProductViewMetadataDto()
            productViewMetadata.currency = item.price.currency.currencyCode
            productViewMetadata.price = item.price.price.toPlainString()
            productViewMetadata.name = item.name
            productViewMetadata.image = item.productImage
            productViewMetadata.productId = item.productId
            productViewMetadata.subProductId = item.productVariantId
            productViewMetadata.category = item.category
            return productViewMetadata
        }

        private fun buildExtraParametersWithDeeplink(deeplink: String?): Map<String, String> {
            if (deeplink == null) {
                return HashMap()
            } else {
                val extraParameters = HashMap<String, String>()
                extraParameters["pd"] = deeplink
                return extraParameters
            }
        }
    }
}
