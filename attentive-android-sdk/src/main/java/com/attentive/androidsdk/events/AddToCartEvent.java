package com.attentive.androidsdk.events;

import com.attentive.androidsdk.ParameterValidation;
import java.util.List;

public final class AddToCartEvent extends Event {
    private final List<Item> items;

    public AddToCartEvent(List<Item> items) {
        this.items = items;
    }

    public static class Builder {
        private final List<Item> items;

        public Builder(List<Item> items) {
            ParameterValidation.verifyNotEmpty(items, "items");
            this.items = items;
        }

        public AddToCartEvent build() {
            return new AddToCartEvent(items);
        }
    }

    public List<Item> getItems() {
        return items;
    }
}
