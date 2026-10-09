package com.attentive.androidsdk.creatives

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.webkit.WebView
import timber.log.Timber

/**
 * WebView that filters touches by a caller-supplied bounding rect. Touches inside the rect
 * are consumed by the WebView normally; touches outside return `false` from `onTouchEvent`
 * so the framework dispatches them to the underlying view hierarchy.
 *
 * Must be a WebView subclass rather than an `OnTouchListener` — a listener returning `false`
 * still lets the WebView's own `onTouchEvent` consume the event; only returning `false` from
 * `onTouchEvent` itself allows pass-through.
 */
open class PassThroughWebView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        // Keep WebView's default style; it makes the view focusable so creative inputs can
        // raise the keyboard.
        defStyleAttr: Int = android.R.attr.webViewStyle,
    ) : WebView(context, attrs, defStyleAttr) {
        /** Bounds within which touches are handled by the WebView. Null = pass all touches to WebView. */
        var creativeBounds: Rect? = null

        override fun onSizeChanged(
            w: Int,
            h: Int,
            oldw: Int,
            oldh: Int,
        ) {
            super.onSizeChanged(w, h, oldw, oldh)
            val bounds = creativeBounds
            if (bounds != null && oldh > 0 && h != oldh) {
                creativeBounds = shiftWithBottomEdge(bounds, h - oldh)
                Timber.i("Height changed $oldh -> $h, shifted creative bounds to $creativeBounds")
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            val x = event.x.toInt()
            val y = event.y.toInt()
            val bounds = creativeBounds

            if (event.action == MotionEvent.ACTION_DOWN) {
                Timber.i("Touch at ($x, $y) - bounds=$bounds")
            }

            val passThrough =
                bounds != null &&
                    shouldPassThrough(bounds.left, bounds.top, bounds.right, bounds.bottom, x, y)
            return if (passThrough) false else super.onTouchEvent(event)
        }

        companion object {
            /**
             * Creatives are positioned from the bottom of the WebView, so when its height
             * changes by [deltaHeight] the creative moves by the same amount.
             */
            internal fun shiftWithBottomEdge(
                bounds: Rect,
                deltaHeight: Int,
            ): Rect = Rect(bounds.left, bounds.top + deltaHeight, bounds.right, bounds.bottom + deltaHeight)

            /**
             * Returns true if a touch at ([x], [y]) should pass through to views behind the
             * WebView. Bounds are inclusive on the top/left edges and exclusive on the
             * right/bottom edges, matching [Rect.contains].
             */
            internal fun shouldPassThrough(
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                x: Int,
                y: Int,
            ): Boolean {
                val inside = x >= left && x < right && y >= top && y < bottom
                return !inside
            }
        }
    }
