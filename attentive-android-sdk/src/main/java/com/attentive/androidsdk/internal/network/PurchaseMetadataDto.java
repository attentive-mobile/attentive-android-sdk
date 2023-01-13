package com.attentive.androidsdk.internal.network;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PurchaseMetadataDto extends ProductMetadata {
    private String quantity;
    private String orderId;
    private String cartTotal;
    private String cartId;
    private String cartCoupon;

    public String getCartTotal() {
        return cartTotal;
    }

    public void setCartTotal(String cartTotal) {
        this.cartTotal = cartTotal;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getCartId() {
        return cartId;
    }

    public void setCartId(String cartId) {
        this.cartId = cartId;
    }

    public String getCartCoupon() {
        return cartCoupon;
    }

    public void setCartCoupon(String cartCoupon) {
        this.cartCoupon = cartCoupon;
    }
}
