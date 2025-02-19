package com.attentive.androidsdk.internal.network

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

@Serializable
@Polymorphic
open class ProductMetadata : Metadata() {
    var productId: String? = null

    var subProductId: String? = null

    var category: String? = null

    open var currency: String? = null

    var image: String? = null

    var name: String? = null
    open var orderId: String? = null

    var price: String? = null
}
