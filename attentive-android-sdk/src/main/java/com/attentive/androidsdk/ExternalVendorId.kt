package com.attentive.androidsdk

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// Custom serializer for Vendor enum
object VendorSerializer : KSerializer<ExternalVendorId.Vendor> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Vendor", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: ExternalVendorId.Vendor,
    ) {
        encoder.encodeString(value.vendorId)
    }

    override fun deserialize(decoder: Decoder): ExternalVendorId.Vendor {
        val vendorId = decoder.decodeString()
        return ExternalVendorId.Vendor.values().firstOrNull { it.vendorId == vendorId }
            ?: throw IllegalArgumentException("Unknown Vendor ID: $vendorId")
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
open class ExternalVendorId {
    var vendor: Vendor? = null
    var id: String? = null
    var name: String? = null

    @Serializable(with = VendorSerializer::class)
    enum class Vendor(val vendorId: String) {
        SHOPIFY("0"),
        KLAVIYO("1"),
        CLIENT_USER("2"),
        CUSTOM_USER("6"),
    }
}
