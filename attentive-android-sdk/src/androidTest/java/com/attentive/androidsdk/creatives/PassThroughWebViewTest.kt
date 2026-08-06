package com.attentive.androidsdk.creatives

import android.app.Activity
import android.graphics.Rect
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Regression guard for MSDK-456. Confirms that [PassThroughWebView.onTouchEvent] routes taps
 * based on [PassThroughWebView.creativeBounds]:
 *  - taps outside the bounds propagate to views behind the WebView;
 *  - taps with bounds unset are consumed by the WebView.
 *
 * The previous inline `setOnTouchListener` in `Creative` returned `true` for out-of-bounds
 * touches, blocking the underlying UI from receiving them.
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
            // A page-less WebView doesn't reliably consume touches — load a minimal HTML page
            // so the WebView has interactive content to swallow the tap.
            activity.loadInteractivePage()
        }
        val latchRef = arrayOfNulls<CountDownLatch>(1)
        activityRule.scenario.onActivity { activity ->
            latchRef[0] = activity.pageLoadedLatch
        }
        // Await off the main thread — onPageFinished dispatches to the main thread, so
        // blocking there would deadlock the WebView callback we're waiting on.
        assertTrue(
            "WebView page did not finish loading in time",
            latchRef[0]!!.await(10, TimeUnit.SECONDS),
        )

        onView(withId(PassThroughWebViewTestActivity.BUTTON_ID)).perform(click())

        assertFalse("WebView should consume the tap when bounds are unset", clicked.get())
    }
}

class PassThroughWebViewTestActivity : Activity() {
    private lateinit var webView: PassThroughWebView
    private lateinit var button: Button
    var onClicked: (() -> Unit)? = null
    val pageLoadedLatch = CountDownLatch(1)

    internal fun setCreativeBounds(bounds: Rect?) {
        webView.creativeBounds = bounds
    }

    internal fun loadInteractivePage() {
        webView.webViewClient =
            object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    pageLoadedLatch.countDown()
                }
            }
        // A full-viewport clickable body ensures the WebView has hit-testable interactive
        // content covering the whole area, so it consumes ACTION_DOWN.
        val html =
            """
            <!doctype html>
            <html>
              <body style="margin:0;padding:0;width:100vw;height:100vh;background:transparent">
                <a href="#" style="display:block;width:100%;height:100%">tap</a>
              </body>
            </html>
            """.trimIndent()
        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
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
