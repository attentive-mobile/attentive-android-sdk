package com.attentive.example2.product

import android.util.Log
import android.widget.ImageView.ScaleType
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.attentive.androidsdk.events.Item
import com.attentive.example2.R
import com.attentive.example2.Routes
import com.attentive.example2.SimpleToolbar
import com.attentive.example2.cart.CartScreenViewModel
import com.attentive.example2.database.ExampleProduct
import com.attentive.example2.ui.theme.BonniGreen
import com.attentive.example2.ui.theme.BonniPink
import com.attentive.example2.ui.theme.BonniYellow
import timber.log.Timber
import kotlin.random.Random

@Composable
fun ProductScreen(
    navHostController: NavHostController,
    viewModel: ProductViewModel = ViewModelProvider(
        LocalActivity.current as ComponentActivity
    )[ProductViewModel::class.java]
) {
    ProductScreenContent(navHostController, viewModel)
}


@Composable
fun ProductScreenContent(navHostController: NavHostController, viewModel: ProductViewModel) {
    val cartItemCount by viewModel.cartItemCount.collectAsState()
    val prices = LocalContext.current.resources.getIntArray(R.array.prices)
    val items by viewModel.productItemsFlow.collectAsState()

    Column {
        SimpleToolbar(title = "Products", actions = {
            BadgedBox(
                modifier = Modifier.padding(4.dp),
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
            IconButton(
                onClick = {
                    navHostController.navigate(Routes.SettingsScreen.name)
                }
            ) {
                Icon(imageVector = Icons.Filled.Build, contentDescription = "Debug")
            }
        }, navHostController)
        Text(
            "All Products",
            fontSize = 20.sp,
            fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 16.dp)
        )

        if (items.isNotEmpty()) {
            Timber.d("items is not empty")
            ProductsGrid(items, viewModel::productWasViewed, viewModel::addToCart)
        } else {
            Timber.d("items is empty")
        }
    }
}

@Composable
fun ProductsGrid(
    products: List<ExampleProduct>,
    onProductViewed: (item: Item) -> Unit,
    onAddToCart: (item: ExampleProduct) -> Unit
) {
    LazyVerticalGrid(modifier = Modifier.background(Color.White), columns = GridCells.Fixed(2)) {
        items(4) { index ->
            ProductCard(index, products[index], onProductViewed, onAddToCart)
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
    val bonniColors = listOf(BonniPink, BonniYellow, BonniGreen)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .clickable(
                onClick = {
                    onAddToCart(item)
                    Toast.makeText(context, "Added Product: $index to cart", Toast.LENGTH_SHORT)
                        .show()
                },
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            ImageBitmap.imageResource(item.imageId),
            contentDescription = "T shirt",
            modifier = Modifier.fillMaxSize().height(285.dp)
        )
        ProductTitle(item.item.name!!)
        ProductSubtitle()
        onProductViewed(item.item)
    }
}


@Composable
fun ProductTitle(title: String){
Text(
    text = title,
    fontSize = 15.sp,
    fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
    textAlign = TextAlign.Start,
    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth())
}

@Preview
@Composable
fun ProductSubtitle(){
    Text(
        text = "Product Subtitle | $12",
        fontSize = 12.sp,
        fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
        textAlign = TextAlign.Start,
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth())
}

@Preview
@Composable
fun ProductScreenPreview() {
    ProductScreenContent(
        navHostController = NavHostController(LocalContext.current),
        ProductViewModel()
    )
}



