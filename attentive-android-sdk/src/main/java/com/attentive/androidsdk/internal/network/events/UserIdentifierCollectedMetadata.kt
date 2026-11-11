package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("UserIdentifierCollected")
data object UserIdentifierCollectedMetadata : EventMetadata()
