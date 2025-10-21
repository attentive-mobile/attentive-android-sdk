package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.Serializable

@Serializable
data class BaseEventRequest(
    val visitorId: String,
    val version: String,
    val attentiveDomain: String,
    val eventType: EventType,
    val timestamp: String,
    val identifiers: Identifiers,
    val eventMetadata: EventMetadata,
    val sourceType: SourceType,
    val referrer: String,
    val locationHref: String? = null,
    val genericMetadata: GenericMetadata? = null
)

@Serializable
enum class EventType {
    Purchase,
    AddToCart,
    ProductView,
    PageView,
    UserIdentifierCollected,
    CartUpdated,
    SortAction,
    PageLeave,
    MobileCustomEvent,
    CartView,
    RemoveFromCart,
    CollectionView,
    SearchSubmit,
    CheckoutStarted,
    PaymentInfoSubmitted
}

@Serializable
enum class SourceType {
    mobile,
    shp_pixel,
    sst,
    ct
}

@Serializable
enum class AppSdk {
    iOS,
    Android,
    ReactNative
}
