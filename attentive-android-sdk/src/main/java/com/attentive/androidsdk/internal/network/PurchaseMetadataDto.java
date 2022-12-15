package com.attentive.androidsdk.internal.network;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PurchaseMetadataDto extends Metadata {
    private String productId;
    private String subProductId;
    private String category;
    private String cartTotal;
    private String currency;
    private String image;
    private String name;
    private String orderId;
    private String price;
    private String quantity;
    private String cartId;
    private String cartCoupon;

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getSubProductId() {
        return subProductId;
    }

    public void setSubProductId(String subProductId) {
        this.subProductId = subProductId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
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
