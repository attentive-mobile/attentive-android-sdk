package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.Serializable

@Serializable
data class SearchSubmitMetadata(
    val eventType: String = "SearchSubmit",
    val searchQuery: String? = null,
    val products: List<Product>? = null,
) : EventMetadata()
