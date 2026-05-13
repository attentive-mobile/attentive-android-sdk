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

/** Encodes [BigDecimal] as its plain-string representation. */
object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: BigDecimal,
    ) {
        encoder.encodeString(value.toPlainString())
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        return BigDecimal(decoder.decodeString())
    }
}

/** Encodes [Currency] as its ISO 4217 currency code. */
object CurrencySerializer : KSerializer<Currency> {
// Custom serializer for Currency
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Currency", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: Currency,
    ) {
        encoder.encodeString(value.currencyCode)
    }

    override fun deserialize(decoder: Decoder): Currency {
        return Currency.getInstance(decoder.decodeString())
    }
}

/**
 * A monetary price: amount + currency. The amount is normalized to two decimal places,
 * rounded down, at construction.
 *
 * @property price The price amount. Rounded to 2 decimal places with [RoundingMode.DOWN] at init.
 * @property currency The currency.
 */
@Serializable
data class Price(
    @Serializable(with = BigDecimalSerializer::class)
    var price: BigDecimal,
    @Serializable(with = CurrencySerializer::class)
    val currency: Currency,
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

        /**
         * @throws IllegalArgumentException if [price] or [currency] was not set.
         */
        fun build(): Price {
            val price = this.price ?: throw IllegalArgumentException("Price must not be null")
            val currency = this.currency ?: throw IllegalArgumentException("Currency must not be null")
            return Price(price, currency)
        }
    }
}
