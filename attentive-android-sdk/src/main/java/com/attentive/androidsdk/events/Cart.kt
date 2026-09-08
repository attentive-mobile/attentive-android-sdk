package com.attentive.androidsdk.events

import kotlinx.serialization.Serializable

/**
 * Cart state associated with a [PurchaseEvent].
 *
 * Every field is optional, matching `ATTNCart` on iOS and the `Cart` schema the events backend
 * accepts (which declares no required properties). A cart with no ID still carries useful
 * coupon/total/discount context, so an omitted [cartId] is passed through rather than rejected.
 *
 * @property cartId Your canonical cart identifier, if you have one.
 * @property cartCoupon A promotion code applied to the cart, if any.
 * @property cartTotal The cart total as a string (e.g. "42.00"). When null on v2 events,
 * the SDK computes a fallback from the item prices.
 * @property cartDiscount The cart discount amount as a string, if any.
 */
@Serializable
data class Cart(
    val cartId: String? = null,
    val cartCoupon: String? = null,
    val cartTotal: String? = null,
    val cartDiscount: String? = null,
) {
    @Serializable
    class Builder {
        var cartId: String? = null
        private var cartCoupon: String? = null
        private var cartTotal: String? = null
        private var cartDiscount: String? = null

        fun cartId(id: String?): Builder {
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
