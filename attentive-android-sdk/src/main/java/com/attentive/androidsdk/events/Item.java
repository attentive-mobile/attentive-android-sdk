package com.attentive.androidsdk.events;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.attentive.androidsdk.ParameterValidation;

public class Item {
    private final String productId;
    private final String productVariantId;
    private final String productImage;
    private final String name;
    private final Price price;
    private final int quantity;
    private final String category;

    private Item(Builder builder) {
        productId = builder.productId;
        productVariantId = builder.productVariantId;
        price = builder.price;
        productImage = builder.productImage;
        name = builder.name;
        quantity = builder.quantity;
        category = builder.category;
    }

    public static final class Builder {
        private final String productId;
        private final String productVariantId;
        private final Price price;
        private String productImage;
        private String name;
        private int quantity = 1;
        private String category;

        public Builder(String productId, String productVariantId, Price price) {
            ParameterValidation.verifyNotEmpty(productId, "productId");
            ParameterValidation.verifyNotEmpty(productVariantId, "productVariantId");
            ParameterValidation.verifyNotNull(price, "price");

            this.productId = productId;
            this.productVariantId = productVariantId;
            this.price = price;
        }

        public Builder productImage(String val) {
            productImage = val;
            return this;
        }

        public Builder name(String val) {
            name = val;
            return this;
        }

        public Builder quantity(int val) {
            quantity = val;
            return this;
        }

        public Builder category(String val) {
            category = val;
            return this;
        }

        public Item build() {
            return new Item(this);
        }
    }

    @NonNull
    public String getProductId() {
        return productId;
    }

    @NonNull
    public String getProductVariantId() {
        return productVariantId;
    }

    @NonNull
    public Price getPrice() {
        return price;
    }

    @Nullable
    public String getProductImage() {
        return productImage;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public int getQuantity() {
        return quantity;
    }

    @Nullable
    public String getCategory() {
        return category;
    }
}
