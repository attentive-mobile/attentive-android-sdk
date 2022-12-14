package com.attentive.example.activities;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

import com.attentive.androidsdk.AttentiveEventTracker;
import com.attentive.androidsdk.events.Cart;
import com.attentive.androidsdk.events.Item;
import com.attentive.androidsdk.events.Order;
import com.attentive.androidsdk.events.Price;
import com.attentive.androidsdk.events.PurchaseEvent;
import com.attentive.example.R;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

public class ProductPageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_page);
        // TODO send Product View event here
    }

    public void purchaseButtonClicked(View view) {
        // Send "Purchase" Event

        // Construct one or more "Item"s, which represents the product(s) purchased
        final String productId = "11111";
        final String productVariantId = "222";
        final Price price = new Price.Builder(new BigDecimal("19.99"), Currency.getInstance("USD")).build();
        final Item item = new Item.Builder(productId, productVariantId, price).quantity(1).build();

        // Construct an "Order", which represents the order for the purchase
        final String orderId = "23456";
        final Order order = new Order.Builder(orderId).build();

        // (Optional) Construct a "Cart", which represents the cart this Purchase was made from
        final Cart cart = new Cart.Builder().cartId("7878").cartCoupon("SomeCoupon").build();

        // Construct a PurchaseEvent, which ties together the preceding objects
        PurchaseEvent purchaseEvent = new PurchaseEvent.Builder(List.of(item), order).cart(cart).build();

        // Record the PurchaseEvent
        AttentiveEventTracker.getInstance().recordEvent(purchaseEvent);
    }
}