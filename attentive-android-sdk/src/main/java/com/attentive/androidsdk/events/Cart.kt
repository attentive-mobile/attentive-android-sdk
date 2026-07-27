package com.attentive.androidsdk.events

import com.attentive.androidsdk.ParameterValidation
import kotlinx.serialization.Serializable

/**
 * Cart state associated with a [PurchaseEvent].
 *
 * @property cartId Your canonical cart identifier. Required, non-empty.
 * @property cartCoupon A promotion code applied to the cart, if any.
 * @property cartTotal The cart total as a string (e.g. "42.00"). When null on v2 events,
 * the SDK computes a fallback from the item prices.
 * @property cartDiscount The cart discount amount as a string, if any.
 */
@Serializable
data class Cart(
    val cartId: String,
    val cartCoupon: String? = null,
    val cartTotal: String? = null,
    val cartDiscount: String? = null,
) {
    init {
        ParameterValidation.verifyNotEmpty(cartId, "cartId")
    }

    @Serializable
    class Builder {
        lateinit var cartId: String
        private var cartCoupon: String? = null
        private var cartTotal: String? = null
        private var cartDiscount: String? = null

        fun cartId(id: String): Builder {
            cartId = id
            return this
        }

        fun cartCoupon(id: String?): Builder {
            cartCoupon = id
            return this
        }

        fun cartTotal(total: String?): Builder {
            cartTotal = total
            return this
        }

        fun cartDiscount(discount: String?): Builder {
            cartDiscount = discount
            return this
        }

        /**
         * @throws UninitializedPropertyAccessException if [cartId] was not set.
         */
        fun build(): Cart {
            return Cart(
                cartId = cartId,
                cartCoupon = cartCoupon,
                cartTotal = cartTotal,
                cartDiscount = cartDiscount,
            )
        }
    }
}
