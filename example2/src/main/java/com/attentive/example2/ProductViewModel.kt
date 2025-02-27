package com.attentive.example2

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

class ProductViewModel : ViewModel() {
    private val _cartItemCount = mutableStateOf(0)
    val cartItemCount: State<Int> = _cartItemCount

    fun addToCart() {
        _cartItemCount.value++
    }
}