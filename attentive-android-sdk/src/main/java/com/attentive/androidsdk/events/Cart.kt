package com.attentive.androidsdk.events

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder

@JsonDeserialize(builder = Cart.Builder::class)
class Cart private constructor(builder: Builder) {
    @JvmField
    val cartId: String?
    @JvmField
    val cartCoupon: String?

    init {
        cartId = builder.cartId
        cartCoupon = builder.cartCoupon
    }

    @JsonPOJOBuilder(withPrefix = "")
    class Builder {
        var cartId: String? = null
        var cartCoupon: String? = null

        fun cartId(cartId: String?): Builder {
            this.cartId = cartId
            return this
        }

        fun cartCoupon(cartCoupon: String?): Builder {
            this.cartCoupon = cartCoupon
            return this
        }

        fun build(): Cart {
            return Cart(this)
        }
    }
}
