package com.attentive.example2

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.attentive.example2.database.ExampleCartItem

@Composable
fun CartScreen(navController: NavController) {
    CartScreenContent(navController)
}

@Composable
fun CartScreenContent(
    navController: NavController,
    viewModel: CartScreenViewModel = CartScreenViewModel()
) {
    val cartItems by viewModel.exampleCartItems.collectAsState()
    Box (modifier = Modifier.fillMaxSize()){
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SimpleToolbar(title = "Cart", actions = {}, navController)
            LazyVerticalGrid(columns = GridCells.Fixed(1)) {
                items(cartItems.size) { index ->
                    ItemInCart(cartItems[index], {
                        viewModel.removeFromCart(cartItems.get(index))
                    })
                }
            }
            Button(modifier = Modifier.align(Alignment.End), onClick = {
                navController.navigate("checkout")
            }) {
                Text("Checkout")
            }
        }
    }
}

@Composable
fun ItemInCart(item: ExampleCartItem, removeCartItem: () -> Unit) {
    Row {
        Text(text = item.product.item.name+ " $${item.product.item.price.price}")
        IconButton(onClick = {
            removeCartItem()
        }) {
            Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete")
        }
    }
}
