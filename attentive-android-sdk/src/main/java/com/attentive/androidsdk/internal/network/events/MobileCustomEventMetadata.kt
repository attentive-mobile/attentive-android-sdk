package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.Serializable

@Serializable
data class MobileCustomEventMetadata(
    val eventType: String = "MobileCustomEvent",
    val customProperties: Map<String, String>? = null
) : EventMetadata()
