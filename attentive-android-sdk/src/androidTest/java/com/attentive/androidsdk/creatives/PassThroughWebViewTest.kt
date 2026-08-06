package com.attentive.androidsdk.creatives

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.InputDevice
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression guard for MSDK-456. Dispatches synthetic touches directly at the WebView
 * (bypassing the framework's input-injection pipeline, which is unreliable under
 * instrumentation) and asserts that [PassThroughWebView.onTouchEvent] routes them based
 * on [PassThroughWebView.creativeBounds]:
 *  - taps outside the bounds return false, letting the framework propagate the event to
 *    views behind the WebView;
 *  - taps with bounds unset are delegated to `super.onTouchEvent`, so the WebView owns
 *    the event.
 */
@RunWith(AndroidJUnit4::class)
class PassThroughWebViewTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(PassThroughWebViewTestActivity::class.java)

    @Test
    fun tapOutsideCreativeBounds_shortCircuitsToFalse() {
        val activity = getActivity()
        activity.setCreativeBounds(Rect(0, 0, 0, 0))
        // Zero-sized bounds → every point is outside → PassThroughWebView must return false.

        val downHandled = dispatchDownAtWebViewCenter()

        assertTrue(
            "PassThroughWebView.onTouchEvent should have run",
            activity.recordingWebView.touchEventInvocations.get() > 0,
        )
        assertTrue(
            "Touches outside bounds must be reported as pass-through",
            activity.recordingWebView.shortCircuited.get(),
        )
        assertFalse(
            "Out-of-bounds ACTION_DOWN must return false so the event propagates behind the WebView",
            downHandled,
        )
    }

    @Test
    fun tapWithBoundsUnset_delegatesToSuper() {
        val activity = getActivity()
        activity.setCreativeBounds(null)

        dispatchDownAtWebViewCenter()

        assertTrue(
            "PassThroughWebView.onTouchEvent should have run",
            activity.recordingWebView.touchEventInvocations.get() > 0,
        )
        assertFalse(
            "PassThroughWebView must not short-circuit when bounds are null",
            activity.recordingWebView.shortCircuited.get(),
        )
        assertTrue(
            "PassThroughWebView must record that it delegated to super",
            activity.recordingWebView.delegatedToSuper.get(),
        )
    }

    @Test
    fun tapInsideCreativeBounds_delegatesToSuper() {
        val activity = getActivity()
        // Bounds cover the entire WebView, so the tap at its center is inside.
        activity.setCreativeBounds(Rect(0, 0, 10_000, 10_000))

        dispatchDownAtWebViewCenter()

        assertTrue(
            "PassThroughWebView.onTouchEvent should have run",
            activity.recordingWebView.touchEventInvocations.get() > 0,
        )
        assertFalse(
            "Touches inside bounds must not short-circuit",
            activity.recordingWebView.shortCircuited.get(),
        )
        assertTrue(
            "PassThroughWebView must delegate in-bounds touches to super",
            activity.recordingWebView.delegatedToSuper.get(),
        )
    }

    private fun getActivity(): PassThroughWebViewTestActivity {
        val ref = arrayOfNulls<PassThroughWebViewTestActivity>(1)
        activityRule.scenario.onActivity { ref[0] = it }
        return ref[0]!!
    }

    /**
     * Dispatches an ACTION_DOWN + ACTION_UP pair at the center of the WebView and returns
     * whether the ACTION_DOWN was handled by [PassThroughWebView.dispatchTouchEvent].
     */
    private fun dispatchDownAtWebViewCenter(): Boolean {
        val activity = getActivity()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val resultRef = booleanArrayOf(false)
        instrumentation.runOnMainSync {
            val wv = activity.recordingWebView
            val x = wv.width / 2f
            val y = wv.height / 2f
            val downTime = SystemClock.uptimeMillis()
            val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0).apply {
                source = InputDevice.SOURCE_TOUCHSCREEN
            }
            val up = MotionEvent.obtain(downTime, downTime + 50, MotionEvent.ACTION_UP, x, y, 0).apply {
                source = InputDevice.SOURCE_TOUCHSCREEN
            }
            resultRef[0] = wv.dispatchTouchEvent(down)
            wv.dispatchTouchEvent(up)
            down.recycle()
            up.recycle()
        }
        instrumentation.waitForIdleSync()
        return resultRef[0]
    }
}

/**
 * PassThroughWebView subclass that records how each touch was routed: whether onTouchEvent
 * was invoked, whether shouldPassThrough short-circuited the event, and whether the
 * subclass ended up delegating to super.
 */
class RecordingPassThroughWebView(context: Context) : PassThroughWebView(context) {
    val touchEventInvocations = java.util.concurrent.atomic.AtomicInteger(0)
    val shortCircuited = java.util.concurrent.atomic.AtomicBoolean(false)
    val delegatedToSuper = java.util.concurrent.atomic.AtomicBoolean(false)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        touchEventInvocations.incrementAndGet()
        val bounds = creativeBounds
        val passThrough =
            bounds != null &&
                PassThroughWebView.shouldPassThrough(
                    bounds.left,
                    bounds.top,
                    bounds.right,
                    bounds.bottom,
                    event.x.toInt(),
                    event.y.toInt(),
                )
        if (passThrough) {
            shortCircuited.set(true)
            return false
        }
        delegatedToSuper.set(true)
        return super.onTouchEvent(event)
    }
}

class PassThroughWebViewTestActivity : Activity() {
    lateinit var rootLayout: FrameLayout
    lateinit var recordingWebView: RecordingPassThroughWebView

    fun setCreativeBounds(bounds: Rect?) {
        recordingWebView.creativeBounds = bounds
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
        )

        rootLayout =
            FrameLayout(this).apply {
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
            }

        recordingWebView =
            RecordingPassThroughWebView(this).apply {
                layoutParams =
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER,
                    )
            }
        rootLayout.addView(recordingWebView)

        setContentView(rootLayout)
    }
}
