package com.attentive.example2

import android.util.Log
import androidx.lifecycle.ViewModel
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.events.AddToCartEvent
import com.attentive.androidsdk.events.Item
import com.attentive.androidsdk.events.ProductViewEvent
import com.attentive.example2.database.AppDatabase
import com.attentive.example2.database.CartItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class ProductViewModel : ViewModel() {
    private val viewedItems = mutableListOf<Item>()
    private val cartItems = mutableListOf<Item>()

    private val _cartItemCount = MutableStateFlow(0)
    val cartItemCount: StateFlow<Int> = _cartItemCount

    private val database: AppDatabase by lazy { AppDatabase.getInstance(AttentiveApp.getInstance().applicationContext) }

    fun addToCart(item: Item) {
        CoroutineScope(Dispatchers.IO).launch {
            var cartItem = database.cartItemDao().getAll().firstOrNull { it.id == item.productId }
            if (cartItem == null) {
                cartItem = CartItem(item.productId, item, 1 )
                database.cartItemDao().insert(cartItem)
            } else {

                val updatedCartItem = CartItem(item.productId, item, cartItem.quantity + 1)
                database.cartItemDao().update(updatedCartItem)
            }

            updateCartItemCount()
        }
    }

    private fun updateCartItemCount() {
        _cartItemCount.value = database.cartItemDao().getAll().sumOf { it.quantity }
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