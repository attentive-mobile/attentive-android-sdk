package com.attentive.androidsdk.events;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.attentive.androidsdk.ParameterValidation;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.List;

@JsonDeserialize(builder = PurchaseEvent.Builder.class)
public final class PurchaseEvent extends Event {
    private final List<Item> items;
    private final Order order;
    private final Cart cart;

    private PurchaseEvent(Builder builder) {
        items = builder.items;
        order = builder.order;
        cart = builder.cart;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final List<Item> items;
        private final Order order;
        private Cart cart;

        @JsonCreator
        public Builder(@JsonProperty("items") List<Item> items, @JsonProperty("order") Order order) {
            ParameterValidation.verifyNotEmpty(items, "items");
            ParameterValidation.verifyNotNull(order, "order");

            this.items = items;
            this.order = order;
        }

        public Builder cart(Cart val) {
            cart = val;
            return this;
        }

        public PurchaseEvent build() {
            return new PurchaseEvent(this);
        }
    }

    @NonNull
    public List<Item> getItems() {
        return items;
    }

    @NonNull
    public Order getOrder() {
        return order;
    }

    @Nullable
    public Cart getCart() {
        return cart;
    }
}
