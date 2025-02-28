package com.attentive.example_kotlin.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.events.AddToCartEvent
import com.attentive.androidsdk.events.Cart
import com.attentive.androidsdk.events.Item
import com.attentive.androidsdk.events.Order
import com.attentive.androidsdk.events.Price
import com.attentive.androidsdk.events.ProductViewEvent
import com.attentive.androidsdk.events.PurchaseEvent
import com.example.example_kotlin.R
import java.math.BigDecimal
import java.util.Currency
import java.util.List

class ProductPageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_page)

        // Send "Product View" Event
        val item = createItem()
        val productViewEvent = ProductViewEvent.Builder()
            .items(listOf(item))
            .deeplink("https://mydeeplink.com/product/32423")
            .build()
        AttentiveEventTracker.instance.recordEvent(productViewEvent)
        showToastMessageForEvent("Product View")
    }

    fun addToCartButtonClicked(view: View) {
        // Send "Add to Cart" Event
        val item = createItem()
        val addToCartEvent = AddToCartEvent.Builder()
            .items(listOf(item))
            .deeplink("https://mydeeplink.com/products/32432423")
            .build()
        AttentiveEventTracker.instance.recordEvent(addToCartEvent)
        showToastMessageForEvent("Add to Cart")
    }

    fun purchaseButtonClicked(view: View) {
        // Send "Purchase" Event

        // Construct one or more "Item"s, which represents the product(s) purchased
        val item = createItem()

        // Construct an "Order", which represents the order for the purchase
        val orderId = "23456"
        val order = Order.Builder(orderId).build()

        // (Optional) Construct a "Cart", which represents the cart this Purchase was made from
        val cart = Cart.Builder().cartId("7878").cartCoupon("SomeCoupon").build()

        // Construct a PurchaseEvent, which ties together the preceding objects
        val purchaseEvent = PurchaseEvent.Builder(listOf(item), order).cart(cart).build()

        // Record the PurchaseEvent
        AttentiveEventTracker.instance.recordEvent(purchaseEvent)
        showToastMessageForEvent("Purchase")
    }

    private fun createItem(): Item {
        val productId = "11111"
        val productVariantId = "222"
        val amount = BigDecimal("19.99")
        val currency = Currency.getInstance("USD")
        val price = Price.Builder().price(amount).currency(currency).build()
        return Item.Builder(productId, productVariantId, price).quantity(1).build()
    }

    private fun showToastMessageForEvent(eventName: String) {
        Toast.makeText(this, "Sent event of type: $eventName", Toast.LENGTH_SHORT).show()
    }
}