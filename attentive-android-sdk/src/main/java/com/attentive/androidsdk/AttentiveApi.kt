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
import com.attentive.androidsdk.internal.network.ContactInfo
import com.attentive.androidsdk.internal.network.CustomEventMetadataDto
import com.attentive.androidsdk.internal.network.DeviceInfo
import com.attentive.androidsdk.internal.network.DirectOpenRequest
import com.attentive.androidsdk.internal.network.LaunchEvent
import com.attentive.androidsdk.internal.network.Metadata
import com.attentive.androidsdk.internal.network.OptInSubscriptionRequest
import com.attentive.androidsdk.internal.network.OptOutSubscriptionRequest
import com.attentive.androidsdk.internal.network.PushTokenRequest
import com.attentive.androidsdk.internal.network.OrderConfirmedMetadataDto
import com.attentive.androidsdk.internal.network.ProductDto
import com.attentive.androidsdk.internal.network.ProductMetadata
import com.attentive.androidsdk.internal.network.ProductViewMetadataDto
import com.attentive.androidsdk.internal.network.PurchaseMetadataDto
import com.attentive.androidsdk.internal.network.RetrofitApiService
import com.attentive.androidsdk.internal.network.RetrofitEventsApiService
import com.attentive.androidsdk.internal.network.UserUpdateRequest
import com.attentive.androidsdk.internal.util.AppInfo
import com.attentive.androidsdk.push.AttentivePush
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import com.google.gson.JsonParser
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import com.attentive.androidsdk.internal.network.events.*


class AttentiveApi(private var httpClient: OkHttpClient, private val domain: String) {
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

    private val baseEventRequestJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true  // Must encode defaults to include eventType field
    }

    private val retrofitEventsEndpoint: HttpUrl.Builder
        get() = HttpUrl.Builder()
            .scheme("https")
            .host(ATTENTIVE_EVENTS_ENDPOINT_HOST)

    private val retrofitMobileEndpoint: HttpUrl.Builder
        get() = HttpUrl.Builder()
            .scheme("https")
            .host(ATTENTIVE_MOBILE_ENDPOINT_HOST)




    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(retrofitMobileEndpoint.build())
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create(
            com.google.gson.GsonBuilder()
                .create()
        ))
        .build()

    val api: RetrofitApiService = retrofit.create(RetrofitApiService::class.java)

    val retrofitEvents: Retrofit = Retrofit.Builder()
        .baseUrl(retrofitEventsEndpoint.build())
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val eventsApi: RetrofitEventsApiService = retrofitEvents.create(RetrofitEventsApiService::class.java)

    internal fun sendUserUpdate(
        domain: String,
        email: String?,
        phoneNumber: String?,
        visitorId: String,
        pushToken: String
    ) {
        val contactInfo = ContactInfo().apply {
            if (email != null) {
                this.email = email
            }
            if (phoneNumber != null) {
                this.phone = phoneNumber
            }
        }

        api.updateUser(
            UserUpdateRequest(
                company = domain,
                userId = visitorId,
                pushToken = pushToken,
                tokenProvider = "fcm",
                sdkVersion = "mobile-app-${AppInfo.attentiveSDKVersion}",
                metadata = contactInfo
            )
        ).enqueue(object : retrofit2.Callback<Unit> {
            override fun onResponse(
                call: retrofit2.Call<Unit>,
                response: retrofit2.Response<Unit>
            ) {
                if(response.isSuccessful) {
                    Timber.i("Successfully sent user update")
                } else {
                    Timber.e("Failed to send user update: ${response.code()}")
                }
            }

            override fun onFailure(call: retrofit2.Call<kotlin.Unit?>, t: kotlin.Throwable) {
               Timber.e("Failed to send user update: ${t.message}")
            }
        })
    }

            /**
     * Sends an event using the new BaseEventRequest format
     * This method supports the following event types:
     * - PurchaseEvent -> Purchase EventMetadata
     * - ProductViewEvent -> ProductView EventMetadata
     * - AddToCartEvent -> AddToCart EventMetadata
     * - CustomEvent -> MobileCustomEvent EventMetadata
     *
     * This is a suspend function that should be called from a coroutine context.
     */
    internal suspend fun recordEvent(
        event: Event,
        userIdentifiers: UserIdentifiers,
        domain: String
    ) {
        Timber.i("recordEvent called with event: %s", event.javaClass.name)

        // Validate that we have a visitorId
        if (userIdentifiers.visitorId.isNullOrEmpty()) {
            Timber.e("Cannot send event: visitorId is required but is null or empty")
            return
        }

        // Map the event to BaseEventRequest(s)
        val baseEventRequests = getBaseEventRequestsFromEvent(event, userIdentifiers, domain)

        if (baseEventRequests.isEmpty()) {
            Timber.w("No event requests generated for event: ${event.javaClass.name}")
            return
        }

        // Send each request
        for (request in baseEventRequests) {
            try {
                // Serialize the BaseEventRequest to JSON string for the -d parameter
                val eventDataJson = baseEventRequestJson.encodeToString(
                    BaseEventRequest.serializer(),
                    request
                )
                Timber.d("Sending event JSON: $eventDataJson")
                api.sendEvent(eventDataJson)
                Timber.i("Successfully sent ${request.eventType} event")
            } catch (e: Exception) {
                Timber.e("Failed to send ${request.eventType} event: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Sends an event using the new BaseEventRequest format (Java-compatible version).
     * This method supports the following event types:
     * - PurchaseEvent -> Purchase EventMetadata
     * - ProductViewEvent -> ProductView EventMetadata
     * - AddToCartEvent -> AddToCart EventMetadata
     * - CustomEvent -> MobileCustomEvent EventMetadata
     *
     * Returns a Retrofit Call that can be executed synchronously or asynchronously.
     *
     * @return A Call object that can be used to execute the request. Returns null if validation fails.
     */
   internal fun recordEventCall(
        event: Event,
        userIdentifiers: UserIdentifiers,
        domain: String,
        callback: AttentiveApiCallback
    ) {
        Timber.i("recordEventCall called with event: %s", event.javaClass.name)

        if (userIdentifiers.visitorId.isNullOrEmpty()) {
            Timber.e("Cannot send event: visitorId is required but is null or empty")
            callback.onFailure("Cannot send event: visitorId is required")
            return
        }

        val baseEventRequests = getBaseEventRequestsFromEvent(event, userIdentifiers, domain)

        if (baseEventRequests.isEmpty()) {
            Timber.w("No event requests generated for event: ${event.javaClass.name}")
            callback.onFailure("No event requests generated for event")
            return
        }

        var completedRequests = 0
        var hasError = false
        val totalRequests = baseEventRequests.size

        for (request in baseEventRequests) {
            try {
                // Serialize the BaseEventRequest to JSON string for the -d parameter
                val eventDataJson = baseEventRequestJson.encodeToString(
                    BaseEventRequest.serializer(),
                    request
                )
                Timber.d("Sending event JSON: $eventDataJson")

                val call = api.sendEvent(eventDataJson)
                call.enqueue(object : retrofit2.Callback<Unit> {
                    override fun onResponse(call: retrofit2.Call<Unit>, response: retrofit2.Response<Unit>) {
                        if (response.isSuccessful) {
                            Timber.i("Successfully sent ${request.eventType} event")
                            synchronized(this@AttentiveApi) {
                                completedRequests++
                                if (completedRequests == totalRequests && !hasError) {
                                    callback.onSuccess()
                                }
                            }
                        } else {
                            Timber.e("Failed to send ${request.eventType} event: ${response.code()}")
                            synchronized(this@AttentiveApi) {
                                if (!hasError) {
                                    hasError = true
                                    callback.onFailure("Failed to send event: ${response.code()}")
                                }
                            }
                        }
                    }

                    override fun onFailure(call: retrofit2.Call<Unit>, t: Throwable) {
                        Timber.e("Failed to send ${request.eventType} event: ${t.message}")
                        synchronized(this@AttentiveApi) {
                            if (!hasError) {
                                hasError = true
                                callback.onFailure("Failed to send event: ${t.message}")
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                Timber.e("Failed to send ${request.eventType} event: ${e.message}")
                if (!hasError) {
                    hasError = true
                    callback.onFailure("Failed to serialize event: ${e.message}")
                }
                e.printStackTrace()
            }
        }
    }



// TODO refactor to use the 'sendEvent' method
fun sendUserIdentifiersCollectedEvent(
    domain: String,
    userIdentifiers: UserIdentifiers,
    callback: AttentiveApiCallback
) {
    Timber.i("Send user identifiers $userIdentifiers")
    internalSendUserIdentifiersCollectedEventAsync(
        domain,
        userIdentifiers,
        callback
    )
}

@JvmOverloads
fun sendEvent(
    event: Event,
    userIdentifiers: UserIdentifiers,
    domain: String,
    callback: AttentiveApiCallback? = null
) {
    Timber.d("sendEvent called with event: %s \n userIdentifiers: %s \n domain: %s", event, userIdentifiers, domain)
    sendEventInternalAsync(
        getEventRequestsFromEvent(event),
        userIdentifiers,
        domain,
        callback
    )
}

private fun buildExternalVendorIdsJson(userIdentifiers: UserIdentifiers): String {
    val externalVendorIds = buildExternalVendorIds(userIdentifiers)
    return try {
        json.encodeToString(externalVendorIds)
    } catch (e: SerializationException) {
        Timber.w(
            "Could not serialize external vendor ids. Using empty array. Error: %s",
            e.message
        )
        "[]"
    }
}

// TODO replace with the generic 'sendEvent' code
private fun internalSendUserIdentifiersCollectedEventAsync(
    domain: String,
    userIdentifiers: UserIdentifiers,
    callback: AttentiveApiCallback
) {
    val externalVendorIdsJson = buildExternalVendorIdsJson(userIdentifiers)

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

    eventsApi.sendEvent(
        version = "mobile-app",
        externalVendorIds = externalVendorIdsJson,
        domain = domain,
        eventType = "idn",
        visitorId = userIdentifiers.visitorId,
        metadata = metadataJson,
    ).enqueue(object : retrofit2.Callback<Unit> {
        override fun onResponse(call: retrofit2.Call<Unit>, response: retrofit2.Response<Unit>) {
            if (response.isSuccessful) {
                callback.onSuccess()
            } else {
                callback.onFailure(
                    String.format(
                        Locale.getDefault(),
                        "Invalid response code when calling the event endpoint: '%d', message: '%s'",
                        response.code(),
                        response.message()
                    )
                )
            }
        }

        override fun onFailure(call: retrofit2.Call<Unit>, t: Throwable) {
            callback.onFailure(
                String.format("Error when calling the event endpoint: '%s'", t.message)
            )
        }
    })
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
            purchaseMetadataDto.orderId =
                purchaseEvent.order.orderId // Assuming orderId is non-nullable
            purchaseMetadataDto.cartTotal =
                cartTotalString // Assuming cartTotalString is non-nullable

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

private fun mapPurchaseEvent(
    event: PurchaseEvent,
    userIdentifiers: UserIdentifiers,
    domain: String
): List<BaseEventRequest> {
    if (event.items.isEmpty()) {
        Timber.w("Purchase event has no items. Skipping.")
        return emptyList()
    }

    val identifiers = buildIdentifiers(userIdentifiers)
    val timestamp = getCurrentTimestamp()
    val visitorId = userIdentifiers.visitorId!! // Safe because we validate it's not null in recordEvent

    val products = event.items.map { item -> itemToProduct(item) }
    val cart = event.cart?.let { cartToCartModel(it) }

    val purchaseMetadata = PurchaseMetadata(
        orderId = event.order.orderId,
        currency = event.items.firstOrNull()?.price?.currency?.currencyCode,
        orderTotal = calculateCartTotal(event.items),
        cart = cart,
        products = products
    )

    return listOf(
        BaseEventRequest(
            visitorId = visitorId,
            version = AppInfo.attentiveSDKVersion,
            attentiveDomain = domain,
            eventType = EventType.Purchase,
            timestamp = timestamp,
            identifiers = identifiers,
            eventMetadata = purchaseMetadata,
            sourceType = SourceType.mobile,
            referrer = "",
            locationHref = null
        )
    )
}

private fun mapProductViewEvent(
    event: ProductViewEvent,
    userIdentifiers: UserIdentifiers,
    domain: String
): List<BaseEventRequest> {
    if (event.items.isEmpty()) {
        Timber.w("Product View event has no items. Skipping.")
        return emptyList()
    }

    val identifiers = buildIdentifiers(userIdentifiers)
    val timestamp = getCurrentTimestamp()
    val visitorId = userIdentifiers.visitorId!! // Safe because we validate it's not null in recordEvent

    return event.items.map { item ->
        val productViewMetadata = ProductViewMetadata(
            product = itemToProduct(item),
            currency = item.price.currency.currencyCode
        )

        BaseEventRequest(
            visitorId = visitorId,
            version = AppInfo.attentiveSDKVersion,
            attentiveDomain = domain,
            eventType = EventType.ProductView,
            timestamp = timestamp,
            identifiers = identifiers,
            eventMetadata = productViewMetadata,
            sourceType = SourceType.mobile,
            referrer = event.deeplink ?: "",
            locationHref = event.deeplink
        )
    }
}

private fun mapAddToCartEvent(
    event: AddToCartEvent,
    userIdentifiers: UserIdentifiers,
    domain: String
): List<BaseEventRequest> {
    if (event.items.isEmpty()) {
        Timber.w("Add to Cart event has no items. Skipping.")
        return emptyList()
    }

    val identifiers = buildIdentifiers(userIdentifiers)
    val timestamp = getCurrentTimestamp()
    val visitorId = userIdentifiers.visitorId!! // Safe because we validate it's not null in recordEvent

    return event.items.map { item ->
        val addToCartMetadata = AddToCartMetadata(
            product = itemToProduct(item),
            currency = item.price.currency.currencyCode
        )

        BaseEventRequest(
            visitorId = visitorId,
            version = AppInfo.attentiveSDKVersion,
            attentiveDomain = domain,
            eventType = EventType.AddToCart,
            timestamp = timestamp,
            identifiers = identifiers,
            eventMetadata = addToCartMetadata,
            sourceType = SourceType.mobile,
            referrer = event.deeplink ?: "",
            locationHref = event.deeplink
        )
    }
}

private fun mapCustomEvent(
    event: CustomEvent,
    userIdentifiers: UserIdentifiers,
    domain: String
): List<BaseEventRequest> {
    val identifiers = buildIdentifiers(userIdentifiers)
    val timestamp = getCurrentTimestamp()
    val visitorId = userIdentifiers.visitorId!! // Safe because we validate it's not null in recordEvent

    val customEventMetadata = MobileCustomEventMetadata(
        customProperties = event.properties.ifEmpty { null }
    )

    return listOf(
        BaseEventRequest(
            visitorId = visitorId,
            version = AppInfo.attentiveSDKVersion,
            attentiveDomain = domain,
            eventType = EventType.MobileCustomEvent,
            timestamp = timestamp,
            identifiers = identifiers,
            eventMetadata = customEventMetadata,
            sourceType = SourceType.mobile,
            referrer = "",
            locationHref = null
        )
    )
}

private fun getCurrentTimestamp(): String {
    return java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }.format(java.util.Date())
}

private fun sendEventInternalAsync(
    eventRequests: List<EventRequest>,
    userIdentifiers: UserIdentifiers,
    domain: String,
    callback: AttentiveApiCallback?
) {
    if (callback == null) {
        // If no callback, just send all requests without tracking
        for (eventRequest in eventRequests) {
            sendEventInternalAsync(eventRequest, userIdentifiers, domain, null)
        }
        return
    }

    var completedRequests = 0
    var hasError = false
    val totalRequests = eventRequests.size

    for (eventRequest in eventRequests) {
        sendEventInternalAsync(eventRequest, userIdentifiers, domain, object : AttentiveApiCallback {
            override fun onSuccess() {
                synchronized(this@AttentiveApi) {
                    completedRequests++
                    if (completedRequests == totalRequests && !hasError) {
                        callback.onSuccess()
                    }
                }
            }

            override fun onFailure(message: String?) {
                synchronized(this@AttentiveApi) {
                    if (!hasError) {
                        hasError = true
                        callback.onFailure(message)
                    }
                }
            }
        })
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

    val metadataJson = json.encodeToString(PolymorphicSerializer(Metadata::class), metadata)
    val extraParameters = eventRequest.extraParameters ?: emptyMap()

    Timber.i("Send event type: %s, domain: %s", eventRequest.type.abbreviation, domain)

    eventsApi.sendEvent(
        version = "mobile-app",
        externalVendorIds = externalVendorIdsJson,
        domain = domain,
        eventType = eventRequest.type.abbreviation,
        visitorId = userIdentifiers.visitorId,
        metadata = metadataJson,
        extraParameters = extraParameters,
    ).enqueue(object : retrofit2.Callback<Unit> {
        override fun onResponse(call: retrofit2.Call<Unit>, response: retrofit2.Response<Unit>) {
            if (response.isSuccessful) {
                Timber.i("Sent the '${eventRequest.type}' request successfully.")
                callback?.onSuccess()
            } else {
                val error = "Could not send the request. Invalid response code: ${response.code()}, message: ${response.message()}"
                Timber.e(error)
                callback?.onFailure(error)
            }
        }

        override fun onFailure(call: retrofit2.Call<Unit>, t: Throwable) {
            val error = "Could not send the request. Error: ${t.message}"
            Timber.e(error)
            callback?.onFailure(error)
        }
    })
}


internal fun registerPushToken(
    token: String,
    permissionGranted: Boolean,
    userIdentifiers: UserIdentifiers,
    domain: String
) {
    registerPushTokenInternal(
        domain = domain,
        token = token,
        permissionGranted = permissionGranted,
        userIdentifiers = userIdentifiers
    )
}

private fun registerPushTokenInternal(
    domain: String,
    token: String,
    permissionGranted: Boolean,
    userIdentifiers: UserIdentifiers
) {
    if (userIdentifiers.visitorId.isNullOrEmpty()) {
        Timber.e("No visitorId available, cannot register push token")
        return
    }

    val externalVendorIdsJson = buildExternalVendorIdsJson(userIdentifiers)
    val metadata: Metadata
    try {
        metadata = buildMetadata(userIdentifiers)
    } catch (e: SerializationException) {
        Timber.e("Could not serialize metadata. Message: '%s'", e.message)
        return
    }

    val request = PushTokenRequest(
        company = domain,
        version = "mobile-app-${AppInfo.attentiveSDKVersion}",
        visitorId = userIdentifiers.visitorId,
        externalVendorIds = JsonParser.parseString(externalVendorIdsJson),
        metadata = ContactInfo(
            phone = metadata.phone ?: "",
            email = metadata.email ?: "",
        ),
        pushToken = token,
        permissionGranted = permissionGranted.toString(),
    )

    api.registerPushToken(request).enqueue(object : retrofit2.Callback<Unit> {
        override fun onResponse(call: retrofit2.Call<Unit>, response: retrofit2.Response<Unit>) {
            Timber.i("Push request success with response ${response.message()}")
        }

        override fun onFailure(call: retrofit2.Call<Unit>, t: Throwable) {
            Timber.e("Push request failed with exception ${t.message}")
        }
    })
}

internal fun sendDirectOpenStatus(
    launchTypes: LaunchType,
    pushToken: String,
    callbackMap: Map<String, String>,
    permissionGranted: Boolean,
    userIdentifiers: UserIdentifiers,
    domain: String
) {
    sendDirectOpenStatusInternal(
        launchTypes,
        pushToken,
        callbackMap,
        permissionGranted,
        userIdentifiers,
        domain
    )
}

private var lastLaunchEventTimeStamp = 0L

private fun sendDirectOpenStatusInternal(
    launchType: LaunchType,
    pushToken: String,
    callbackMap: Map<String, String>,
    permissionGranted: Boolean,
    userIdentifiers: UserIdentifiers,
    domain: String,
) {

    Timber.d("sendDirectOpenStatusInternal called with launchType: %s", launchType.value)

    if (userIdentifiers.visitorId.isNullOrEmpty()) {
        Timber.e("No visitorId available, cannot send direct open status")
        return
    }

    //TODO root cause triage this
    if (lastLaunchEventTimeStamp == 0L) {
        lastLaunchEventTimeStamp = System.currentTimeMillis()
    } else if (System.currentTimeMillis() - lastLaunchEventTimeStamp < 3000) {
        Timber.d("Debouncing launch event because it was sent too recently (<3 seconds ago).")
        return
    } else {
        val delta = System.currentTimeMillis() - lastLaunchEventTimeStamp
        Timber.d("Delta $delta ms since last launch event, sending.")
        lastLaunchEventTimeStamp = System.currentTimeMillis()
    }

    val externalVendorIdsJson = buildExternalVendorIdsJson(userIdentifiers)
    val metadata: Metadata
    try {
        metadata = buildMetadata(userIdentifiers)
    } catch (e: SerializationException) {
        Timber.e("Could not serialize metadata. Message: '%s'", e.message)
        return
    }

    val deepLink = callbackMap[AttentivePush.ATTENTIVE_DEEP_LINK_KEY]

    val events = mutableListOf(
        LaunchEvent(type = launchType.value, data = callbackMap)
    )
    if (launchType != LaunchType.APP_LAUNCHED) {
        events.add(LaunchEvent(type = LaunchType.APP_LAUNCHED.value, data = callbackMap))
    }

    val request = DirectOpenRequest(
        events = events,
        device = DeviceInfo(
            company = domain,
            version = "mobile-app-${AppInfo.attentiveSDKVersion}",
            visitorId = userIdentifiers.visitorId,
            externalVendorIds = JsonParser.parseString(externalVendorIdsJson),
            metadata = ContactInfo(
                phone = metadata.phone ?: "",
                email = metadata.email ?: "",
            ),
            pushToken = pushToken,
            permissionGranted = permissionGranted.toString(),
            deepLink = deepLink,
        ),
    )

    api.sendDirectOpenStatus(request).enqueue(object : retrofit2.Callback<Unit> {
        override fun onResponse(call: retrofit2.Call<Unit>, response: retrofit2.Response<Unit>) {
            Timber.i("Direct open status request success with response ${response.message()}")
        }

        override fun onFailure(call: retrofit2.Call<Unit>, t: Throwable) {
            Timber.e("Direct open status request failed with exception ${t.message}")
        }
    })
}


internal fun sendOptInSubscriptionStatus(
    phoneNumber: String? = "",
    email: String? = "",
    pushToken: String?,
    domain: String,
    userIdentifiers: UserIdentifiers
) {
    if (pushToken == null) {
        Timber.e("Invalid push token, cannot send opt-in subscription status")
        return
    }
    if (userIdentifiers.visitorId.isNullOrEmpty()) {
        Timber.e("No visitorId available, cannot send opt-in subscription")
        return
    }
    val externalVendorIdsJson = buildExternalVendorIdsJson(userIdentifiers)

    val request = OptInSubscriptionRequest(
        company = domain,
        version = "mobile-app",
        visitorId = userIdentifiers.visitorId,
        externalVendorIds = JsonParser.parseString(externalVendorIdsJson),
        pushToken = pushToken,
        email = email,
        phone = phoneNumber,
    )

    api.optInSubscription(request).enqueue(object : retrofit2.Callback<Unit> {
        override fun onResponse(call: retrofit2.Call<Unit>, response: retrofit2.Response<Unit>) {
            Timber.i("Opt-in subscription request success: ${response.message()}")
        }

        override fun onFailure(call: retrofit2.Call<Unit>, t: Throwable) {
            Timber.e("Opt-in subscription request failed: ${t.message}")
        }
    })
}


internal fun sendOptOutSubscriptionStatus(
    email: String?,
    phoneNumber: String?,
    domain: String,
    pushToken: String?,
    userIdentifiers: UserIdentifiers
) {
    if (pushToken == null) {
        Timber.e("Invalid push token, cannot send opt-out subscription status")
        return
    }
    if (userIdentifiers.visitorId.isNullOrEmpty()) {
        Timber.e("No visitorId available, cannot send opt-out subscription")
        return
    }
    val externalVendorIdsJson = buildExternalVendorIdsJson(userIdentifiers)

    val request = OptOutSubscriptionRequest(
        company = domain,
        version = "mobile-app",
        visitorId = userIdentifiers.visitorId,
        externalVendorIds = JsonParser.parseString(externalVendorIdsJson),
        pushToken = pushToken,
        email = email,
        phone = phoneNumber,
    )

    api.optOutSubscription(request).enqueue(object : retrofit2.Callback<Unit> {
        override fun onResponse(call: retrofit2.Call<Unit>, response: retrofit2.Response<Unit>) {
            Timber.i("Opt-out subscription request success: ${response.message()}")
        }

        override fun onFailure(call: retrofit2.Call<Unit>, t: Throwable) {
            Timber.e("Opt-out subscription request failed: ${t.message}")
        }
    })
}

// Test helper functions - marked as @VisibleForTesting internal so tests can access them
@VisibleForTesting
internal fun getBaseEventRequestsFromEvent(
    event: Event,
    userIdentifiers: UserIdentifiers,
    domain: String
): List<BaseEventRequest> {
    return when (event) {
        is PurchaseEvent -> mapPurchaseEvent(event, userIdentifiers, domain)
        is ProductViewEvent -> mapProductViewEvent(event, userIdentifiers, domain)
        is AddToCartEvent -> mapAddToCartEvent(event, userIdentifiers, domain)
        is CustomEvent -> mapCustomEvent(event, userIdentifiers, domain)
        else -> {
            Timber.e("Unknown Event type: ${event.javaClass.name}")
            emptyList()
        }
    }
}

@VisibleForTesting
internal fun buildIdentifiers(userIdentifiers: UserIdentifiers): Identifiers {
    val otherIdentifiers = mutableListOf<OtherIdentifier>()

    userIdentifiers.clientUserId?.let {
        otherIdentifiers.add(
            OtherIdentifier(
                idType = IdType.ClientUserId,
                value = it
            )
        )
    }

    userIdentifiers.shopifyId?.let {
        otherIdentifiers.add(
            OtherIdentifier(
                idType = IdType.ShopifyId,
                value = it
            )
        )
    }

    userIdentifiers.klaviyoId?.let {
        otherIdentifiers.add(
            OtherIdentifier(
                idType = IdType.KlaviyoId,
                value = it
            )
        )
    }

    userIdentifiers.customIdentifiers.forEach { (key, value) ->
        otherIdentifiers.add(
            OtherIdentifier(
                idType = IdType.CustomId,
                value = value,
                name = key
            )
        )
    }

    return Identifiers(
        encryptedEmail = userIdentifiers.email?.let { android.util.Base64.encodeToString(it.toByteArray(), android.util.Base64.NO_WRAP) },
        encryptedPhone = userIdentifiers.phone?.let { android.util.Base64.encodeToString(it.toByteArray(), android.util.Base64.NO_WRAP) },
        otherIdentifiers = otherIdentifiers.ifEmpty { null }
    )
}

@VisibleForTesting
internal fun itemToProduct(item: Item): Product {
    return Product(
        productId = item.productId,
        variantId = item.productVariantId,
        name = item.name,
        variantName = null,
        imageUrl = item.productImage,
        categories = item.category?.let { listOf(it) },
        price = item.price.price.toPlainString(),
        quantity = item.quantity,
        productUrl = null
    )
}

@VisibleForTesting
internal fun cartToCartModel(cart: com.attentive.androidsdk.events.Cart): Cart {
    return Cart(
        cartTotal = null,
        cartCoupon = cart.cartCoupon,
        cartDiscount = null,
        cartId = cart.cartId
    )
}

@VisibleForTesting
internal fun calculateCartTotal(items: List<Item>): String {
    var cartTotal = BigDecimal.ZERO
    for (item in items) {
        cartTotal = cartTotal.add(item.price.price)
    }
    return cartTotal.setScale(2, RoundingMode.DOWN).toPlainString()
}

companion object {
    const val ATTENTIVE_EVENTS_ENDPOINT_HOST: String = "events.attentivemobile.com"
    const val ATTENTIVE_MOBILE_ENDPOINT_HOST: String = "mobile.attentivemobile.com"
//        const val ATTENTIVE_DEV_MOBILE_ENDPOINT: String = "mobile.dev.attentivemobile.com"

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

/***
 * Represents whether the app was launched from or through a push notification.
 */
enum class LaunchType(val value: String) {
    //Opened from push notification
    DIRECT_OPEN("o"),
    //Opened from launcher
    APP_LAUNCHED("al");

    companion object {
        fun fromValue(value: String): LaunchType {
            return LaunchType.entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown LaunchType value: $value")
        }
    }
}
}
