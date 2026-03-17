package com.attentive.bonni.welcome

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.attentive.bonni.ui.theme.AttentiveAndroidSDKTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class WelcomeScreenSnapshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun greeting_snapshot() {
        composeRule.setContent {
            AttentiveAndroidSDKTheme {
                Greeting()
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun continueAsGuestButton_snapshot() {
        composeRule.setContent {
            AttentiveAndroidSDKTheme {
                ContinueAsGuestButton()
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun createAccountButton_snapshot() {
        composeRule.setContent {
            AttentiveAndroidSDKTheme {
                CreateAccountButton()
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
