package com.attentive.androidsdk.creatives

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PassThroughWebViewTest {
    @Test
    fun pointInsideBounds_doesNotPassThrough() {
        assertFalse(PassThroughWebView.shouldPassThrough(100, 200, 300, 400, x = 150, y = 250))
    }

    @Test
    fun pointOutsideBounds_passesThrough() {
        assertTrue(PassThroughWebView.shouldPassThrough(100, 200, 300, 400, x = 50, y = 50))
        assertTrue(PassThroughWebView.shouldPassThrough(100, 200, 300, 400, x = 500, y = 500))
        assertTrue(PassThroughWebView.shouldPassThrough(100, 200, 300, 400, x = 150, y = 50))
        assertTrue(PassThroughWebView.shouldPassThrough(100, 200, 300, 400, x = 50, y = 250))
    }

    @Test
    fun pointOnTopLeftEdge_isInside() {
        assertFalse(PassThroughWebView.shouldPassThrough(100, 200, 300, 400, x = 100, y = 200))
    }

    @Test
    fun pointOnBottomRightEdge_isOutside() {
        // Right and bottom edges are exclusive, matching android.graphics.Rect.contains.
        assertTrue(PassThroughWebView.shouldPassThrough(100, 200, 300, 400, x = 300, y = 400))
    }

    @Test
    fun emptyBounds_passesThroughEverywhere() {
        assertTrue(PassThroughWebView.shouldPassThrough(0, 0, 0, 0, x = 0, y = 0))
        assertTrue(PassThroughWebView.shouldPassThrough(0, 0, 0, 0, x = 100, y = 100))
    }
}
