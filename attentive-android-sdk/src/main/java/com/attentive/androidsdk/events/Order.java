package com.attentive.androidsdk.events;

import androidx.annotation.NonNull;
import com.attentive.androidsdk.ParameterValidation;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = Order.Builder.class)
public class Order {
    private final String orderId;

    private Order(Builder builder) {
        orderId = builder.orderId;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final String orderId;

        @JsonCreator
        public Builder(@JsonProperty("orderId") String orderId) {
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
