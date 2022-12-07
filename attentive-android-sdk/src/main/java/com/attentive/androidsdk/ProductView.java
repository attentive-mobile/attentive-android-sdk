package com.attentive.androidsdk;

import java.util.List;

public class ProductView extends Event {
    private List<Product> products;

    public List<Product> getItems() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }
}
