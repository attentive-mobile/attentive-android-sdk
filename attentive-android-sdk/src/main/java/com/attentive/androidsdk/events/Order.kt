package com.attentive.androidsdk.events

import com.attentive.androidsdk.ParameterValidation
import kotlinx.serialization.Serializable

/**
 * Order identifier attached to a [PurchaseEvent].
 *
 * @property orderId Your canonical order identifier. Required, non-empty.
 */
@Serializable
data class Order(
    val orderId: String,
) : Event() {
    init {
        ParameterValidation.verifyNotEmpty(orderId, "orderId")
    }

    /**
     * Builder for [Order].
     */
    @Serializable
    class Builder {
        private lateinit var orderId: String

        /**
         * Sets the order ID. Required.
         *
         * @param orderId A non-empty order identifier.
         */
        fun orderId(orderId: String): Builder {
            this.orderId = orderId
            return this
        }

        /**
         * Builds the [Order].
         *
         * @throws UninitializedPropertyAccessException if [orderId] was not set.
         */
        fun build(): Order {
            return Order(orderId)
        }
    }
}
