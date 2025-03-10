package com.attentive.example2

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.attentive.androidsdk.events.Item
import com.attentive.androidsdk.events.Price
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale
import kotlin.random.Random

@Composable
fun ProductScreen(
    navHostController: NavHostController,
    viewModel: ProductViewModel = viewModel()
) {
    ProductScreenContent(navHostController, viewModel)
}


@Composable
fun ProductScreenContent(navHostController: NavHostController, viewModel: ProductViewModel) {
    val cartItemCount by viewModel.cartItemCount.collectAsState()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SimpleToolbar(title = "Products", actions = {
            BadgedBox(modifier = Modifier.padding(4.dp),
                badge = {
                    if (cartItemCount > 0) {
                        Badge(containerColor = Color.White, contentColor = Color.Black) {
                            Text(text = cartItemCount.toString())
                        }
                    }
                }
            ) {
                IconButton(
                    onClick = {
                        navHostController.navigate(Routes.CartScreen.name)
                    }
                ) {
                    Icon(imageVector = Icons.Filled.ShoppingCart, contentDescription = "Cart")
                }
            }
        }, navHostController)
        Text(
            "Products",
            fontSize = 36.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 48.dp)
        )
        ProductsGrid(viewModel::productWasViewed, viewModel::addToCart)
    }
}

@Composable
fun ProductsGrid(onProductViewed: (item: Item) -> Unit, onAddToCart: (item: Item) -> Unit) {
    LazyVerticalGrid(columns = GridCells.Fixed(2)) {
        items(4) { index ->
            ProductCard(index, onProductViewed, onAddToCart)
        }
    }
}

@Composable
fun ProductCard(
    index: Int,
    onProductViewed: (item: Item) -> Unit,
    onAddToCart: (item: Item) -> Unit
) {
    Card(
        modifier = Modifier
            .padding(32.dp)
            .height(100.dp)
            .clickable(
                onClick = {
                    val price = BigDecimal((index * 10) + 1)
                    val item = Item.Builder(
                        "id + $index",
                        "variantId",
                        Price(price, Currency.getInstance(Locale.getDefault()))
                    ).build()
                    onAddToCart(item)
                },
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ), colors = CardDefaults.cardColors(containerColor = Color(Random(index).nextInt()))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val price = BigDecimal((index * 10) + 1)
            Text("Product id: $index \n Price: ${price}")
            val item = Item.Builder(
                "id",
                "variantId",
                Price(price, Currency.getInstance(Locale.getDefault()))
            ).name("T shirt").build()
            onProductViewed(item)
        }
    }
}

@Preview
@Composable
fun ProductScreenPreview() {
    ProductScreenContent(
        navHostController = NavHostController(LocalContext.current),
        ProductViewModel()
    )
}



