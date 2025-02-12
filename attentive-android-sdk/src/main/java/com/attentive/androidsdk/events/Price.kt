package com.attentive.androidsdk.events

import com.attentive.androidsdk.ParameterValidation
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency

@JsonDeserialize(builder = Price.Builder::class)
class Price private constructor(builder: Builder) {
    @JvmField
    val price: BigDecimal
    @JvmField
    val currency: Currency

    init {
        price = builder.price
        currency = builder.currency
    }

    @JsonPOJOBuilder(withPrefix = "")
    class Builder @JsonCreator constructor(
        @JsonProperty("price") price: BigDecimal,
        @JsonProperty("currency") currency: Currency
    ) {
        val price: BigDecimal
        val currency: Currency

        init {
            ParameterValidation.verifyNotNull(price, "price")
            ParameterValidation.verifyNotNull(currency, "currency")

            this.price = price.setScale(2, RoundingMode.DOWN)
            this.currency = currency
        }

        fun build(): Price {
            return Price(this)
        }
    }
}
