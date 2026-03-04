package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.Serializable

@Serializable
data class ProductViewMetadata(
    val eventType: String = "ProductView",
    val product: Product? = null,
    val currency: String? = null,
) : EventMetadata()
