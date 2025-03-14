package com.attentive.example2

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.attentive.androidsdk.events.Item
import com.attentive.example2.database.ExampleProduct
import kotlinx.coroutines.flow.MutableStateFlow
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
    val prices = LocalContext.current.resources.getIntArray(R.array.prices)

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
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 32.dp)
        )
        viewModel.productItemsFlow.collectAsState().value.let {
            if(it.size > 0) {
                ProductsGrid(it, viewModel::productWasViewed, viewModel::addToCart)
            }
        }
    }
}

@Composable
fun ProductsGrid(
    prodcuts: MutableList<ExampleProduct>,
    onProductViewed: (item: Item) -> Unit,
    onAddToCart: (item: ExampleProduct) -> Unit
) {
    LazyVerticalGrid(modifier = Modifier.background(Color.White), columns = GridCells.Fixed(2)) {
        items(4) { index ->
            ProductCard(index, prodcuts[index], onProductViewed, onAddToCart)
        }
    }
}

@Composable
fun ProductCard(
    index: Int,
    item: ExampleProduct,
    onProductViewed: (item: Item) -> Unit,
    onAddToCart: (item: ExampleProduct) -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .padding(8.dp)
            .height(250.dp)
            .clickable(
                onClick = {
                    onAddToCart(item)
                    Toast.makeText(context, "Added Product: $index to cart", Toast.LENGTH_SHORT).show()
                },
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ),
        colors = CardDefaults.cardColors(containerColor = Color(Random(index).nextInt())),
        elevation = CardDefaults.cardElevation()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("${item.item.name} \n$${item.item.price.price}")
            Image(ImageBitmap.imageResource(item.imageId), contentDescription = "T shirt")
            onProductViewed(item.item)
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



