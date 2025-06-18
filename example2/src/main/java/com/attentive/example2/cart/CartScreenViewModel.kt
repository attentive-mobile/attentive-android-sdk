package com.attentive.example2.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attentive.example2.database.AppDatabase
import com.attentive.example2.database.CartRepository
import com.attentive.example2.database.ExampleCartItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

open class CartScreenViewModel: ViewModel() {

    private val database: AppDatabase by lazy { AppDatabase.getInstance() }
    private val _exampleCartItems = MutableStateFlow<List<ExampleCartItem>>(emptyList())
    open val exampleCartItems: StateFlow<List<ExampleCartItem>> = _exampleCartItems
    val cartRepo = CartRepository



    init {
        viewModelScope.launch(Dispatchers.IO) {
            database
                .cartItemDao()
                .getAll()
                .collectLatest { items ->
                    _exampleCartItems.value = items
                }
        }
    }

    fun removeFromCart(item: ExampleCartItem){
        viewModelScope.launch(Dispatchers.IO){
            cartRepo.removeFromCart(item)
        }
    }
}
