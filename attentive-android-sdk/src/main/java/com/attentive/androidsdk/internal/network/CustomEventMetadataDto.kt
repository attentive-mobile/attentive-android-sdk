package com.attentive.androidsdk.internal.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

// Custom serializer to handle the special "properties" field
object PropertiesMapSerializer : KSerializer<Map<String, String>?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("PropertiesMap", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Map<String, String>?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            val json = Json{ignoreUnknownKeys = true}
            val jsonString = json.encodeToString(JsonElement.serializer(), Json.encodeToJsonElement(value))
            encoder.encodeString(jsonString)
        }
    }

    override fun deserialize(decoder: Decoder): Map<String, String>? {
        // We don't need to deserialize this field in this case, so we return null
        return null
    }
}

@Serializable
@Polymorphic
class CustomEventMetadataDto : Metadata() {
    var type: String? = null

    /**
     * We expect this map of properties to be a string containing a valid json map rather than a List object
     * For example:
     * Instead of this: {"properties": { "Color": "Blue" }}
     * We want this:    {"properties": "{ \"Color\": \"Blue\" }" }
     */
    @Serializable(with = PropertiesMapSerializer::class)
    var properties: Map<String, String>? = null
}