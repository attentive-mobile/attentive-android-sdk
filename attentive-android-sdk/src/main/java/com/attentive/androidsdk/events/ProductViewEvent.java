package com.attentive.androidsdk.events;

import com.attentive.androidsdk.ParameterValidation;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.ArrayList;
import java.util.List;

@JsonDeserialize(builder = ProductViewEvent.Builder.class)
public final class ProductViewEvent extends Event {
    private final List<Item> items;

    private ProductViewEvent(List<Item> items) {
        this.items = items;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final List<Item> items;

        @JsonCreator
        public Builder(@JsonProperty("items") List<Item> items) {
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
