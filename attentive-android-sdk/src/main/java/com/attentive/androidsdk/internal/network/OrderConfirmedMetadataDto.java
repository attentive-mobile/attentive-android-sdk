package com.attentive.androidsdk.internal.network;

import com.attentive.androidsdk.internal.util.ObjectToRawJsonStringConverter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OrderConfirmedMetadataDto extends Metadata {
    private String orderId;
    private String cartTotal;
    private String currency;

    @JsonSerialize(converter = ObjectToRawJsonStringConverter.class)
    private List<ProductDto> products;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCartTotal() {
        return cartTotal;
    }

    public void setCartTotal(String cartTotal) {
        this.cartTotal = cartTotal;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<ProductDto> getProducts() {
        return products;
    }

    public void setProducts(List<ProductDto> productDtos) {
        this.products = productDtos;
    }
}
