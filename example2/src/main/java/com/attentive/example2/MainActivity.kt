package com.attentive.example2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.attentive.androidsdk.push.AttentiveFirebaseMessagingService
import com.attentive.example2.ui.theme.AttentiveAndroidSDKTheme
import com.attentive.example2.welcome.Greeting
import com.attentive.example2.welcome.WelcomeScreen

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

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        AttentiveAndroidSDKTheme {
            Greeting()
        }
    }
}