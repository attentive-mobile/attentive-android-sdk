package com.attentive.example2.cart

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import com.attentive.example2.Routes
import com.attentive.example2.SimpleToolbar
import com.attentive.example2.database.ExampleCartItem

@Composable
fun CartScreen(navController: NavController) {
    CartScreenContent(navController)
}

@Composable
fun CartScreenContent(
    navController: NavController,
    viewModel: CartScreenViewModel = ViewModelProvider(
        LocalActivity.current as ComponentActivity
    )[CartScreenViewModel::class.java]
) {
    val cartItems by viewModel.exampleCartItems.collectAsState()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SimpleToolbar(title = "Cart", actions = {}, navController)
            LazyColumn {
                items(cartItems.size + 1) { index ->
                    if (index < cartItems.size) {
                        ItemInCart(cartItems[index], {
                            viewModel.removeFromCart(cartItems.get(index))
                        })
                    } else {
                        // Checkout button
                        CheckoutButton(navController, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun CheckoutButton(navController: NavController, viewModel: CartScreenViewModel) {
    var total = 0.0f
    viewModel.exampleCartItems.collectAsState().value.let {
        if (it.isNotEmpty()) {
            total = it.sumOf { item -> item.product.item.price.price.toDouble() * item.quantity }
                .toFloat()
        }
    }
    Button(modifier = Modifier.padding(24.dp), onClick = {
        navController.navigate(Routes.ShippingScreen.name)
    }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Total: $${total}")
            Text("Checkout")
        }
    }
}

@Composable
fun ItemInCart(item: ExampleCartItem, removeCartItem: () -> Unit) {
    Row(modifier = Modifier.padding(16.dp)) {
        Card {
            Column(
                modifier = Modifier
                    .width(240.dp)
                    .height(240.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    ImageBitmap.imageResource(item.product.imageId),
                    contentDescription = "T shirt",
                    modifier = Modifier.weight(4.0f)
                )
                Text(
                    text = item.product.item.name + " $${item.product.item.price.price}",
                    modifier = Modifier.weight(1.0f)
                )
                Text(text = "Quantity: ${item.quantity}", modifier = Modifier.weight(1.0f))
            }
        }
        IconButton(onClick = {
            removeCartItem()
        }) {
            Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete")
        }
    }
}

