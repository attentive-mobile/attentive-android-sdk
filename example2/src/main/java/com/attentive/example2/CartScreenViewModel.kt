package com.attentive.example2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attentive.example2.database.AppDatabase
import com.attentive.example2.database.ExampleCartItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CartScreenViewModel: ViewModel() {

    private val database: AppDatabase by lazy { AppDatabase.getInstance(AttentiveApp.getInstance().applicationContext) }
    private val _exampleCartItems = MutableStateFlow<List<ExampleCartItem>>(emptyList())
    val exampleCartItems: StateFlow<List<ExampleCartItem>> = _exampleCartItems


    init {
        viewModelScope.launch(Dispatchers.IO) {
            database.cartItemDao().getAll()
                .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
                .collect{ items ->
                    _exampleCartItems.value = items
                }
        }
    }

    fun removeFromCart(item: ExampleCartItem){
        viewModelScope.launch(Dispatchers.IO){
            database.cartItemDao().delete(item)
            //_cartItems.value = database.cartItemDao().getAll()
        }
    }
}
