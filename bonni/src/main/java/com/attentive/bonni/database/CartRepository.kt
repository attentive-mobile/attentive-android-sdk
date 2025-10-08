package com.attentive.bonni.database

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object CartRepository {
    private val database: AppDatabase by lazy { AppDatabase.getInstance() }
    private val _cartItemCount = MutableStateFlow(0)
    val cartItemCount: StateFlow<Int> = _cartItemCount

    init {
        // Observe cart changes and update the count
        kotlinx.coroutines.GlobalScope.launch {
            database.cartItemDao().getAll().collectLatest { items ->
                _cartItemCount.value = items.sumOf { it.quantity }
            }
        }
    }

    suspend fun removeFromCart(item: ExampleCartItem) {
        database.cartItemDao().delete(item)
    }
}