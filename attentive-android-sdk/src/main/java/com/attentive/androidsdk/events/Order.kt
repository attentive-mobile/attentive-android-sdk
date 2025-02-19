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
    class Builder(
        private val orderId: String
    ) {
        init {
            ParameterValidation.verifyNotEmpty(orderId, "orderId")
        }

        fun build(): Order {
            return Order(orderId)
        }
    }
}