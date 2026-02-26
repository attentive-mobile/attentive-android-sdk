package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.Serializable

@Serializable
data class Cart(
    val cartTotal: String? = null,
    val cartCoupon: String? = null,
    val cartDiscount: String? = null,
    val cartId: String? = null,
)
