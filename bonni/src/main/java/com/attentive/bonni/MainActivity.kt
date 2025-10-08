package com.attentive.bonni

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.attentive.androidsdk.push.AttentiveFirebaseMessagingService
import com.attentive.bonni.ui.theme.AttentiveAndroidSDKTheme
import com.attentive.bonni.welcome.Greeting
import com.attentive.bonni.welcome.WelcomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AttentiveAndroidSDKTheme {
                val navController = rememberNavController()
                WelcomeScreen(navController)
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        AttentiveAndroidSDKTheme {
            Greeting()
        }
    }
}