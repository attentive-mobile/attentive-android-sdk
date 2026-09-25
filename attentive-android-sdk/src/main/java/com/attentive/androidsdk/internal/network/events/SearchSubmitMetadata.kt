package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("SearchSubmit")
data class SearchSubmitMetadata(
    val searchQuery: String? = null,
    val products: List<Product>? = null,
) : EventMetadata()
