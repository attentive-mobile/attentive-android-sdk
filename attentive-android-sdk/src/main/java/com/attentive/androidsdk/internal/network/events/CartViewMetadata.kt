package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("CartView")
data class CartViewMetadata(
    val cart: Cart? = null,
    val products: List<Product>? = null,
    val currency: String? = null,
) : EventMetadata()
