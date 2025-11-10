package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.Serializable

@Serializable
data class PageLeaveMetadata(
    val eventType: String = "PageLeave",
    val timeOnPage: Float? = null,
    val scrollDepth: Int? = null
) : EventMetadata()
