package com.attentive.androidsdk.internal.network

open class ProductMetadata : Metadata() {
    @JvmField
    var productId: String? = null
    @JvmField
    var subProductId: String? = null
    @JvmField
    var category: String? = null
    @JvmField
    var currency: String? = null
    @JvmField
    var image: String? = null
    @JvmField
    var name: String? = null
    open var orderId: String? = null
    @JvmField
    var price: String? = null
}
