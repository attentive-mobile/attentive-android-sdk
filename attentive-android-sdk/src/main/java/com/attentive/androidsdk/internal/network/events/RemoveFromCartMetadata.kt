package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("RemoveFromCart")
data class RemoveFromCartMetadata(
    val product: Product? = null,
) : EventMetadata()
