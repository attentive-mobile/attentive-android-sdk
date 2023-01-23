package com.attentive.androidsdk.events;

import com.attentive.androidsdk.ParameterValidation;
import java.util.ArrayList;
import java.util.List;

public final class ProductViewEvent extends Event {
    private final List<Item> items;

    private ProductViewEvent(List<Item> items) {
        this.items = items;
    }

    public static final class Builder {
        private final List<Item> items;

        public Builder(List<Item> items) {
            ParameterValidation.verifyNotEmpty(items, "items");

            this.items = new ArrayList<>(items);
        }

        public ProductViewEvent build() {
            return new ProductViewEvent(this.items);
        }
    }

    public List<Item> getItems() {
        return items;
    }
}
