package com.attentive.example2

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.attentive.example2.cart.CartScreen
import com.attentive.example2.product.ProductScreen
import com.attentive.example2.shipping.ShippingScreen
import com.attentive.example2.ui.theme.AttentiveAndroidSDKTheme
import com.attentive.example2.ui.theme.AttentiveDarkYellow
import com.attentive.example2.ui.theme.AttentiveYellow

@Composable
fun WelcomeScreenContent(navController: NavHostController){
    var newAccount by remember { mutableStateOf(false) }
    Scaffold(modifier = Modifier.fillMaxSize(), containerColor = AttentiveYellow) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 64.dp),
            horizontalAlignment = CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Greeting(
                name = "Android",
                modifier = Modifier
                    .padding(innerPadding)
                    .weight(.5f)
            )

            AccountNameTextField(isVisible = newAccount)
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Attentive Logo",
                modifier = Modifier.size(128.dp)
            )
            Column(horizontalAlignment = CenterHorizontally) {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = AttentiveDarkYellow),
                    onClick = { newAccount = true }) {
                    Text("Create Account", color = Color.Black)
                }

                Button(onClick = { navController.navigate(Routes.ProductScreenRoute.name)}) {
                    Text("Continue as guest")
                }
            }
        }
    }
}
@Composable
fun WelcomeScreen(navController: NavHostController = rememberNavController()) {
    Scaffold(modifier = Modifier.fillMaxSize(), containerColor = AttentiveYellow) { innerPadding ->
        NavHost(navController = navController, startDestination = Routes.WelcomeScreenRoute.name) {
            composable(Routes.WelcomeScreenRoute.name) {
                WelcomeScreenContent(navController)
            }
            composable(Routes.ProductScreenRoute.name) {
                ProductScreen(navController)
            }
            composable(Routes.CartScreen.name){
                CartScreen(navController)
            }
            composable(Routes.ShippingScreen.name){
                ShippingScreen(navController)
            }
        }
    }
}

@Composable
fun AccountNameTextField(isVisible: Boolean) {
    var currentText by remember { mutableStateOf("") }
    if (isVisible)
        TextField(
            value = currentText,
            onValueChange = { currentText = it },
            label = { Text("Account Name") },
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White)
        )
}

@Preview
@Composable
fun AccountNameTextFieldPreview() {
    TextField(
        value = "",
        onValueChange = { },
        label = { Text("Account Name") },
        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White)
    )
}

@Preview
@Composable
fun WelcomeScreenPreview() {
    AttentiveAndroidSDKTheme {
        WelcomeScreen()
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Attentive Mobile Sample App!",
        fontSize = 24.sp,
        modifier = modifier
    )
}