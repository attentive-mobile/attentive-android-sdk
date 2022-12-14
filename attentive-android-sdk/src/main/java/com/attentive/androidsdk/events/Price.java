package com.attentive.androidsdk.events;

import androidx.annotation.NonNull;
import com.attentive.androidsdk.ParameterValidation;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

public class Price {
    private final BigDecimal price;
    private final Currency currency;

    private Price(Builder builder) {
        price = builder.price;
        currency = builder.currency;
    }

    public static final class Builder {
        private final BigDecimal price;
        private final Currency currency;

        public Builder(BigDecimal price, Currency currency) {
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
