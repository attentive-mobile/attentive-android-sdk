package com.attentive.androidsdk.events

import com.attentive.androidsdk.ParameterValidation
import kotlinx.serialization.Serializable

/**
 * Cart state associated with a [PurchaseEvent].
 *
 * @property cartId Your canonical cart identifier. Required, non-empty.
 * @property cartCoupon A promotion code applied to the cart, if any.
 */
@Serializable
data class Cart(
    val cartId: String,
    val cartCoupon: String? = null,
) {
    init {
        ParameterValidation.verifyNotEmpty(cartId, "cartId")
    }

    /**
     * Builder for [Cart].
     */
    @Serializable
    class Builder {
        lateinit var cartId: String
        private var cartCoupon: String? = null

        /**
         * Sets the cart ID. Required.
         *
         * @param id A non-empty cart identifier.
         */
        fun cartId(id: String): Builder {
            cartId = id
            return this
        }

        /**
         * Sets an applied coupon code.
         *
         * @param id The coupon code, or `null`.
         */
        fun cartCoupon(id: String?): Builder {
            cartCoupon = id
            return this
        }

        /**
         * Builds the [Cart].
         *
         * @throws UninitializedPropertyAccessException if [cartId] was not set.
         */
        fun build(): Cart {
            return Cart(
                cartId = cartId,
                cartCoupon = cartCoupon,
            )
        }
    }
}
