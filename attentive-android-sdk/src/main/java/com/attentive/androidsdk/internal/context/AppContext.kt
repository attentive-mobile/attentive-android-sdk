package com.attentive.androidsdk.internal.context

import com.attentive.androidsdk.events.Item
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal enum class LaunchSource {
    @SerialName("push")
    PUSH,

    @SerialName("universal_link")
    UNIVERSAL_LINK,

    @SerialName("deep_link")
    DEEP_LINK,

    @SerialName("organic")
    ORGANIC,
}

/** The UTM parameters kept from an opening link. Every other query parameter is dropped. */
@Serializable
internal enum class UtmKey(val queryName: String) {
    @SerialName("source")
    SOURCE("utm_source"),

    @SerialName("medium")
    MEDIUM("utm_medium"),

    @SerialName("campaign")
    CAMPAIGN("utm_campaign"),

    @SerialName("term")
    TERM("utm_term"),

    @SerialName("content")
    CONTENT("utm_content"),
}

/** A UTM value and the time it stops being reported. Each key expires on its own. */
@Serializable
internal data class UtmValue(
    val value: String,
    val expiresAtMs: Long,
)

@Serializable
internal data class ContextProduct(
    val productId: String,
    val productVariantId: String?,
    val name: String?,
    val price: String?,
    val currency: String?,
    val imageUrl: String?,
    val observedAtMs: Long,
)

internal fun Item.toContextProduct(observedAtMs: Long): ContextProduct =
    ContextProduct(
        productId = productId,
        productVariantId = productVariantId,
        name = name,
        price = price.price.toPlainString(),
        currency = price.currency.currencyCode,
        imageUrl = productImage,
        observedAtMs = observedAtMs,
    )

@Serializable
internal data class ScreenContext(
    /** snake_case, at most 64 characters. */
    val currentScreenName: String?,
    /** `app://<package name>/<screen name>`, built from [currentScreenName] on read. */
    val currentScreenUrl: String?,
    /** Null on the first screen of a session. */
    val previousScreenName: String?,
    /** Foreground time spent on [previousScreenName]. */
    val previousScreenDurationMs: Long?,
    /** 1-based within the session, 0 before the first screen. */
    val screenIndex: Int,
)

@Serializable
internal data class ProductContext(
    /** At most 3 distinct products, newest first. */
    val recentlyViewed: List<ContextProduct>,
    /** Newest first, emptied by a purchase. */
    val recentlyAddedToCart: List<ContextProduct>,
)

@Serializable
internal data class LaunchContext(
    val source: LaunchSource,
    /** Unexpired keys only. */
    val utmParams: Map<UtmKey, UtmValue>,
    /** The opening link without its query string or fragment. */
    val landingUrl: String?,
)

@Serializable
internal data class AppContext(
    val screen: ScreenContext,
    val products: ProductContext,
    val launch: LaunchContext,
)

/** Read side of the context store, for the creative context builder. */
internal interface AppContextReader {
    val screen: ScreenContext
    val products: ProductContext
    val launch: LaunchContext

    /** All three contexts read at the same moment. */
    fun snapshot(): AppContext
}
