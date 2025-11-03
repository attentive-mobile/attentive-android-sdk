package com.attentive.example2.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.events.AddToCartEvent
import com.attentive.androidsdk.events.Item
import com.attentive.androidsdk.events.ProductViewEvent
import com.attentive.example2.database.AppDatabase
import com.attentive.example2.database.CartRepository
import com.attentive.example2.database.ExampleCartItem
import com.attentive.example2.database.ExampleProduct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductViewModel : ViewModel() {
    private val viewedItems = mutableListOf<Item>()
    private val cartItems = mutableListOf<Item>()
    private val _productItemsFlow = MutableStateFlow<List<ExampleProduct>>(emptyList())
    val productItemsFlow: StateFlow<List<ExampleProduct>> = _productItemsFlow.asStateFlow()
    private val cartRepo = CartRepository
    private val _cartItemCount = MutableStateFlow(0)
    val cartItemCount: StateFlow<Int> = cartRepo.cartItemCount

    private val database: AppDatabase by lazy { AppDatabase.getInstance() }

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                database.productItemDao().getAll().collect{
                    _productItemsFlow.value = it
                }
            }
            database.cartItemDao().getAll().collect {
                _cartItemCount.value = it.sumOf { it.quantity }
            }
        }
    }

    fun addToCart(product: ExampleProduct) {
        CoroutineScope(Dispatchers.IO).launch {
            var cartItem =
                database.cartItemDao().getAll().first().firstOrNull { it.product.id == product.id }
            if (cartItem == null) {
                cartItem = ExampleCartItem(product.id, product, 1)
                database.cartItemDao().insert(cartItem)
            } else {
                val updatedExampleCartItem =
                    ExampleCartItem(product.id, product, cartItem.quantity + 1)
                database.cartItemDao().update(updatedExampleCartItem)
            }

            val addToCartEvent = AddToCartEvent.Builder().items(listOf(product.item)).build()
            AttentiveEventTracker.instance.recordEvent(addToCartEvent)
            updateCartItemCount()
        }
    }

    private suspend fun updateCartItemCount() {
        _cartItemCount.value = database.cartItemDao().getAll().first().sumOf { it.quantity }
    }

    fun productWasViewed(item: Item) {
        if (viewedItems.contains(item).not()) {
            viewedItems.add(item)
        }
    }

    override fun onCleared() {
        super.onCleared()
        val event = ProductViewEvent.Builder().items(viewedItems).build()
       // AttentiveEventTracker.instance.recordEvent(event)

        CoroutineScope(Dispatchers.IO).launch {
            AttentiveEventTracker.instance.recordEvent(event)

            if (cartItems.isNotEmpty()) {
                val cartEvent = AddToCartEvent.Builder().items(cartItems).build()
                AttentiveEventTracker.instance.recordEvent(cartEvent)
            }
        }


    }
}