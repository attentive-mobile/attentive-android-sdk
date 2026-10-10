package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("AddToCart")
data class AddToCartMetadata(
    val product: Product? = null,
    val currency: String? = null,
) : EventMetadata()
