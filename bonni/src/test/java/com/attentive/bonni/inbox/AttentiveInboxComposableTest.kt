package com.attentive.bonni.inbox

import android.app.Application
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.AttentiveSdk
import com.attentive.androidsdk.inbox.AttentiveInbox
import com.attentive.androidsdk.inbox.InboxState
import com.attentive.androidsdk.inbox.Message
import com.attentive.androidsdk.inbox.Style
import com.attentive.bonni.ui.theme.AttentiveAndroidSDKTheme
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Compose UI tests for the SDK's [AttentiveInbox] composable, driven from bonni
 * (which is the module that has the Compose test + Robolectric + Roborazzi
 * infrastructure). Seeds `AttentiveSdk`'s private inbox state via reflection so
 * tests can drive the composable without hitting the network.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class AttentiveInboxComposableTest {

    @get:Rule
    val composeRule = createComposeRule()

    @org.junit.Before
    fun setUp() {
        // Prevent the composable's initializeInbox()/ON_RESUME observer from
        // firing real network calls — CI runners don't have outbound network.
        setDisableAutoFetch(true)
    }

    @After
    fun tearDown() {
        setInboxState(InboxState())
        setDisableAutoFetch(false)
        val inboxApiField = AttentiveSdk::class.java.getDeclaredField("inboxApi")
        inboxApiField.isAccessible = true
        inboxApiField.set(AttentiveSdk, null)
    }

    private fun setDisableAutoFetch(disabled: Boolean) {
        val field = AttentiveSdk::class.java.getDeclaredField("disableInboxAutoFetchForTest")
        field.isAccessible = true
        field.setBoolean(AttentiveSdk, disabled)
    }

    @Test
    fun rendersTitleAndBody_forSmallStyleMessage() {
        seedSdk()
        setInboxState(
            InboxState(
                messages = listOf(smallMessage(id = "m1", title = "Order shipped", body = "Order #12345 is on the way.")),
                unreadCount = 1,
            ),
        )

        composeRule.setContent {
            AttentiveAndroidSDKTheme { AttentiveInbox() }
        }

        composeRule.onNodeWithText("Order shipped").assertIsDisplayed()
        composeRule.onNodeWithText("Order #12345 is on the way.").assertIsDisplayed()
    }

    @Test
    fun rendersEmptyState_whenNoMessages() {
        seedSdk()
        setInboxState(InboxState(messages = emptyList()))

        composeRule.setContent {
            AttentiveAndroidSDKTheme { AttentiveInbox() }
        }

        composeRule.onNodeWithText("No Messages").assertIsDisplayed()
    }

    @Test
    fun clickingRow_invokesOnMessageClickWithMessage() {
        seedSdk()
        val message = smallMessage(id = "clickme", title = "Tap me", body = "clickable row")
        setInboxState(InboxState(messages = listOf(message), unreadCount = 1))

        var clickedMessage: Message? = null
        composeRule.setContent {
            AttentiveAndroidSDKTheme {
                AttentiveInbox(onMessageClick = { clickedMessage = it })
            }
        }

        composeRule.onNodeWithText("Tap me").performClick()

        assertNotNull(clickedMessage)
        assertEquals("clickme", clickedMessage?.id)
    }

    @Test
    fun smallRow_imageOnLeft_snapshot() {
        seedSdk()
        setInboxState(
            InboxState(
                messages = listOf(
                    smallMessage(
                        id = "m1",
                        title = "Your cart is waiting",
                        body = "Pick up where you left off.",
                        imageUrl = "https://picsum.photos/200",
                    ),
                ),
                unreadCount = 1,
            ),
        )

        composeRule.setContent {
            AttentiveAndroidSDKTheme {
                AttentiveInbox(modifier = Modifier.height(120.dp))
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun largeRow_snapshot() {
        seedSdk()
        setInboxState(
            InboxState(
                messages = listOf(
                    Message(
                        id = "m1",
                        title = "Sale ends tonight",
                        body = "50% off on all items.",
                        timestamp = System.currentTimeMillis() - 60 * 60 * 1000L,
                        isRead = false,
                        imageUrl = "https://picsum.photos/400/200",
                        actionUrl = null,
                        style = Style.Large,
                    ),
                ),
                unreadCount = 1,
            ),
        )

        composeRule.setContent {
            AttentiveAndroidSDKTheme {
                AttentiveInbox(modifier = Modifier.height(300.dp))
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    private fun seedSdk() {
        val application = RuntimeEnvironment.getApplication() as Application
        val config = AttentiveConfig.Builder()
            .domain("bonni")
            .mode(AttentiveConfig.Mode.DEBUG)
            .applicationContext(application)
            .build()
        val configField = AttentiveSdk::class.java.getDeclaredField("_config")
        configField.isAccessible = true
        configField.set(AttentiveSdk, config)
    }

    private fun setInboxState(state: InboxState) {
        val field = AttentiveSdk::class.java.getDeclaredField("_inboxState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (field.get(AttentiveSdk) as MutableStateFlow<InboxState>).value = state
    }

    private fun smallMessage(
        id: String,
        title: String,
        body: String,
        imageUrl: String? = null,
    ) = Message(
        id = id,
        title = title,
        body = body,
        timestamp = System.currentTimeMillis() - 60 * 60 * 1000L,
        isRead = false,
        imageUrl = imageUrl,
        actionUrl = null,
        style = Style.Small,
    )
}
