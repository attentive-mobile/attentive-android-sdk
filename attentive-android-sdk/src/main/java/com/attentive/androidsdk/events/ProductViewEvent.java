package com.attentive.androidsdk.events;

import com.attentive.androidsdk.ParameterValidation;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@JsonDeserialize(builder = ProductViewEvent.Builder.class)
public final class ProductViewEvent extends Event {
    @NotNull
    private final List<Item> items;
    @Nullable
    private final String deeplink;

    private ProductViewEvent(@NotNull List<Item> items) {
        this.items = items;
        this.deeplink = null;
    }

    private ProductViewEvent(@NotNull Builder builder) {
        this.items = builder.items;
        this.deeplink = builder.deeplink;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        @NotNull
        private List<Item> items = new ArrayList<>();
        @Nullable
        private String deeplink;

        /**
         * @deprecated As of release 1.0.0-beta01, replaced by standard builder methods
         */
        @Deprecated(since = "1.0.0-beta01", forRemoval = true)
        @JsonCreator
        public Builder(@JsonProperty("items") List<Item> items) {
            ParameterValidation.verifyNotEmpty(items, "items");

            this.items = new ArrayList<>(items);
            this.deeplink = null;
        }

        public Builder() {}

        public Builder items(@NotNull List<Item> items) {
            this.items = items;
            return this;
        }

        public Builder deeplink(@Nullable String deeplink) {
            this.deeplink = deeplink;
            return this;
        }

        public ProductViewEvent buildIt() {
            return new ProductViewEvent(this);
        }

        /**
         * @deprecated As of release 1.0.0-beta01, replaced by {@link ProductViewEvent.Builder#buildIt}
         * Only to be used if using the new builder approach not the deprecated one.
         */
        @Deprecated(since = "1.0.0-beta01", forRemoval = true)
        public ProductViewEvent build() {
            return new ProductViewEvent(this.items);
        }
    }

    @NotNull
    public List<Item> getItems() {
        return items;
    }

    @Nullable
    public String getDeeplink() {
        return deeplink;
    }
}
