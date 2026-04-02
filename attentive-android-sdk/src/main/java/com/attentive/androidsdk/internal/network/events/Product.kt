package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.Serializable

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
    val productUrl: String? = null,
)
