package com.attentive.androidsdk.journeys

import com.attentive.androidsdk.events.Item

/**
 * Supplies the text of a cart abandonment notification.
 *
 * The SDK calls [createCopy] on a background thread while the app is in the foreground, shortly
 * after the cart changes, and saves the result for when the notification is shown. This lets
 * providers use on-device models that refuse background use. If no saved copy matches the cart
 * when the notification is shown, the SDK calls [createCopy] once more before falling back to
 * its default copy.
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
