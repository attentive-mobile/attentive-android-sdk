package com.attentive.androidsdk.creatives

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.InputDevice
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.widget.EditText
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.attentive.androidsdk.AttentiveConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Keyboard behavior of a real [Creative] in an edge-to-edge host that also has its own
 * [EditText]: the creative must dismiss only a keyboard it owns, and must stay above the IME.
 * Needs API 30+ for reliable IME visibility from window insets.
 */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 30)
class CreativeKeyboardTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(CreativeKeyboardTestActivity::class.java)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var activity: CreativeKeyboardTestActivity
    private lateinit var creative: Creative
    private lateinit var webView: WebView

    @Before
    fun setUp() {
        activityRule.scenario.onActivity { activity = it }
        assertTrue("Test activity never gained window focus", waitFor(10_000) { onMain { activity.hasWindowFocus() } })
        onMain {
            val config =
                AttentiveConfig.Builder()
                    .applicationContext(activity.applicationContext as Application)
                    .mode(AttentiveConfig.Mode.DEBUG)
                    .domain("games")
                    .build()
            creative = Creative(config, activity.creativeParent, activity)
        }
        assertTrue("Creative WebView was never created", waitFor(10_000) { onMain { creative.isWebViewReady } })
        webView = onMain { requireNotNull(creative.webView) }
    }

    @Test
    fun closeWithCreativeKeyboardUp_hidesKeyboardAndHostDoesNotTakeFocus() {
        openCreativeAndFocusInput()

        creative.closeCreative()

        assertTrue("Closing the creative must hide its keyboard", waitFor(5_000) { !imeVisible() })
        SystemClock.sleep(1_000)
        assertFalse("Keyboard must stay hidden after close", imeVisible())
        assertFalse("Focus must not jump to the host EditText", onMain { activity.hostEditText.hasFocus() })
    }

    @Test
    fun timeoutWhileHostIsTyping_leavesHostKeyboardUp() {
        showHostKeyboard()

        creative.onCreativeTimedOut()

        assertHostKeyboardStaysUp()
    }

    @Test
    fun destroyWhileHostIsTyping_leavesHostKeyboardUp() {
        showHostKeyboard()

        onMain { creative.destroy() }

        assertHostKeyboardStaysUp()
    }

    @Test
    fun creativeKeyboardUp_creativeStaysAboveIme() {
        openCreativeAndFocusInput()

        val aboveIme =
            waitFor(5_000) {
                onMain {
                    val imeTop = webView.rootView.height - imeBottom()
                    val location = IntArray(2)
                    webView.getLocationInWindow(location)
                    imeBottom() > 0 && location[1] + webView.height <= imeTop
                }
            }
        val state =
            onMain {
                val location = IntArray(2)
                webView.getLocationInWindow(location)
                val margin = (webView.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin
                val imm = activity.getSystemService(InputMethodManager::class.java)
                "webViewBottom=${location[1] + webView.height} imeTop=${webView.rootView.height - imeBottom()} " +
                    "imeVisible=${imeVisibleNow()} imeActiveOnWebView=${imm.isActive(webView)} bottomMargin=$margin"
            }
        assertTrue("Creative WebView must end above the IME in an edge-to-edge host ($state)", aboveIme)
    }

    private fun openCreativeAndFocusInput() {
        val loaded = java.util.concurrent.CountDownLatch(1)
        onMain {
            webView.webViewClient =
                object : android.webkit.WebViewClient() {
                    override fun onPageFinished(
                        view: WebView,
                        url: String,
                    ) = loaded.countDown()
                }
            webView.loadDataWithBaseURL(null, INPUT_PAGE, "text/html", "utf-8", null)
        }
        assertTrue("Test page did not load", loaded.await(10, java.util.concurrent.TimeUnit.SECONDS))
        onMain { creative.openCreative(height = webView.height, width = webView.width) }
        instrumentation.waitForIdleSync()
        SystemClock.sleep(500)

        tap(webView, webView.width / 2f, webView.height - TAP_FROM_BOTTOM_PX)

        val imm = activity.getSystemService(InputMethodManager::class.java)
        assertTrue(
            "Tapping the creative input must show the keyboard for the WebView",
            waitFor(5_000) { onMain { imm.isActive(webView) } && imeVisible() },
        )
    }

    private fun showHostKeyboard() {
        val imm = activity.getSystemService(InputMethodManager::class.java)
        onMain {
            activity.hostEditText.requestFocus()
            imm.showSoftInput(activity.hostEditText, InputMethodManager.SHOW_IMPLICIT)
        }
        assertTrue(
            "Host keyboard did not show",
            waitFor(5_000) { onMain { imm.isActive(activity.hostEditText) } && imeVisible() },
        )
    }

    private fun assertHostKeyboardStaysUp() {
        instrumentation.waitForIdleSync()
        SystemClock.sleep(1_500)
        val imm = activity.getSystemService(InputMethodManager::class.java)
        assertTrue("Host keyboard must stay up", imeVisible())
        assertTrue("Host EditText must keep the keyboard", onMain { imm.isActive(activity.hostEditText) })
        assertTrue("Host EditText must keep focus", onMain { activity.hostEditText.hasFocus() })
    }

    private fun imeVisible(): Boolean = onMain { imeVisibleNow() }

    private fun imeVisibleNow(): Boolean = ViewCompat.getRootWindowInsets(activity.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) == true

    private fun imeBottom(): Int =
        ViewCompat.getRootWindowInsets(activity.window.decorView)?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0

    private fun tap(
        view: WebView,
        x: Float,
        y: Float,
    ) {
        onMain {
            val downTime = SystemClock.uptimeMillis()
            val down =
                MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0).apply {
                    source = InputDevice.SOURCE_TOUCHSCREEN
                }
            val up =
                MotionEvent.obtain(downTime, downTime + 50, MotionEvent.ACTION_UP, x, y, 0).apply {
                    source = InputDevice.SOURCE_TOUCHSCREEN
                }
            view.dispatchTouchEvent(down)
            view.dispatchTouchEvent(up)
            down.recycle()
            up.recycle()
        }
    }

    private fun <T> onMain(block: () -> T): T {
        var result: Result<T>? = null
        instrumentation.runOnMainSync { result = runCatching(block) }
        return requireNotNull(result).getOrThrow()
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

    private companion object {
        // The input is 200 CSS px tall (>= 200 device px), so this always lands on it.
        const val TAP_FROM_BOTTOM_PX = 50f
        const val INPUT_PAGE =
            "<html><head><meta name=\"viewport\" content=\"width=device-width\"></head>" +
                "<body style=\"margin:0\">" +
                "<input type=\"email\" style=\"position:fixed;bottom:0;left:0;width:100%;height:200px;font-size:32px\">" +
                "</body></html>"
    }
}

/**
 * Edge-to-edge host with its own [EditText] at the top and a full-window parent for the
 * creative, like a Compose screen that overlays the creative on its content.
 */
class CreativeKeyboardTestActivity : Activity() {
    lateinit var hostEditText: EditText
    lateinit var creativeParent: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN,
        )

        hostEditText = EditText(this)
        creativeParent = FrameLayout(this)
        setContentView(
            FrameLayout(this).apply {
                addView(
                    hostEditText,
                    FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP)
                        .apply { topMargin = 200 },
                )
                addView(creativeParent, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            },
        )
    }
}
