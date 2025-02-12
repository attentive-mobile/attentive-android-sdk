package com.attentive.androidsdk.internal.network

import com.attentive.androidsdk.internal.util.ObjectToRawJsonStringConverter
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonSerialize

@JsonInclude(JsonInclude.Include.NON_EMPTY)
class OrderConfirmedMetadataDto : Metadata() {
    var orderId: String? = null
    var cartTotal: String? = null
    var currency: String? = null

    /**
     * We expect this list of products to be a string containing a valid json list rather than a List object
     * For example:
     * Instead of this: {"products": [ {"name": "T-shirt"}, {"name": "Shorts"} ]}
     * We want this: {"products": "[ {\"name\": \"T-shirt\"}, {\"name\": \"Shorts\"} ]"}
     */
    @JsonSerialize(converter = ObjectToRawJsonStringConverter::class)
    var products: List<ProductDto>? = null
}
