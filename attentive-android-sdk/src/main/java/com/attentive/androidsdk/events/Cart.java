package com.attentive.androidsdk.events;

import androidx.annotation.Nullable;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;


@JsonDeserialize(builder = Cart.Builder.class)
public class Cart {
    private final String cartId;
    private final String cartCoupon;

    private Cart(Builder builder) {
        cartId = builder.cartId;
        cartCoupon = builder.cartCoupon;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private String cartId;
        private String cartCoupon;

        public Builder cartId(String cartId) {
            this.cartId = cartId;
            return this;
        }

        public Builder cartCoupon(String cartCoupon) {
            this.cartCoupon = cartCoupon;
            return this;
        }

        public Cart build() {
            return new Cart(this);
        }
    }

    @Nullable
    public String getCartId() {
        return cartId;
    }

    @Nullable
    public String getCartCoupon() {
        return cartCoupon;
    }
}
