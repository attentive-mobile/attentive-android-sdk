package com.attentive.androidsdk.internal.journeys

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min

internal data class IntentSignals(
    val addToCartCount: Int,
    val cartValue: Double,
    val productViewsOfCartItems: Int,
    val appOpensSinceAdd: Int,
    val hoursSinceLastActivity: Double,
    val hasPurchasedBefore: Boolean,
)

/**
 * Scores how likely a user is to complete a purchase, from 0 to 1, as a logistic function of a
 * weighted sum of on-device signals. Engagement raises the score; time away lowers it.
 */
internal object PurchaseIntentScorer {
    private const val BIAS = -1.0
    private const val ADD_TO_CART_WEIGHT = 0.5
    private const val PRODUCT_VIEW_WEIGHT = 0.6
    private const val APP_OPEN_WEIGHT = 0.4
    private const val MAX_COUNTED_APP_OPENS = 3
    private const val PRIOR_PURCHASE_WEIGHT = 0.8
    private const val CART_VALUE_WEIGHT = 0.3
    private const val CART_VALUE_SCALE = 50.0
    private const val HOURLY_DECAY = 0.15
    private const val MAX_DECAY_HOURS = 48.0

    fun score(signals: IntentSignals): Double {
        val z = BIAS +
            ADD_TO_CART_WEIGHT * ln(1.0 + signals.addToCartCount) +
            PRODUCT_VIEW_WEIGHT * ln(1.0 + signals.productViewsOfCartItems) +
            APP_OPEN_WEIGHT * min(signals.appOpensSinceAdd, MAX_COUNTED_APP_OPENS) +
            (if (signals.hasPurchasedBefore) PRIOR_PURCHASE_WEIGHT else 0.0) +
            CART_VALUE_WEIGHT * ln(1.0 + signals.cartValue.coerceAtLeast(0.0) / CART_VALUE_SCALE) -
            HOURLY_DECAY * signals.hoursSinceLastActivity.coerceIn(0.0, MAX_DECAY_HOURS)
        return 1.0 / (1.0 + exp(-z))
    }
}
