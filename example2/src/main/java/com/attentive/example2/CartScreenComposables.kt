package com.attentive.example2

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun CartScreen(navController: NavController){
    CartScreenContent(navController)
}

@Composable
fun CartScreenContent(navController: NavController, viewModel: CartScreenViewModel = CartScreenViewModel()){
    val cartItems = viewModel.cartItems
    Column {
        SimpleToolbar(title = "Cart", actions = {}, navController)
        LazyVerticalGrid(columns = GridCells.Fixed(1)) {
            items(cartItems.size) {
                CartItem({
                    cartItems.removeAt(it)
                })
            }
        }
    }
}

@Composable
fun CartItem(removeCartItem: () -> Unit){
    Row {
        Column {
            Card {

            }
            Text(text = "Product Name")
        }
        IconButton(onClick = {
            removeCartItem()
        }) {
            Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete")
        }
    }
}