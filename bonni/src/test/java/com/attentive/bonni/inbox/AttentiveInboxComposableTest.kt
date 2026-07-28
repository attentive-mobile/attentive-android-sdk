package com.attentive.bonni.inbox

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.AttentiveSdk
import com.attentive.androidsdk.inbox.AttentiveInbox
import com.attentive.androidsdk.inbox.InboxState
import com.attentive.androidsdk.inbox.Message
import com.attentive.androidsdk.inbox.Style
import com.attentive.bonni.ui.theme.AttentiveAndroidSDKTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Compose UI tests for the SDK's [AttentiveInbox] composable, driven from bonni
 * (which is the module that has the Compose test + Robolectric + Roborazzi
 * infrastructure). Seeds `AttentiveSdk`'s private inbox state via reflection so
 * tests can drive the composable without hitting the network.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AttentiveInboxComposableTest {

    @get:Rule
    val composeRule = createComposeRule()

    @After
    fun tearDown() {
        setInboxState(InboxState())
        val inboxApiField = AttentiveSdk::class.java.getDeclaredField("inboxApi")
        inboxApiField.isAccessible = true
        inboxApiField.set(AttentiveSdk, null)
    }

    companion object {
        @JvmStatic
        @org.junit.BeforeClass
        fun setUpClass() {
            // Set the flag once for the whole test class so queued ON_RESUME
            // refresh coroutines can't race past a per-test reset and hit real
            // network on CI. Never reset — flag is a test-only escape hatch.
            val field = AttentiveSdk::class.java.getDeclaredField("disableInboxAutoFetchForTest")
            field.isAccessible = true
            field.setBoolean(AttentiveSdk, true)
        }
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
