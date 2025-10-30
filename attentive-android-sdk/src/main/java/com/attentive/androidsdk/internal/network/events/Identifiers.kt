package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.Serializable

@Serializable
data class Identifiers(
    val encryptedEmail: String? = null,
    val encryptedPhone: String? = null,
    val otherIdentifiers: List<OtherIdentifier>? = null
)
