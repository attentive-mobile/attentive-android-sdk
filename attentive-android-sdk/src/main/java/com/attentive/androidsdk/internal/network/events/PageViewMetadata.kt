package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.Serializable

@Serializable
data class PageViewMetadata(
    val eventType: String = "PageView"
) : EventMetadata()
