package com.attentive.androidsdk.internal.network

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_EMPTY)
class ProductDto {
    var name: String? = null
    var quantity: String? = null
    @JvmField
    var price: String? = null
    @JvmField
    var productId: String? = null
    @JvmField
    var subProductId: String? = null
    @JvmField
    var category: String? = null
    var currency: String? = null
    @JvmField
    var image: String? = null
}
