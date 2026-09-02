package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("PageLeave")
data class PageLeaveMetadata(
    val timeOnPage: Float? = null,
    val scrollDepth: Int? = null,
) : EventMetadata()
