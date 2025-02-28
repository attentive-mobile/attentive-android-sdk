package com.attentive.example2

import android.util.Log
import androidx.lifecycle.ViewModel
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.events.AddToCartEvent
import com.attentive.androidsdk.events.Item
import com.attentive.androidsdk.events.ProductViewEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProductViewModel : ViewModel() {
    private val viewedItems = mutableListOf<Item>()
    private val cartItems = mutableListOf<Item>()

    private val _cartItemCount = MutableStateFlow(0)
    val cartItemCount: StateFlow<Int> get() = _cartItemCount

    fun addToCart(item: Item) {
        if(cartItems.contains(item).not()) {
            cartItems.add(item)
        } else {
            cartItems.remove(item)
        }
        _cartItemCount.value = cartItems.size
    }

    fun productWasViewed(item: Item) {
        if(viewedItems.contains(item).not()) {
            viewedItems.add(item)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.e("pfaff", "onCleared")
        val event = ProductViewEvent.Builder().items(viewedItems).build()
        AttentiveEventTracker.instance.recordEvent(event)

        val cartEvent = AddToCartEvent.Builder().items(cartItems).build()
        AttentiveEventTracker.instance.recordEvent(cartEvent)
    }
}