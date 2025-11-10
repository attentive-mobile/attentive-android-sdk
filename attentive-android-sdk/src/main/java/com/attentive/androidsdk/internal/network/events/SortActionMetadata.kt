package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.Serializable

@Serializable
data class SortActionMetadata(
    val eventType: String = "SortAction",
    val sortBy: String? = null,
    val direction: SortDirection? = null
) : EventMetadata()
