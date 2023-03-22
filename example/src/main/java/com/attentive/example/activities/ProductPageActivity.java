package com.attentive.example.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.attentive.androidsdk.AttentiveEventTracker;
import com.attentive.androidsdk.events.AddToCartEvent;
import com.attentive.androidsdk.events.Cart;
import com.attentive.androidsdk.events.CustomEvent;
import com.attentive.androidsdk.events.Item;
import com.attentive.androidsdk.events.Order;
import com.attentive.androidsdk.events.Price;
import com.attentive.androidsdk.events.ProductViewEvent;
import com.attentive.androidsdk.events.Purchase;
import com.attentive.androidsdk.events.PurchaseEvent;
import com.attentive.example.R;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Map;

public class ProductPageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_page);

        // Send "Product View" Event
        final Item item = createItem();
        final ProductViewEvent productViewEvent = new ProductViewEvent.Builder(List.of(item)).build();
        AttentiveEventTracker.getInstance().recordEvent(productViewEvent);
        showToastMessageForEvent("Product View");
    }

    public void addToCartButtonClicked(View view) {
        // Send "Add to Cart" Event
        final Item item = createItem();
        final AddToCartEvent addToCartEvent=  new AddToCartEvent.Builder(List.of(item)).build();
        AttentiveEventTracker.getInstance().recordEvent(addToCartEvent);
        showToastMessageForEvent("Add to Cart");
    }

    public void purchaseButtonClicked(View view) {
        // Send "Purchase" Event

        // Construct one or more "Item"s, which represents the product(s) purchased
        final Item item = createItem();

        // Construct an "Order", which represents the order for the purchase
        final String orderId = "23456";
        final Order order = new Order.Builder(orderId).build();

        // (Optional) Construct a "Cart", which represents the cart this Purchase was made from
        final Cart cart = new Cart.Builder().cartId("7878").cartCoupon("SomeCoupon").build();

        // Construct a PurchaseEvent, which ties together the preceding objects
        PurchaseEvent purchaseEvent = new PurchaseEvent.Builder(List.of(item), order).cart(cart).build();

        // Record the PurchaseEvent
        AttentiveEventTracker.getInstance().recordEvent(purchaseEvent);
        showToastMessageForEvent("Purchase");
    }

    public void customEventButtonClicked(View view) {
        CustomEvent customEvent = new CustomEvent.Builder("Concert Viewed", Map.of("band", "The Beatles")).build();

        AttentiveEventTracker.getInstance().recordEvent(customEvent);
        showToastMessageForEvent("Custom Event");
    }

    @NonNull
    private Item createItem() {
        final String productId = "11111";
        final String productVariantId = "222";
        final Price price = new Price.Builder(new BigDecimal("19.99"), Currency.getInstance("USD")).build();
        return new Item.Builder(productId, productVariantId, price).quantity(1).build();
    }

    private void showToastMessageForEvent(String eventName) {
        Toast.makeText(this, "Sent event of type: " + eventName, Toast.LENGTH_SHORT).show();
    }
}