package com.attentive.androidsdk.journeys

import com.attentive.androidsdk.events.Item

/**
 * Supplies the text of a cart abandonment notification. Runs on a background thread when the
 * notification is about to be shown.
 */
interface AbandonmentCopyProvider {
    /**
     * @return the copy to show, or `null` to use the SDK's default copy.
     */
    suspend fun createCopy(cart: AbandonedCart): NotificationCopy?
}

/**
 * The cart a cart abandonment notification is about.
 *
 * @property items The items in the cart, merged by variant.
 * @property intentScore The purchase intent score, from 0 to 1.
 * @property addedAtMillis When the most recent item was added, in epoch milliseconds.
 */
data class AbandonedCart(
    val items: List<Item>,
    val intentScore: Double,
    val addedAtMillis: Long,
)

data class NotificationCopy(
    val title: String,
    val body: String,
)
