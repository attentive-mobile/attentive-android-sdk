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
import com.attentive.androidsdk.internal.network.GeoAdjustedDomainInterceptor
import com.attentive.androidsdk.internal.network.Metadata
import com.attentive.androidsdk.internal.network.OrderConfirmedMetadataDto
import com.attentive.androidsdk.internal.network.ProductDto
import com.attentive.androidsdk.internal.network.ProductMetadata
import com.attentive.androidsdk.internal.network.ProductViewMetadataDto
import com.attentive.androidsdk.internal.network.PurchaseMetadataDto
import com.attentive.androidsdk.internal.network.RetrofitApiService
import com.attentive.androidsdk.internal.network.UserUpdateRequest
import com.attentive.androidsdk.internal.util.AppInfo
import com.attentive.androidsdk.push.AttentivePush
import com.attentive.androidsdk.push.TokenProvider
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import java.util.regex.Pattern


class AttentiveApi(private var httpClient: OkHttpClient, private val domain: String) {
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

    private val httpUrlEventsEndpointBuilder: HttpUrl.Builder
        get() = HttpUrl.Builder()
            .scheme("https")
            .host(ATTENTIVE_EVENTS_ENDPOINT_HOST)
            .addPathSegment("e")


    private val httpUrlPushTokenStatusEndpointBuilder: HttpUrl.Builder
        get() = HttpUrl.Builder()
            .scheme("https")
            .host(ATTENTIVE_MOBILE_ENDPOINT_HOST)
            .addPathSegment("token")

    private val httpUrlDirectOpenStatusEndpointBuilder: HttpUrl.Builder
        get() = HttpUrl.Builder()
            .scheme("https")
            .host(ATTENTIVE_MOBILE_ENDPOINT_HOST)
            .addPathSegment("mtctrl")

    private val httpUrlOptInSubscriptionStatusEndpointBuilder: HttpUrl.Builder
        get() = HttpUrl.Builder()
            .scheme("https")
            .host(ATTENTIVE_MOBILE_ENDPOINT_HOST)
            .addPathSegment("opt-in-subscriptions")

    private val httpUrlOptOutSubscriptionStatusEndpointBuilder: HttpUrl.Builder
        get() = HttpUrl.Builder()
            .scheme("https")
            .host(ATTENTIVE_MOBILE_ENDPOINT_HOST)
            .addPathSegment("opt-out-subscriptions")

    private val httpMobileEndpointBuilder: HttpUrl.Builder
        get() = HttpUrl.Builder()
            .scheme("https")
            .host(ATTENTIVE_MOBILE_ENDPOINT_HOST)


    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(httpMobileEndpointBuilder.build())
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: RetrofitApiService = retrofit.create(RetrofitApiService::class.java)

    internal suspend fun sendUserUpdate(domain: String, email: String?, phoneNumber: String?) {
        AttentiveEventTracker.instance.config.clearUser()

        val visitorId = AttentiveEventTracker.instance.config.userIdentifiers.visitorId
        if (visitorId == null) {
            Timber.e("No visitorId available, cannot send user update")
            return
        }
        val pushToken = TokenProvider.getInstance().token
        if (pushToken == null) {
            Timber.e("No push token available, cannot send user update")
            return
        }

        val contactInfo = ContactInfo().apply {
            if (email != null) {
                this.email = email
            }
            if (phoneNumber != null) {
                this.phone = phoneNumber
            }
        }

        val builder = UserIdentifiers.Builder()
        email?.let {
            builder.withEmail(it)
        }
        phoneNumber?.let {
            builder.withPhone(it)
        }

        AttentiveEventTracker.instance.config.identify(builder.build())


        api.updateUserSuspend(
            UserUpdateRequest(
                company = domain,
                userId = visitorId,
                pushToken = pushToken,
                tokenProvider = "fcm:",
                sdkVersion = AppInfo.attentiveSDKVersion,
                metadata = contactInfo
            )
        )
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
    geoAdjustedDomain: String,
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
        .addQueryParameter(
            "m",
            json.encodeToString(PolymorphicSerializer(Metadata::class), metadata)
        )

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


internal fun registerPushToken(
    token: String,
    permissionGranted: Boolean,
    userIdentifiers: UserIdentifiers,
    domain: String
) {
    getGeoAdjustedDomainAsync(domain, object : GetGeoAdjustedDomainCallback {
        override fun onFailure(reason: String?) {
            Timber.w("Could not get geo-adjusted domain. Trying to use the original domain.")
            registerPushToken(
                geoAdjustedDomain = domain,
                token = token,
                permissionGranted = permissionGranted,
                userIdentifiers = userIdentifiers
            )
        }

        override fun onSuccess(geoAdjustedDomain: String) {
            registerPushToken(
                geoAdjustedDomain = geoAdjustedDomain,
                token = token,
                permissionGranted = permissionGranted,
                userIdentifiers = userIdentifiers
            )
        }
    })
}

internal fun registerPushToken(
    geoAdjustedDomain: String,
    token: String,
    permissionGranted: Boolean,
    userIdentifiers: UserIdentifiers
) {
    val externalVendorIdsJson = buildExternalVendorIdsJson(userIdentifiers)
    val metadataJson: String
    val metadata: Metadata
    try {
        metadata = buildMetadata(userIdentifiers)
        metadataJson = json.encodeToString(metadata)
    } catch (e: SerializationException) {
        Timber.e(
            "Could not serialize metadata. Message: '%s'",
            e.message
        )
        return
    }

    //        "pd": "${buildExtraParametersWithDeeplink()}",


    val jsonBody = """
    {
        "c": "$geoAdjustedDomain",
        "v": "mobile-app-${AppInfo.attentiveSDKVersion}",
        "u": "${userIdentifiers.visitorId}",
        "evs": ${externalVendorIdsJson},
        "m": $metadataJson,
        "pt": "$token",
        "st": "$permissionGranted",
        "tp": "fcm"
    }
""".trimIndent()

    val pushUrl = httpUrlPushTokenStatusEndpointBuilder.build()

    val requestBody = RequestBody.create("application/json".toMediaType(), jsonBody)

    val request = Request.Builder()
        .url(pushUrl)
        .addHeader("x-datadog-sampling-priority", "1")
        .addHeader("Content-Type", "application/json")
        .post(requestBody)
        .build()


    httpClient.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Timber.e("Push request failed with exception ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            Timber.d("Push request success with response ${response.message}")
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
    getGeoAdjustedDomainAsync(domain, object : GetGeoAdjustedDomainCallback {
        override fun onFailure(reason: String?) {
            Timber.w("Could not get geo-adjusted domain. Trying to use the original domain.")
            sendDirectOpenStatusInternal(
                launchTypes,
                pushToken,
                callbackMap,
                permissionGranted,
                userIdentifiers,
                domain
            )
        }

        override fun onSuccess(geoAdjustedDomain: String) {
            Timber.d("Geo adjusted domain: $geoAdjustedDomain")
            sendDirectOpenStatusInternal(
                launchTypes,
                pushToken,
                callbackMap,
                permissionGranted,
                userIdentifiers,
                geoAdjustedDomain
            )
        }
    })
}

private var lastLaunchEventTimeStamp = 0L

private fun sendDirectOpenStatusInternal(
    launchType: LaunchType,
    pushToken: String,
    callbackMap: Map<String, String>,
    permissionGranted: Boolean,
    userIdentifiers: UserIdentifiers,
    geoAdjustedDomain: String,
) {

    Timber.d("sendDirectOpenStatusInternal called with alaunchType: %s", launchType.value)

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
    val metadataJson: String
    val metadata: Metadata
    try {
        metadata = buildMetadata(userIdentifiers)
        metadataJson = json.encodeToString(metadata)
    } catch (e: SerializationException) {
        Timber.e(
            "Could not serialize metadata. Message: '%s'",
            e.message
        )
        return
    }

    //TODO
    val deepLink = callbackMap[AttentivePush.ATTENTIVE_DEEP_LINK_KEY]
//        val pd = "${buildExtraParametersWithDeeplink(deepLink)}"

    // Build the data array from callbackMap
    val dataArray = callbackMap.entries.joinToString(separator = ",") { entry ->
        """"${entry.key}" : "${entry.value}""""
    }

    val eventsArray = if (launchType == LaunchType.APP_LAUNCHED) {
        """{
          "events":[
            {
                "ist": "${launchType.value}",
                "data": {$dataArray}
            }
          ],"""
    } else {
        """{
          "events":[
            {
                "ist": "${launchType.value}",
                "data": {$dataArray}
            },
            {
                "ist": "${LaunchType.APP_LAUNCHED.value}",
                "data": {$dataArray}
            }
          ],"""
    }


    val jsonBody = """
    $eventsArray
    "device":{
        "c": "$geoAdjustedDomain",
        "v": "mobile-app-${AppInfo.attentiveSDKVersion}",
        "u": "${userIdentifiers.visitorId}",
        "evs": ${externalVendorIdsJson},
        "m": $metadataJson,
        "pt": "$pushToken",
        "st": "$permissionGranted",
        "pd": "$deepLink",
        "tp": "fcm"
        }
    }
""".trimIndent()

    val openStatusUrl = httpUrlDirectOpenStatusEndpointBuilder.build()

    val requestBody = RequestBody.create("application/json".toMediaType(), jsonBody)

    val request = Request.Builder()
        .url(openStatusUrl)
        .addHeader("x-datadog-sampling-priority", "1")
        .addHeader("Content-Type", "application/json")
        .post(requestBody)
        .build()


    httpClient.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Timber.e("Push request failed with exception ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            Timber.d("Push request success with response ${response.message}")
        }
    })
}


internal fun sendOptInSubscriptionStatus(
    phoneNumber: String? = "",
    email: String? = "",
    pushToken: String?
) {
    if (pushToken == null) {
        Timber.e("Invalid push token, cannot send opt-in subscription status")
        return
    }
    getGeoAdjustedDomainAsync(
        AttentiveEventTracker.instance.config.domain,
        object : GetGeoAdjustedDomainCallback {
            override fun onFailure(reason: String?) {
                Timber.w("Could not get geo-adjusted domain. Trying to use the original domain.")
                sendOptInSubscriptionStatusInternal(
                    phoneNumber,
                    email,
                    AttentiveEventTracker.instance.config.domain,
                    pushToken
                )
            }

            override fun onSuccess(geoAdjustedDomain: String) {
                Timber.d("Geo adjusted domain: $geoAdjustedDomain")
                sendOptInSubscriptionStatusInternal(
                    phoneNumber,
                    email,
                    geoAdjustedDomain,
                    pushToken
                )
            }

            private fun sendOptInSubscriptionStatusInternal(
                phoneNumber: String? = "",
                email: String? = "",
                domain: String,
                pushToken: String,
                type: String = "MARKETING"
            ) {
                val userIdentifiers = AttentiveEventTracker.instance.config.userIdentifiers
                val externalVendorIdsJson = buildExternalVendorIdsJson(userIdentifiers)
                val visitorId = userIdentifiers.visitorId ?: ""
                val jsonBody = """
        {
            "c": "$domain",
            "v": "mobile-app",
            "u": "$visitorId",
            "evs": $externalVendorIdsJson,
            "tp": "fcm",
            "pt": "$pushToken",
            "email": "$email",
            "phone": "$phoneNumber",
            "type": "$type"
        }
    """.trimIndent()

                val url = httpUrlOptInSubscriptionStatusEndpointBuilder.build()
                val requestBody = RequestBody.create("application/json".toMediaType(), jsonBody)
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                httpClient.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Timber.e("Opt-in subscription request failed: ${e.message}")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        Timber.d("Opt-in subscription request success: ${response.message}")
                    }
                })
            }


        })
}


internal fun sendOptOutSubscriptionStatus(
    email: String?,
    phoneNumber: String?,
    domain: String,
    pushToken: String?
) {
    if (pushToken == null) {
        Timber.e("Invalid push token, cannot send opt-in subscription status")
        return
    }
    getGeoAdjustedDomainAsync(domain, object : GetGeoAdjustedDomainCallback {
        override fun onFailure(reason: String?) {
            Timber.w("Could not get geo-adjusted domain. Trying to use the original domain.")
            sendOptOutSubscriptionStatusInternal(
                domain,
                email,
                phoneNumber,
                pushToken
            )
        }

        override fun onSuccess(geoAdjustedDomain: String) {
            Timber.d("Geo adjusted domain: $geoAdjustedDomain")
            sendOptOutSubscriptionStatusInternal(
                geoAdjustedDomain,
                email,
                phoneNumber,
                pushToken
            )
        }

        private fun sendOptOutSubscriptionStatusInternal(
            domain: String,
            email: String?,
            phoneNumber: String?,
            pushToken: String
        ) {
            val userIdentifiers = AttentiveEventTracker.instance.config.userIdentifiers
            val externalVendorIdsJson = buildExternalVendorIdsJson(userIdentifiers)
            val email = email
            val phone = phoneNumber
            val visitorId = userIdentifiers.visitorId ?: ""
            val jsonBody = """
        {
            "c": "$domain",
            "v": "mobile-app",
            "u": "$visitorId",
            "evs": $externalVendorIdsJson,
            "tp": "fcm",
            "pt": "$pushToken",
            "email": "$email",
            "phone": "$phone"
        }
    """.trimIndent()

            val url = httpUrlOptOutSubscriptionStatusEndpointBuilder.build()
            val requestBody = RequestBody.create("application/json".toMediaType(), jsonBody)
            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            httpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Timber.e("Opt-out subscription request failed: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    Timber.d("Opt-out subscription request success: ${response.message}")
                }
            })
        }


    })
}

private fun buildEmptyRequest(): RequestBody {
    return RequestBody.create(null, ByteArray(0))
}

companion object {
    const val ATTENTIVE_EVENTS_ENDPOINT_HOST: String = "events.attentivemobile.com"
    const val ATTENTIVE_DTAG_URL: String = "https://cdn.attn.tv/%s/dtag.js"
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
 * Represents whether the app was launched directly or through a push notification.
 */
enum class LaunchType(val value: String) {
    DIRECT_OPEN("o"),
    APP_LAUNCHED("al");

    companion object {
        fun fromValue(value: String): LaunchType {
            return LaunchType.entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown LaunchType value: $value")
        }
    }
}
}
