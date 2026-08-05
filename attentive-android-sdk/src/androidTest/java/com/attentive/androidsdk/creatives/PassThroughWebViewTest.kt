package com.attentive.androidsdk.creatives

import android.app.Activity
import android.graphics.Rect
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Confirms that taps landing outside [PassThroughWebView.creativeBounds] reach a view sitting
 * behind the WebView in the z-order. Regression guard for MSDK-456 — the previous
 * `setOnTouchListener` implementation returned `true` for out-of-bounds touches, which
 * consumed the event and blocked the view underneath from receiving it.
 */
@RunWith(AndroidJUnit4::class)
class PassThroughWebViewTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(PassThroughWebViewTestActivity::class.java)

    @Test
    fun tapOutsideCreativeBounds_reachesViewBehindWebView() {
        val clicked = AtomicBoolean(false)

        activityRule.scenario.onActivity { activity ->
            activity.onClicked = { clicked.set(true) }
            // Zero-sized bounds → every tap is "outside" the creative and should pass through.
            activity.setCreativeBounds(Rect(0, 0, 0, 0))
        }

        onView(withId(PassThroughWebViewTestActivity.BUTTON_ID)).perform(click())

        assertTrue("Button behind PassThroughWebView should receive the tap", clicked.get())
    }

    @Test
    fun tapWithBoundsUnset_webViewConsumesEvent() {
        val clicked = AtomicBoolean(false)

        activityRule.scenario.onActivity { activity ->
            activity.onClicked = { clicked.set(true) }
            activity.setCreativeBounds(null)
        }

        // With bounds unset the WebView handles the touch itself. Even a WebView with no page
        // loaded consumes ACTION_DOWN at the native layer, so the click should not reach the
        // button underneath.
        onView(withId(PassThroughWebViewTestActivity.BUTTON_ID)).perform(click())

        assertFalse("WebView should consume the tap when bounds are unset", clicked.get())
    }
}

class PassThroughWebViewTestActivity : Activity() {
    private lateinit var webView: PassThroughWebView
    private lateinit var button: Button
    var onClicked: (() -> Unit)? = null

    internal fun setCreativeBounds(bounds: Rect?) {
        webView.creativeBounds = bounds
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root =
            FrameLayout(this).apply {
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
            }

        button =
            Button(this).apply {
                id = BUTTON_ID
                text = "Behind"
                layoutParams =
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER,
                    )
                setOnClickListener { onClicked?.invoke() }
            }
        root.addView(button)

        webView =
            PassThroughWebView(this).apply {
                layoutParams =
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
            }
        root.addView(webView)

        setContentView(root)
    }

    companion object {
        const val BUTTON_ID = 0x00abc001
    }
}
