package com.attentive.androidsdk.creatives

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import timber.log.Timber

/**
 * Dismisses the soft keyboard and clears focus only if this WebView owns them. Leaves the
 * keyboard alone when the user is typing in a host view, e.g. when a creative times out or
 * is destroyed in the background.
 */
internal fun WebView.releaseKeyboardIfOwned() {
    if (!hasFocus()) return
    val imm = context.getSystemService(InputMethodManager::class.java)
    if (imm != null && imm.isActive(this)) {
        Timber.i("Creative owns the keyboard, hiding it")
        imm.hideSoftInputFromWindow(windowToken, 0)
    }
    clearFocus()
}

/**
 * Shrinks the WebView from the bottom by however much the IME overlaps [parent], so
 * bottom-anchored creatives stay above the keyboard in edge-to-edge hosts. Hosts that already
 * resize for the IME (adjustResize, `imePadding`) end up with no overlap and no margin.
 */
internal fun WebView.avoidImeOverlap(parent: View) {
    // IME insets are only reported reliably from API 30.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
    val params = layoutParams as? ViewGroup.MarginLayoutParams ?: return
    val imeBottom = ViewCompat.getRootWindowInsets(this)?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
    val parentLocation = IntArray(2)
    parent.getLocationInWindow(parentLocation)
    val overlap = imeOverlap(imeBottom, rootView.height, parentLocation[1] + parent.height)
    if (params.bottomMargin != overlap) {
        Timber.i("IME overlaps creative by %d px", overlap)
        params.bottomMargin = overlap
        layoutParams = params
    }
}

/**
 * How many px of an IME [imeBottom] px tall cover a parent whose bottom edge sits at
 * [parentBottomInWindow] in a window [windowHeight] px tall.
 */
internal fun imeOverlap(
    imeBottom: Int,
    windowHeight: Int,
    parentBottomInWindow: Int,
): Int = (imeBottom - (windowHeight - parentBottomInWindow)).coerceAtLeast(0)
