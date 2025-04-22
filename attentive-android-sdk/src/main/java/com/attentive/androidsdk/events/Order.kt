package com.attentive.androidsdk.events

import com.attentive.androidsdk.ParameterValidation
import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val orderId: String
) : Event() {
    init {
        ParameterValidation.verifyNotEmpty(orderId, "orderId")
    }

    @Serializable
    class Builder {
        private lateinit var orderId: String
        fun orderId(orderId: String): Builder {
            this.orderId = orderId
            return this
        }

        fun build(): Order {
            return Order(orderId)
        }
    }
}