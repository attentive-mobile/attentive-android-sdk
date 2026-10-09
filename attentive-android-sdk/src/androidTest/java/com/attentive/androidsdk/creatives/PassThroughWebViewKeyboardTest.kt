package com.attentive.androidsdk.creatives

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Regression guard for MSDK-543: creative inputs must be able to raise the soft keyboard.
 * In 2.2.1–2.3.0 [PassThroughWebView] dropped WebView's default style, so it was never
 * focusable in touch mode and the IME ignored the creative's showSoftInput request.
 */
@RunWith(AndroidJUnit4::class)
class PassThroughWebViewKeyboardTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(PassThroughWebViewKeyboardTestActivity::class.java)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun defaultConstructor_isFocusableLikePlainWebView() {
        instrumentation.runOnMainSync {
            val context = instrumentation.targetContext
            val plain = WebView(context)
            val passThrough = PassThroughWebView(context)

            assertTrue("PassThroughWebView must be focusable", passThrough.isFocusable)
            assertTrue("PassThroughWebView must be focusable in touch mode", passThrough.isFocusableInTouchMode)
            assertEquals(plain.isFocusableInTouchMode, passThrough.isFocusableInTouchMode)

            plain.destroy()
            passThrough.destroy()
        }
    }

    @Test
    fun tapOnInput_whileAnotherViewHasFocus_servesImeToWebView() {
        val activity = getActivity()
        assertTrue("Test page did not load", activity.pageLoaded.await(10, TimeUnit.SECONDS))
        awaitWindowFocus(activity)
        instrumentation.runOnMainSync {
            assertTrue("Host EditText should start with focus", activity.hostEditText.requestFocus())
        }
        // Give the renderer a frame to lay out the input before tapping it.
        SystemClock.sleep(500)

        tapWebViewCenter(activity.webView)

        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        var state = ""
        val served =
            waitFor(timeoutMs = 5_000) {
                var active = false
                instrumentation.runOnMainSync {
                    val webView = activity.webView
                    state =
                        "windowFocus=${activity.hasWindowFocus()} webViewFocused=${webView.hasFocus()} " +
                            "imeActive=${imm.isActive(webView)} focusedView=${activity.currentFocus?.javaClass?.simpleName}"
                    active = webView.hasFocus() && imm.isActive(webView)
                }
                active
            }
        assertTrue("Tapping a creative input must focus the WebView and attach the IME to it ($state)", served)
    }

    @Test
    fun heightChange_shiftsCreativeBoundsWithBottomEdge() {
        val activity = getActivity()
        var originalHeight = 0
        instrumentation.runOnMainSync {
            originalHeight = activity.webView.height
            activity.webView.creativeBounds = Rect(0, originalHeight - 300, 400, originalHeight)
            activity.webView.layoutParams =
                (activity.webView.layoutParams as LinearLayout.LayoutParams).apply {
                    weight = 0f
                    height = originalHeight - 200
                }
        }
        instrumentation.waitForIdleSync()

        instrumentation.runOnMainSync {
            assertEquals(originalHeight - 200, activity.webView.height)
            assertEquals(Rect(0, originalHeight - 500, 400, originalHeight - 200), activity.webView.creativeBounds)
        }
    }

    private fun getActivity(): PassThroughWebViewKeyboardTestActivity {
        val ref = arrayOfNulls<PassThroughWebViewKeyboardTestActivity>(1)
        activityRule.scenario.onActivity { ref[0] = it }
        return requireNotNull(ref[0]) { "Activity not available" }
    }

    private fun tapWebViewCenter(webView: WebView) {
        instrumentation.runOnMainSync {
            val x = webView.width / 2f
            val y = webView.height / 2f
            val downTime = SystemClock.uptimeMillis()
            val down =
                MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0).apply {
                    source = InputDevice.SOURCE_TOUCHSCREEN
                }
            val up =
                MotionEvent.obtain(downTime, downTime + 50, MotionEvent.ACTION_UP, x, y, 0).apply {
                    source = InputDevice.SOURCE_TOUCHSCREEN
                }
            webView.dispatchTouchEvent(down)
            webView.dispatchTouchEvent(up)
            down.recycle()
            up.recycle()
        }
    }

    private fun waitFor(
        timeoutMs: Long,
        condition: () -> Boolean,
    ): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return true
            SystemClock.sleep(100)
        }
        return condition()
    }
}

/**
 * Hosts a focused [EditText] above a [PassThroughWebView] showing a full-page `<input>`, so
 * the WebView has to take focus from another view, as it does when hosted in Compose.
 */
class PassThroughWebViewKeyboardTestActivity : Activity() {
    lateinit var hostEditText: EditText
    lateinit var webView: PassThroughWebView
    val pageLoaded = CountDownLatch(1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
        )
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        hostEditText = EditText(this)
        webView =
            PassThroughWebView(this).apply {
                settings.javaScriptEnabled = true
                webViewClient =
                    object : WebViewClient() {
                        override fun onPageFinished(
                            view: WebView,
                            url: String,
                        ) {
                            pageLoaded.countDown()
                        }
                    }
                loadDataWithBaseURL(null, INPUT_PAGE, "text/html", "utf-8", null)
            }

        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(hostEditText, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                addView(webView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            },
        )
    }

    companion object {
        private const val INPUT_PAGE =
            "<html><body style=\"margin:0\">" +
                "<input type=\"email\" style=\"width:100%;height:100vh;font-size:32px\">" +
                "</body></html>"
    }
}
