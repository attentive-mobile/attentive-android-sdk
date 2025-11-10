package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.Serializable

@Serializable
data class CartViewMetadata(
    val eventType: String = "CartView",
    val cart: Cart? = null,
    val products: List<Product>? = null,
    val currency: String? = null
) : EventMetadata()
