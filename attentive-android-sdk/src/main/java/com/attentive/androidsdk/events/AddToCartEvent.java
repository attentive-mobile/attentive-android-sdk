package com.attentive.androidsdk.events;

import com.attentive.androidsdk.ParameterValidation;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.List;

@JsonDeserialize(builder = AddToCartEvent.Builder.class)
public final class AddToCartEvent extends Event {
    private final List<Item> items;

    public AddToCartEvent(List<Item> items) {
        this.items = items;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final List<Item> items;

        @JsonCreator
        public Builder(@JsonProperty("items") List<Item> items) {
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
