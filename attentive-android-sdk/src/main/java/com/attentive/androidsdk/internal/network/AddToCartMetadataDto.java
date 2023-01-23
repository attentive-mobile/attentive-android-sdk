package com.attentive.androidsdk.internal.network;

public class AddToCartMetadataDto extends ProductMetadata {
    private String quantity;

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }
}
