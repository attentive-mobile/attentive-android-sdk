package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.Serializable

@Serializable
data class GenericMetadata(
    val identity: Map<String, String>? = null,
    val productCatalog: Map<String, String>? = null,
)
