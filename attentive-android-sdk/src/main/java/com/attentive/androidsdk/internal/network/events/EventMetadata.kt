package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class EventMetadata {
    abstract val eventType: String
}

@Serializable
@SerialName("Purchase")
data class PurchaseMetadata(
    override val eventType: String = "Purchase",
    val orderId: String? = null,
    val currency: String? = null,
    val orderTotal: String? = null,
    val cart: Cart? = null,
    val products: List<Product>? = null
) : EventMetadata()

@Serializable
@SerialName("AddToCart")
data class AddToCartMetadata(
    override val eventType: String = "AddToCart",
    val product: Product? = null,
    val currency: String? = null
) : EventMetadata()

@Serializable
@SerialName("ProductView")
data class ProductViewMetadata(
    override val eventType: String = "ProductView",
    val product: Product? = null,
    val currency: String? = null
) : EventMetadata()

@Serializable
@SerialName("PageView")
data class PageViewMetadata(
    override val eventType: String = "PageView"
) : EventMetadata()

@Serializable
@SerialName("UserIdentifierCollected")
data class UserIdentifierCollectedMetadata(
    override val eventType: String = "UserIdentifierCollected"
) : EventMetadata()

@Serializable
@SerialName("CartUpdated")
data class CartUpdatedMetadata(
    override val eventType: String = "CartUpdated",
    val cart: Cart? = null,
    val products: List<Product>? = null,
    val currency: String? = null
) : EventMetadata()

@Serializable
@SerialName("SortAction")
data class SortActionMetadata(
    override val eventType: String = "SortAction",
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
@SerialName("PageLeave")
data class PageLeaveMetadata(
    override val eventType: String = "PageLeave",
    val timeOnPage: Float? = null,
    val scrollDepth: Int? = null
) : EventMetadata()

@Serializable
@SerialName("MobileCustomEvent")
data class MobileCustomEventMetadata(
    override val eventType: String = "MobileCustomEvent",
    val customProperties: Map<String, String>? = null
) : EventMetadata()

@Serializable
@SerialName("CartView")
data class CartViewMetadata(
    override val eventType: String = "CartView",
    val cart: Cart? = null,
    val products: List<Product>? = null,
    val currency: String? = null
) : EventMetadata()

@Serializable
@SerialName("RemoveFromCart")
data class RemoveFromCartMetadata(
    override val eventType: String = "RemoveFromCart",
    val product: Product? = null
) : EventMetadata()

@Serializable
@SerialName("CollectionView")
data class CollectionViewMetadata(
    override val eventType: String = "CollectionView",
    val collectionId: String? = null,
    val collectionTitle: String? = null,
    val products: List<Product>? = null
) : EventMetadata()

@Serializable
@SerialName("SearchSubmit")
data class SearchSubmitMetadata(
    override val eventType: String = "SearchSubmit",
    val searchQuery: String? = null,
    val products: List<Product>? = null
) : EventMetadata()

@Serializable
@SerialName("CheckoutStarted")
data class CheckoutStartedMetadata(
    override val eventType: String = "CheckoutStarted",
    val orderId: String? = null,
    val currency: String? = null,
    val orderTotal: String? = null,
    val cart: Cart? = null,
    val products: List<Product>? = null
) : EventMetadata()

@Serializable
@SerialName("PaymentInfoSubmitted")
data class PaymentInfoSubmittedMetadata(
    override val eventType: String = "PaymentInfoSubmitted",
    val orderId: String? = null,
    val currency: String? = null,
    val orderTotal: String? = null,
    val cart: Cart? = null,
    val products: List<Product>? = null
) : EventMetadata()
