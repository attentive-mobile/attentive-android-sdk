package com.attentive.androidsdk.internal.journeys

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchaseIntentScorerTest {
    private val baseline =
        IntentSignals(
            addToCartCount = 1,
            cartValue = 30.0,
            productViewsOfCartItems = 0,
            appOpensSinceAdd = 0,
            hoursSinceLastActivity = 1.0,
            hasPurchasedBefore = false,
        )

    @Test
    fun singleAddAfterOneHour_scoresJustAboveDefaultThreshold() {
        assertEquals(0.34, PurchaseIntentScorer.score(baseline), 0.01)
    }

    @Test
    fun engagementSignals_raiseScore() {
        val base = PurchaseIntentScorer.score(baseline)
        assertTrue(PurchaseIntentScorer.score(baseline.copy(addToCartCount = 3)) > base)
        assertTrue(PurchaseIntentScorer.score(baseline.copy(productViewsOfCartItems = 2)) > base)
        assertTrue(PurchaseIntentScorer.score(baseline.copy(appOpensSinceAdd = 1)) > base)
        assertTrue(PurchaseIntentScorer.score(baseline.copy(hasPurchasedBefore = true)) > base)
        assertTrue(PurchaseIntentScorer.score(baseline.copy(cartValue = 300.0)) > base)
    }

    @Test
    fun timeAway_lowersScore() {
        assertTrue(PurchaseIntentScorer.score(baseline.copy(hoursSinceLastActivity = 6.0)) < PurchaseIntentScorer.score(baseline))
    }

    @Test
    fun appOpensAndDecay_areCapped() {
        assertEquals(
            PurchaseIntentScorer.score(baseline.copy(appOpensSinceAdd = 3)),
            PurchaseIntentScorer.score(baseline.copy(appOpensSinceAdd = 30)),
            1e-9,
        )
        assertEquals(
            PurchaseIntentScorer.score(baseline.copy(hoursSinceLastActivity = 48.0)),
            PurchaseIntentScorer.score(baseline.copy(hoursSinceLastActivity = 500.0)),
            1e-9,
        )
    }

    @Test
    fun score_staysWithinZeroAndOne() {
        val extremes =
            listOf(
                baseline.copy(addToCartCount = 0, cartValue = 0.0, hoursSinceLastActivity = 48.0),
                baseline.copy(addToCartCount = 1000, cartValue = 1e9, productViewsOfCartItems = 1000, appOpensSinceAdd = 3, hasPurchasedBefore = true),
            )
        extremes.forEach { assertTrue(PurchaseIntentScorer.score(it) in 0.0..1.0) }
    }
}
