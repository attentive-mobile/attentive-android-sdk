package com.attentive.androidsdk.events

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency

object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeString(value.toPlainString())
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        return BigDecimal(decoder.decodeString())
    }
}

object CurrencySerializer : KSerializer<Currency> {
// Custom serializer for Currency
override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("Currency", PrimitiveKind.STRING)

override fun serialize(encoder: Encoder, value: Currency) {
    encoder.encodeString(value.currencyCode)
}

override fun deserialize(decoder: Decoder): Currency {
    return Currency.getInstance(decoder.decodeString())
}
}

@Serializable
data class Price(
    @Serializable(with = BigDecimalSerializer::class)
    var price: BigDecimal,
    @Serializable(with = CurrencySerializer::class)
    val currency: Currency
) {
    init {
        this.price = this.price.setScale(2, RoundingMode.DOWN)
    }

    @Serializable
    class Builder {
        @Serializable(with = BigDecimalSerializer::class)
        var price: BigDecimal? = null
        @Serializable(with = CurrencySerializer::class)
        var currency: Currency? = null

        fun price(price: BigDecimal): Builder {
            this.price = price
            return this
        }

        fun currency(currency: Currency): Builder {
            this.currency = currency
            return this
        }

        fun build(): Price {
            val price = this.price ?: throw IllegalArgumentException("Price must not be null")
            val currency = this.currency ?: throw IllegalArgumentException("Currency must not be null")
            return Price(price, currency)
        }
    }
}