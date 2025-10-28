package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class EventMetadata

@Serializable
data class PurchaseMetadata(
    val eventType: String = "Purchase",
    val orderId: String? = null,
    val currency: String? = null,
    val orderTotal: String? = null,
    val cart: Cart? = null,
    val products: List<Product>? = null
) : EventMetadata()

@Serializable
data class AddToCartMetadata(
    val eventType: String = "AddToCart",
    val product: Product? = null,
    val currency: String? = null
) : EventMetadata()

@Serializable
data class ProductViewMetadata(
    val eventType: String = "ProductView",
    val product: Product? = null,
    val currency: String? = null
) : EventMetadata()

@Serializable
data class PageViewMetadata(
    val eventType: String = "PageView"
) : EventMetadata()

@Serializable
data class UserIdentifierCollectedMetadata(
    val eventType: String = "UserIdentifierCollected"
) : EventMetadata()

@Serializable
data class CartUpdatedMetadata(
    val eventType: String = "CartUpdated",
    val cart: Cart? = null,
    val products: List<Product>? = null,
    val currency: String? = null
) : EventMetadata()

@Serializable
data class SortActionMetadata(
    val eventType: String = "SortAction",
    val sortBy: String? = null,
    val direction: SortDirection? = null
) : EventMetadata()

@Serializable
enum class SortDirection {
    @SerialName("asc")
    ASC,
    @SerialName("desc")
    DESC
}

@Serializable
data class PageLeaveMetadata(
    val eventType: String = "PageLeave",
    val timeOnPage: Float? = null,
    val scrollDepth: Int? = null
) : EventMetadata()

@Serializable
data class MobileCustomEventMetadata(
    val eventType: String = "MobileCustomEvent",
    val customProperties: Map<String, String>? = null
) : EventMetadata()

@Serializable
data class CartViewMetadata(
    val eventType: String = "CartView",
    val cart: Cart? = null,
    val products: List<Product>? = null,
    val currency: String? = null
) : EventMetadata()

@Serializable
data class RemoveFromCartMetadata(
    val eventType: String = "RemoveFromCart",
    val product: Product? = null
) : EventMetadata()

@Serializable
data class CollectionViewMetadata(
    val eventType: String = "CollectionView",
    val collectionId: String? = null,
    val collectionTitle: String? = null,
    val products: List<Product>? = null
) : EventMetadata()

@Serializable
data class SearchSubmitMetadata(
    val eventType: String = "SearchSubmit",
    val searchQuery: String? = null,
    val products: List<Product>? = null
) : EventMetadata()

@Serializable
data class CheckoutStartedMetadata(
    val eventType: String = "CheckoutStarted",
    val orderId: String? = null,
    val currency: String? = null,
    val orderTotal: String? = null,
    val cart: Cart? = null,
    val products: List<Product>? = null
) : EventMetadata()

@Serializable
data class PaymentInfoSubmittedMetadata(
    val eventType: String = "PaymentInfoSubmitted",
    val orderId: String? = null,
    val currency: String? = null,
    val orderTotal: String? = null,
    val cart: Cart? = null,
    val products: List<Product>? = null
) : EventMetadata()
