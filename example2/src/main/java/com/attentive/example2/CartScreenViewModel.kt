package com.attentive.example2

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel

class CartScreenViewModel: ViewModel() {

    val cartItems = mutableStateListOf<CartItem>()

}
