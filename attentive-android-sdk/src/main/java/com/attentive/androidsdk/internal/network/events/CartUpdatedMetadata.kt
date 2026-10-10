package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("CartUpdated")
data class CartUpdatedMetadata(
    val cart: Cart? = null,
    val products: List<Product>? = null,
    val currency: String? = null,
) : EventMetadata()
