package com.attentive.androidsdk.journeys

/**
 * Snapshot of on-device cart abandonment tracking, exposed by
 * [com.attentive.androidsdk.AttentiveSdk.cartAbandonmentState].
 *
 * @property itemCount Total quantity of items in the tracked cart.
 * @property intentScore The current purchase intent score, from 0 to 1, or `null` with no cart.
 * @property checkAtMillis When the cart will next be checked for abandonment, in epoch
 *   milliseconds, or `null` when no check is scheduled.
 */
data class CartAbandonmentState(
    val status: Status = Status.DISABLED,
    val itemCount: Int = 0,
    val intentScore: Double? = null,
    val checkAtMillis: Long? = null,
) {
    enum class Status {
        /** Cart abandonment is not enabled. */
        DISABLED,

        /** No cart is being tracked. */
        EMPTY,

        /** A cart is being tracked and will be checked at [checkAtMillis]. */
        WAITING,

        /** The cart was abandoned and the user was notified. */
        NOTIFIED,

        /** The cart was abandoned but its score was below the threshold. */
        BELOW_THRESHOLD,

        /** The cart was abandoned but notifications are disabled or not permitted. */
        PUSH_UNAVAILABLE,

        /** The user completed a purchase. */
        PURCHASED,
    }
}
