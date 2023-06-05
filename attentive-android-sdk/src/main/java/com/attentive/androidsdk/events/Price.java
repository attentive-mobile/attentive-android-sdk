package com.attentive.androidsdk.events;

import androidx.annotation.NonNull;
import com.attentive.androidsdk.ParameterValidation;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

@JsonDeserialize(builder = Price.Builder.class)
public class Price {
    private final BigDecimal price;
    private final Currency currency;

    private Price(Builder builder) {
        price = builder.price;
        currency = builder.currency;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final BigDecimal price;
        private final Currency currency;

        @JsonCreator
        public Builder(@JsonProperty("price") BigDecimal price, @JsonProperty("currency") Currency currency) {
            ParameterValidation.verifyNotNull(price, "price");
            ParameterValidation.verifyNotNull(currency, "currency");

            this.price = price.setScale(2, RoundingMode.DOWN);
            this.currency = currency;
        }

        public Price build() {
            return new Price(this);
        }
    }

    @NonNull
    public BigDecimal getPrice() {
        return price;
    }

    @NonNull
    public Currency getCurrency() {
        return currency;
    }
}
