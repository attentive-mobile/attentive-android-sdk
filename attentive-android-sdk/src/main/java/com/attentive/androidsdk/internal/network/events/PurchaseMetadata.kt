package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Purchase")
data class PurchaseMetadata(
    val orderId: String? = null,
    val currency: String? = null,
    val orderTotal: String? = null,
    val cart: Cart? = null,
    val products: List<Product>? = null,
) : EventMetadata()
