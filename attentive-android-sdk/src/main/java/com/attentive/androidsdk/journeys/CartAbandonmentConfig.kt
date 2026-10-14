package com.attentive.androidsdk.journeys

import java.util.concurrent.TimeUnit

/**
 * Configures on-device cart abandonment detection. Pass to
 * [com.attentive.androidsdk.AttentiveSdk.enableCartAbandonment].
 *
 * After each [com.attentive.androidsdk.events.AddToCartEvent], the SDK waits [delayMillis]. If no
 * [com.attentive.androidsdk.events.PurchaseEvent] is recorded in that window and the cart's
 * purchase intent score is at least [scoreThreshold], the SDK shows a local notification that
 * opens the cart.
 *
 * @property delayMillis How long to wait after the most recent add-to-cart. Defaults to one hour.
 * @property scoreThreshold Minimum purchase intent score, from 0 to 1, needed to notify.
 * @property cartDeeplink Deep link the notification opens when the add-to-cart events carried none.
 * @property copyProvider Supplies the notification title and body. Defaults to built-in copy.
 */
class CartAbandonmentConfig private constructor(
    val delayMillis: Long,
    val scoreThreshold: Double,
    val cartDeeplink: String?,
    val copyProvider: AbandonmentCopyProvider?,
) {
    class Builder {
        private var delayMillis: Long = DEFAULT_DELAY_MILLIS
        private var scoreThreshold: Double = DEFAULT_SCORE_THRESHOLD
        private var cartDeeplink: String? = null
        private var copyProvider: AbandonmentCopyProvider? = null

        fun delay(duration: Long, unit: TimeUnit): Builder {
            require(duration > 0) { "delay must be positive" }
            delayMillis = unit.toMillis(duration)
            return this
        }

        fun scoreThreshold(threshold: Double): Builder {
            require(threshold in 0.0..1.0) { "scoreThreshold must be between 0 and 1" }
            scoreThreshold = threshold
            return this
        }

        fun cartDeeplink(deeplink: String?): Builder {
            cartDeeplink = deeplink
            return this
        }

        fun copyProvider(provider: AbandonmentCopyProvider?): Builder {
            copyProvider = provider
            return this
        }

        fun build(): CartAbandonmentConfig =
            CartAbandonmentConfig(delayMillis, scoreThreshold, cartDeeplink, copyProvider)
    }

    companion object {
        val DEFAULT_DELAY_MILLIS: Long = TimeUnit.HOURS.toMillis(1)
        const val DEFAULT_SCORE_THRESHOLD: Double = 0.3
    }
}
