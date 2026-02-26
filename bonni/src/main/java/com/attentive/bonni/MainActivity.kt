package com.attentive.bonni

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.attentive.bonni.ui.theme.AttentiveAndroidSDKTheme
import com.attentive.bonni.welcome.Greeting
import com.attentive.bonni.welcome.WelcomeScreen
import timber.log.Timber

class MainActivity : ComponentActivity() {
    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AttentiveAndroidSDKTheme {
                navController = rememberNavController()

                // Log backstack changes
                LaunchedEffect(navController) {
                    navController.currentBackStack.collect { backStack ->
                        val entries =
                            backStack.map { entry ->
                                "${entry.destination.route}"
                            }
                        Timber.d("📚 BackStack changed: ${entries.joinToString(" -> ")}")
                        Timber.d("📚 BackStack size: ${entries.size}")
                    }
                }

                WelcomeScreen(navController)
            }
        }

        // Handle deep link if launched with one
        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                Timber.d("🔗 Deep link received: $uri")
                navigateToDeepLink(uri)
            }
        }
    }

    private fun navigateToDeepLink(uri: Uri) {
        // Only navigate if navController is initialized
        if (!::navController.isInitialized) {
            Timber.w("🔗 NavController not initialized yet, skipping deep link")
            return
        }

        when {
            uri.scheme == "bonni" && uri.host == "cart" -> {
                Timber.d("🔗 Navigating to cart via deep link")
                navController.navigate(Routes.CartScreen.name)
            }
            // Add other deep link routes as needed
            else -> {
                Timber.w("🔗 Unknown deep link: $uri")
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
