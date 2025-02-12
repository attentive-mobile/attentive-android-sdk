package com.attentive.androidsdk.internal.network

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_EMPTY)
class PurchaseMetadataDto : ProductMetadata() {
    @JvmField
    var quantity: String? = null
    override var orderId: String? = null
    var cartTotal: String? = null
    @JvmField
    var cartId: String? = null
    @JvmField
    var cartCoupon: String? = null
}
