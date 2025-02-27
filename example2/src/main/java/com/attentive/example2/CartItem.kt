package com.attentive.example2

data class CartItem(val name: String, val price: Double, val quantity: Int) {
    val total: Double
        get() = price * quantity
}
