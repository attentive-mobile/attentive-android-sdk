package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.Serializable

@Serializable
data class RemoveFromCartMetadata(
    val eventType: String = "RemoveFromCart",
    val product: Product? = null
) : EventMetadata()
