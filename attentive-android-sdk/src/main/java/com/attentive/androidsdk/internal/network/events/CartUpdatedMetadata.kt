package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.Serializable

@Serializable
data class CartUpdatedMetadata(
    val eventType: String = "CartUpdated",
    val cart: Cart? = null,
    val products: List<Product>? = null,
    val currency: String? = null
) : EventMetadata()
