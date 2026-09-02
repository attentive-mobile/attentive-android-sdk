package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("SortAction")
data class SortActionMetadata(
    val sortBy: String? = null,
    val direction: SortDirection? = null,
) : EventMetadata()
