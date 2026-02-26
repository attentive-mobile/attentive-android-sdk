package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.Serializable

@Serializable
data class PurchaseMetadata(
    val eventType: String = "Purchase",
    val orderId: String? = null,
    val currency: String? = null,
    val orderTotal: String? = null,
    val cart: Cart? = null,
    val products: List<Product>? = null,
) : EventMetadata()
