package com.attentive.androidsdk.creatives

import android.app.Activity
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue

/**
 * Waits for [activity] to gain window focus, which the IME needs before it serves any view.
 * On failure, reports which window has focus instead (e.g. a system dialog on the CI AVD).
 */
internal fun awaitWindowFocus(
    activity: Activity,
    timeoutMs: Long = 10_000,
) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val deadline = SystemClock.uptimeMillis() + timeoutMs
    var focused = false
    while (!focused && SystemClock.uptimeMillis() < deadline) {
        instrumentation.runOnMainSync { focused = activity.hasWindowFocus() }
        if (!focused) SystemClock.sleep(100)
    }
    assertTrue("Test activity never gained window focus (${focusedWindows()})", focused)
}

private fun focusedWindows(): String {
    val output = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("dumpsys window")
    return ParcelFileDescriptor.AutoCloseInputStream(output).bufferedReader().use { reader ->
        reader.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("mCurrentFocus") || it.startsWith("mFocusedApp") }
            .distinct()
            .joinToString(" | ")
    }
}
