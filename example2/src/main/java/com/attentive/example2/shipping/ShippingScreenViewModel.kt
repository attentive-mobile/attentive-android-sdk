package com.attentive.example2.shipping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.events.Cart
import com.attentive.androidsdk.events.Item
import com.attentive.androidsdk.events.Order
import com.attentive.androidsdk.events.PurchaseEvent
import com.attentive.example2.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ShippingScreenViewModel : ViewModel() {

    fun placeOrder() {
        viewModelScope.launch(Dispatchers.IO) {
            val items = AppDatabase.getInstance().cartItemDao().getAll()
            val itemss = items.firstOrNull()
            var baseItems = mutableListOf<Item>()
            itemss?.forEach {
                baseItems.add(it.product.item)
            }
            val order = Order.Builder().orderId("anOrderId").build()
            val cart = Cart.Builder().cartId("aCartId").cartCoupon("someCoupon").build()
            val purchaseEvent = PurchaseEvent.Builder(baseItems, order).cart(cart).build()
            AttentiveEventTracker.instance.recordEvent(purchaseEvent)

            AppDatabase.getInstance().cartItemDao().deleteAll()
        }
    }

    override fun onCleared() {

    }
}