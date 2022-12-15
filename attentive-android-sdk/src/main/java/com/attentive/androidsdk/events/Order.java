package com.attentive.androidsdk.events;

import androidx.annotation.NonNull;
import com.attentive.androidsdk.ParameterValidation;

public class Order {
    private final String orderId;

    private Order(Builder builder) {
        orderId = builder.orderId;
    }

    public static final class Builder {
        private final String orderId;

        public Builder(String orderId) {
            ParameterValidation.verifyNotEmpty(orderId, "orderId");

            this.orderId = orderId;
        }

        public Order build() {
            return new Order(this);
        }
    }

    @NonNull
    public String getOrderId() {
        return orderId;
    }
}
