package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.Serializable

@Serializable
data class UserIdentifierCollectedMetadata(
    val eventType: String = "UserIdentifierCollected",
) : EventMetadata()
