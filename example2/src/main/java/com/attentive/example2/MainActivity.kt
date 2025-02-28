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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.AttentiveLogLevel
import com.attentive.androidsdk.UserIdentifiers
import com.attentive.androidsdk.events.ProductViewEvent
import com.attentive.example2.ui.theme.AttentiveAndroidSDKTheme
import com.attentive.example2.ui.theme.AttentiveDarkYellow
import com.attentive.example2.ui.theme.AttentiveYellow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSampleDB()
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
            Greeting("Android")
        }
    }

    fun initSampleDB() {
        val attentiveConfig =
            AttentiveConfig
                .Builder()
                .context(this)
                .domain("games")
                .mode(AttentiveConfig.Mode.DEBUG)
                .logLevel(AttentiveLogLevel.VERBOSE).build()

        val tracker = AttentiveEventTracker.instance.initialize(attentiveConfig)

        val userIdentifiers = UserIdentifiers.Builder().withClientUserId("app_user_id").build()
        attentiveConfig.identify(userIdentifiers)

//        ProductViewEvent("product_id").tr
//        AttentiveEventTracker.instance.recordEvent()

    }
}