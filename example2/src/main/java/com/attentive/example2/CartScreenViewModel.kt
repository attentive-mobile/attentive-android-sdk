package com.attentive.example2

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.attentive.example2.database.CartItem

class CartScreenViewModel: ViewModel() {

    val cartItems = mutableStateListOf<CartItem>()

}
