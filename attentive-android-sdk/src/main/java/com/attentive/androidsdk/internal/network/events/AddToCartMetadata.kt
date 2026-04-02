package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.Serializable

@Serializable
data class AddToCartMetadata(
    val eventType: String = "AddToCart",
    val product: Product? = null,
    val currency: String? = null,
) : EventMetadata()
