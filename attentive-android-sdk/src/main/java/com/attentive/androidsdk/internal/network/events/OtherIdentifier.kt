package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.Serializable

@Serializable
data class OtherIdentifier(
    val idType: IdType,
    val value: String,
    val name: String? = null,
)
