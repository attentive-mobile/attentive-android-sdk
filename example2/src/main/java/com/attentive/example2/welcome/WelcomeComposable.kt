package com.attentive.example2.welcome

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.attentive.example2.R
import com.attentive.example2.Routes
import com.attentive.example2.cart.CartScreen
import com.attentive.example2.settings.debug.DebugScreenComposables
import com.attentive.example2.settings.SettingsScreen
import com.attentive.example2.product.ProductScreen
import com.attentive.example2.shipping.ShippingScreen
import com.attentive.example2.ui.theme.AttentiveAndroidSDKTheme
import com.attentive.example2.ui.theme.BonniPink

@Composable
fun WelcomeScreenContent(navController: NavHostController) {
    var newAccount by remember { mutableStateOf(false) }
    var existingAccount by remember { mutableStateOf(false) }
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier.paint(
                painterResource(R.drawable.background),
                contentScale = ContentScale.FillHeight
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 64.dp),
                horizontalAlignment = CenterHorizontally,
            ) {
                Greeting()
                SignUpForm(isVisible = newAccount, navController)
                SignInForm(isVisible = existingAccount, navController)
                Column(horizontalAlignment = CenterHorizontally, modifier = Modifier.padding(top = 106.dp)) {
                    SignInButton()
                    ContinueAsGuestButton(navController)

                }
            }
        }
    }
}


@Preview
@Composable
fun SignInButton() {
    Button(
        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
        onClick = {},
        shape = RectangleShape,
        modifier = Modifier
            .width(378.dp)
            .height(62.dp)
        //onClick = { existingAccount = true }
    ) {
        Text(
            "SIGN IN",
            color = White,
            fontSize = 22.sp,
            fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
            fontWeight = FontWeight.Normal
        )
    }
}

@Preview
@Composable
fun ContinueAsGuestButton(navController: NavHostController = rememberNavController()) {
    Button(
        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        shape = RectangleShape,
        onClick = { navController.navigate(Routes.ProductScreenRoute.name) },
        modifier = Modifier
            .width(378.dp)
            .height(62.dp)
    ) {
        Text(
            "CONTINUE AS GUEST",
            color = Color.Black,
            fontSize = 22.sp,
            fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
            fontWeight = FontWeight.Normal
        )
    }
}

@Preview
@Composable
fun CreateAccountButton() {
    Button(
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        onClick = {}
        //    onClick = { newAccount = true }
    ) {
        Text("Create Account", color = Color.Black)
    }
}

@Composable
fun WelcomeScreen(navController: NavHostController = rememberNavController()) {
    Scaffold(modifier = Modifier.fillMaxSize(), containerColor = White) { innerPadding ->
        NavHost(navController = navController, startDestination = Routes.WelcomeScreenRoute.name) {
            composable(Routes.WelcomeScreenRoute.name) {
                WelcomeScreenContent(navController)
            }
            composable(Routes.ProductScreenRoute.name) {
                ProductScreen(navController)
            }
            composable(Routes.CartScreen.name) {
                CartScreen(navController)
            }
            composable(Routes.ShippingScreen.name) {
                ShippingScreen(navController)
            }
            composable(Routes.SettingsScreen.name) {
                SettingsScreen(navController)
            }
            composable(Routes.DebugScreen.name) {
                DebugScreenComposables(navController)
            }
        }
    }
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

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun WelcomeScreenContentPreview() {
    val navController = rememberNavController()
    AttentiveAndroidSDKTheme {
        WelcomeScreenContent(navController = navController)
    }
}

@Preview
@Composable
fun Greeting() {
    Column {
        Text(
            text = "HEY BESTIE!",
            fontSize = 38.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 204.dp),
            fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Welcome to Bonni Beauty!",
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
            lineHeight = 62.sp,
            fontSize = 54.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun SignUpForm(
    isVisible: Boolean,
    navHostController: NavHostController,
    viewModel: SignUpSignInViewModel = ViewModelProvider(LocalActivity.current as ComponentActivity)[SignUpSignInViewModel::class.java]
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var submitIsVisible by remember { mutableStateOf(false) }
    val signedIn by viewModel.signedIn.collectAsState()

    LaunchedEffect(signedIn) {
        if (signedIn) navHostController.navigate(Routes.ProductScreenRoute.name)
    }
    if (isVisible) {
        Column {
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") }
            )
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") }
            )
            TextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    submitIsVisible = password == confirmPassword
                },
                label = { Text("Confirm Password") }
            )
            if (submitIsVisible) {
                Button(onClick = {
                    viewModel.onSignUp(email, password)

                }) {
                    Text("Submit")
                }
            }
        }
    }
}

@Composable
fun SignInForm(
    isVisible: Boolean,
    navHostController: NavHostController,
    viewModel: SignUpSignInViewModel = ViewModelProvider(LocalActivity.current as ComponentActivity)[SignUpSignInViewModel::class.java]
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val signedIn = viewModel.signedIn.collectAsState()

    LaunchedEffect(signedIn) {
        if (signedIn.value) navHostController.navigate(Routes.ProductScreenRoute.name)
    }

    if (isVisible) {
        Column {
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") }
            )
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") }
            )
            Button(onClick = {
                viewModel.onSignIn(email, password)
            }) {
                Text("Submit")
            }

        }
    }
}

@Composable
fun SignOutButton() {

}