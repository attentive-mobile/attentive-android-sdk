package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SortDirection {
    @SerialName("asc")
    ASC,
    @SerialName("desc")
    DESC
}
