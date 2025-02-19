package com.attentive.androidsdk.internal.network

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
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

// Custom serializer to handle the special "products" field
object ProductListSerializer : KSerializer<List<ProductDto>?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ProductList", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: List<ProductDto>?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            val json = Json{ignoreUnknownKeys = true}
            val jsonString = json.encodeToString(JsonElement.serializer(), json.encodeToJsonElement(value))
            encoder.encodeString(jsonString)
        }
    }

    override fun deserialize(decoder: Decoder): List<ProductDto>? {
        // We don't need to deserialize this field in this case, so we return null
        return null
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Polymorphic
class OrderConfirmedMetadataDto : Metadata() {
    var orderId: String? = null
    var cartTotal: String? = null
    var currency: String? = null

    /**
     * We expect this list of products to be a string containing a valid json list rather than a List object
     * For example:
     * Instead of this: {"products": [ {"name": "T-shirt"}, {"name": "Shorts"} ]}
     * We want this: {"products": "[ {\"name\": \"T-shirt\"}, {\"name\": \"Shorts\"} ]"}
     */
    @Serializable(with = ProductListSerializer::class)
    @EncodeDefault
    var products: List<ProductDto>? = null
}