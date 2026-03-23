package com.attentive.bonni.welcome

import androidx.compose.runtime.Composable
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

    private fun snapshot(content: @Composable () -> Unit) {
        composeRule.setContent {
            AttentiveAndroidSDKTheme {
                content()
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun greeting_snapshot() = snapshot { Greeting() }

    @Test
    fun continueAsGuestButton_snapshot() = snapshot { ContinueAsGuestButton() }

    @Test
    fun createAccountButton_snapshot() = snapshot { CreateAccountButton() }
}
