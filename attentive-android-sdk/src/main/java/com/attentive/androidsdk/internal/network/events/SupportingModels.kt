package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.Serializable

@Serializable
data class Identifiers(
    val encryptedEmail: String? = null,
    val encryptedPhone: String? = null,
    val otherIdentifiers: List<OtherIdentifier>? = null
)

@Serializable
data class OtherIdentifier(
    val idType: IdType,
    val value: String,
    val name: String? = null
)

@Serializable
enum class IdType {
    ShopifyId,
    KlaviyoId,
    ClientUserId,
    CustomId,
    ShopifyY,
    BluecoreId,
    SalesforceId,
    EdgetagId,
    SalesforceMcId,
    CordialId,
    GenericClickId,
    AttnEatId
}

@Serializable
data class Cart(
    val cartTotal: String? = null,
    val cartCoupon: String? = null,
    val cartDiscount: String? = null,
    val cartId: String? = null
)

@Serializable
data class Product(
    val productId: String? = null,
    val variantId: String? = null,
    val name: String? = null,
    val variantName: String? = null,
    val imageUrl: String? = null,
    val categories: List<String>? = null,
    val price: String? = null,
    val quantity: Int? = null,
    val productUrl: String? = null
)

@Serializable
data class GenericMetadata(
    val identity: Map<String, String>? = null,
    val productCatalog: Map<String, String>? = null
)
