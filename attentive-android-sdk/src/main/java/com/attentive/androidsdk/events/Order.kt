package com.attentive.androidsdk.events

import com.attentive.androidsdk.ParameterValidation
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder

@JsonDeserialize(builder = Order.Builder::class)
class Order private constructor(builder: Builder) {
    @JvmField
    val orderId: String

    init {
        orderId = builder.orderId
    }

    @JsonPOJOBuilder(withPrefix = "")
    class Builder @JsonCreator constructor(@JsonProperty("orderId") orderId: String) {
        val orderId: String

        init {
            ParameterValidation.verifyNotEmpty(orderId, "orderId")

            this.orderId = orderId
        }

        fun build(): Order {
            return Order(this)
        }
    }
}
