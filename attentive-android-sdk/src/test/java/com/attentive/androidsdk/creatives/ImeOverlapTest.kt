package com.attentive.androidsdk.creatives

import org.junit.Assert.assertEquals
import org.junit.Test

class ImeOverlapTest {
    @Test
    fun noIme_noOverlap() {
        assertEquals(0, imeOverlap(imeBottom = 0, windowHeight = 2000, parentBottomInWindow = 2000))
    }

    @Test
    fun parentReachesWindowBottom_overlapIsImeHeight() {
        assertEquals(800, imeOverlap(imeBottom = 800, windowHeight = 2000, parentBottomInWindow = 2000))
    }

    @Test
    fun parentEndsAboveWindowBottom_overlapIsReducedByGap() {
        // e.g. a 150px bottom nav bar below the parent
        assertEquals(650, imeOverlap(imeBottom = 800, windowHeight = 2000, parentBottomInWindow = 1850))
    }

    @Test
    fun hostAlreadyResizedForIme_noOverlap() {
        // adjustResize or imePadding moved the parent's bottom above the IME
        assertEquals(0, imeOverlap(imeBottom = 800, windowHeight = 2000, parentBottomInWindow = 1200))
    }
}
