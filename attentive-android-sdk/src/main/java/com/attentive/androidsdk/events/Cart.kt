package com.attentive.androidsdk.events

import com.attentive.androidsdk.ParameterValidation
import kotlinx.serialization.Serializable

@Serializable
data class Cart(
    val cartId: String,
    val cartCoupon: String? = null,
) {
    init {
        ParameterValidation.verifyNotEmpty(cartId, "cartId")
    }

    @Serializable
    class Builder {
        lateinit var cartId: String
        private var cartCoupon: String? = null

        fun cartId(id: String): Builder {
            cartId = id
            return this
        }

        fun cartCoupon(id: String?): Builder {
            cartCoupon = id
            return this
        }

        fun build(): Cart {
            return Cart(
                cartId = cartId,
                cartCoupon = cartCoupon,
            )
        }
    }
}
