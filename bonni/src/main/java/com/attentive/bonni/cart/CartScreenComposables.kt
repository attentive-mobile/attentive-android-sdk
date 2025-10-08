package com.attentive.bonni.cart

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import com.attentive.androidsdk.events.Item
import com.attentive.androidsdk.events.Price
import com.attentive.bonni.R
import com.attentive.bonni.Routes
import com.attentive.bonni.SimpleToolbar
import com.attentive.bonni.database.ExampleCartItem
import com.attentive.bonni.database.ExampleProduct
import com.attentive.bonni.product.ProductSubtitle
import com.attentive.bonni.product.ProductTitle
import com.attentive.bonni.settings.HorizontalLine
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale

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
    Box(modifier = Modifier.fillMaxSize().padding(bottom = 16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SimpleToolbar(title = "Cart", {}, navController)
            LazyColumn {
                items(cartItems.size + 1) { index ->
                    if (index < cartItems.size) {
                        ItemInCart(cartItems[index]) {
                            viewModel.removeFromCart(cartItems[index])
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp, end = 16.dp))
            Total(viewModel)
            CheckoutButton(navController)
        }
    }
}

@Composable
fun CheckoutButton(navController: NavController) {
    Button(
        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
        onClick ={
            navController.navigate(Routes.ShippingScreen.name)
        },
        shape = RectangleShape,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .padding(start = 16.dp, end = 16.dp)
    ) {
        Text(
            "CHECKOUT",
            color = White,
            fontSize = 16.sp,
            fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun Total(viewModel: CartScreenViewModel) {
    var total = 0.0f
    viewModel.exampleCartItems.collectAsState().value.let {
        if (it.isNotEmpty()) {
            total = it.sumOf { item -> item.product.item.price.price.toDouble() * item.quantity }
                .toFloat()
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text("Total", modifier = Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily(Font(R.font.degulardisplay_regular)))
        Text("$$total", fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily(Font(R.font.degulardisplay_regular)))
    }
}


@Composable
fun ItemInCart(item: ExampleCartItem, removeCartItem: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .height(140.dp)
                .padding(16.dp)
                .background(White), verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                ImageBitmap.imageResource(item.product.imageId),
                contentDescription = "T shirt",
                modifier = Modifier
                    .height(110.dp)
                    .width(110.dp),
                contentScale = ContentScale.Crop
            )
            Column(verticalArrangement = Arrangement.Top, modifier = Modifier.fillMaxHeight()) {
                ProductTitle(item.product.item.name!!)
                Text(
                    text = "Product Subtitle",
                    fontSize = 12.sp,
                    fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                )
                Spacer(Modifier.weight(1.0f))
                Row(Modifier.padding(start = 16.dp, end = 16.dp)) {
                    Text(
                        text = "${item.quantity}",
                        fontSize = 16.sp,
                        fontFamily = FontFamily(Font(R.font.degulardisplay_regular))
                    )
                    Box(
                        modifier = Modifier
                            .weight(1.0f)
                            .padding(4.dp)
                            .clickable { removeCartItem() }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier
                                .size(12.dp)
                                .align(Alignment.CenterStart)
                        )
                    }
                    Text(
                        text = "$12",
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                        fontSize = 17.sp,
                        fontFamily = FontFamily(Font(R.font.degulardisplay_regular))
                    )
                }
            }

        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    }
}

@Preview
@Composable
fun CartItemPreview() {
    val item = ExampleCartItem(
        "1",
        ExampleProduct(
            "1",
            Item.Builder("productId", "variantId", Price(BigDecimal(20.0), Currency.getInstance(Locale.getDefault())))
                .name("Test Product")
                .build(),
            R.drawable.stick1
        ),
        2
    )
    ItemInCart(item, {})
}

@Preview(showBackground = true)
@Composable
fun CartScreenContentPreview() {
    // Fake NavController for preview
    val navController =  NavController(LocalContext.current)

    // Provide a fake ViewModel with sample data
    val fakeViewModel = object : CartScreenViewModel() {
        override val exampleCartItems = kotlinx.coroutines.flow.MutableStateFlow(
            listOf(
                ExampleCartItem(
                    "1",
                    ExampleProduct(
                        "1",
                        Item.Builder("productId", "variantId", Price(BigDecimal(20.0), Currency.getInstance(Locale.getDefault())))
                            .name("Test Product")
                            .build(),
                        R.drawable.stick1
                    ),
                    2
                ),
                ExampleCartItem(
                    "2",
                    ExampleProduct(
                        "2",
                        Item.Builder("productId2", "variantId2", Price(BigDecimal(15.0), Currency.getInstance(Locale.getDefault())))
                            .name("Another Product")
                            .build(),
                        R.drawable.stick1
                    ),
                    1
                )
            )
        )
    }

    CartScreenContent(
        navController = navController,
        viewModel = fakeViewModel
    )
}

