package com.attentive.androidsdk.internal.network

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Polymorphic
class PurchaseMetadataDto : ProductMetadata() {
    var quantity: String? = null
    var cartTotal: String? = null
    var cartId: String? = null
    var cartCoupon: String? = null
}
