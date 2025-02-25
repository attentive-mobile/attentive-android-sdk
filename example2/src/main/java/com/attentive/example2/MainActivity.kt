package com.attentive.example2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attentive.example2.ui.theme.AttentiveAndroidSDKTheme
import com.attentive.example2.ui.theme.AttentiveDarkYellow
import com.attentive.example2.ui.theme.AttentiveYellow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AttentiveAndroidSDKTheme {
                WelcomeScreen()
            }
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

//@Composable
//fun CreateAccountButton

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        AttentiveAndroidSDKTheme {
            Greeting("Android")
        }
    }

    @Composable
    fun WelcomeScreen(){
        Scaffold(modifier = Modifier.fillMaxSize(), containerColor = AttentiveYellow) { innerPadding ->
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp , vertical = 64.dp), horizontalAlignment = CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                Greeting(
                    name = "Android",
                    modifier = Modifier.padding(innerPadding).weight(.5f)
                )
                Image(painter = painterResource(id = R.drawable.ic_launcher_foreground), contentDescription = "Attentive Logo", modifier = Modifier.size(128.dp))

                Column(horizontalAlignment = CenterHorizontally) {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = AttentiveDarkYellow),
                        onClick = { }) {

                        Text("Create Account", color = Color.Black)
                    }

                    Button(onClick = { }) {
                        Text("Continue as guest")
                    }
                }
            }
        }
    }

    @Preview
    @Composable
    fun WelcomeScreenPreview() {
        AttentiveAndroidSDKTheme {
            WelcomeScreen()
        }
    }
}