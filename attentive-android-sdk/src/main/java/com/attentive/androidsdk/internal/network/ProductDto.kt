package com.attentive.androidsdk.internal.network

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
class ProductDto {
    var name: String? = null
    var quantity: String? = null
    var price: String? = null
    var productId: String? = null
    var subProductId: String? = null
    var category: String? = null
    var currency: String? = null
    var image: String? = null
}