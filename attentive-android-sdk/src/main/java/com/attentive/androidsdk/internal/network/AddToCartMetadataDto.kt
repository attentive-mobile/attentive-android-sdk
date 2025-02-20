package com.attentive.androidsdk.internal.network

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

@Serializable
@Polymorphic
class AddToCartMetadataDto : ProductMetadata() {
    var quantity: String? = null
}
