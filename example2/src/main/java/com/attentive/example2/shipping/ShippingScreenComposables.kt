package com.attentive.example2.shipping

import android.widget.Toolbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.attentive.example2.Routes
import com.attentive.example2.SimpleToolbar


@Composable
fun ShippingScreen(navController: NavController) {
    ShippingScreenContent(navController)
}

@Composable
fun ShippingScreenContent(navController: NavController) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            SimpleToolbar(title = "Checkout", actions = {}, navController)
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                item { ShippingAddressForm(navController) }
                item { BillingAddressForm(navController) }
                item { PaymentMethodForm(navController) }
                item { PlaceOrderButton(navController) }
            }
        }
    }
}

@Composable
fun ShippingAddressForm(navController: NavController) {
    var streetAddress by remember { mutableStateOf("123 Main St") }
    var cityAddress by remember { mutableStateOf("San Diego") }
    var stateAddress by remember { mutableStateOf("CA") }
    Column {
        Text("Shipping Address")
        TextField(streetAddress, enabled = true, onValueChange = {
            streetAddress = it
        })
        TextField(cityAddress, onValueChange = {
            cityAddress = it
        })
        TextField(stateAddress, onValueChange = {
            stateAddress = it
        })
    }
}

@Composable
fun BillingAddressForm(navController: NavController) {
    var streetAddress by remember { mutableStateOf("123 Main St") }
    var cityAddress by remember { mutableStateOf("San Diego") }
    var stateAddress by remember { mutableStateOf("CA") }
    Column {
        Text("Billing Address")
        TextField(streetAddress, onValueChange = {
            streetAddress = it
        })
        TextField(cityAddress, onValueChange = {
            cityAddress = it
        })
        TextField(stateAddress, onValueChange = {
            stateAddress = it
        })
    }
}

@Composable
fun PaymentMethodForm(navController: NavController) {
    var cardNumber by remember { mutableStateOf("1234 5678 9012 3456") }
    Column {
        Text("Payment Method")
        Text("Visa")
        TextField(cardNumber, onValueChange = { cardNumber = it })

    }
}

@Composable
fun PlaceOrderButton(
    navController: NavController,
    viewModel: ShippingScreenViewModel = ShippingScreenViewModel()
) {

    var showThankYouDialog by remember { mutableStateOf(false) }

    Button(onClick = {
        viewModel.placeOrder()
        showThankYouDialog = true
    }) {
        Text("Place Order")
    }

    if(showThankYouDialog){
        ThankYouDialog(navController = navController, onDismiss = { showThankYouDialog = false })
    }
}

@Composable
@Preview
fun ShippingScreenPreview() {
    ShippingScreenContent(navController = NavController(context = LocalContext.current))
}

@Composable
fun ThankYouDialog(navController: NavController, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .background(Color.White)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "Thank you for your order!", fontSize = 24.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    onDismiss()
                    navController.navigate(Routes.ProductScreenRoute.name) {
                        popUpTo(Routes.ProductScreenRoute.name) { inclusive = false }
                    }
                }) {
                    Text("Done")
                }
            }
        }
    }
}
