package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The `eventType` discriminator this serializes with is always `MobileCustomEvent` - it identifies
 * the metadata shape, *not* the host's event name.
 *
 * @property type The host-supplied custom event name (`CustomEvent.type`), which the legacy `/e`
 * path sends as the `type` metadata entry. Matches `ATTNMobileCustomEventMetadata.type` on iOS.
 */
@Serializable
@SerialName("MobileCustomEvent")
data class MobileCustomEventMetadata(
    val type: String? = null,
    val customProperties: Map<String, String>? = null,
) : EventMetadata()
