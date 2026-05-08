package com.attentive.androidsdk.events

import kotlinx.serialization.Serializable
import timber.log.Timber

/**
 * A purchase event. Fires when the user completes an order.
 *
 * One [PurchaseEvent] with N items produces N `Purchase` requests plus one `OrderConfirmed`
 * request on the backend.
 *
 * @property items The items in the order. Must be non-empty for the event to produce any requests.
 * @property order The order identifier.
 * @property cart The cart state at checkout. Optional.
 */
@Serializable
data class PurchaseEvent(
    val items: List<Item>,
    val order: Order,
    val cart: Cart? = null,
) : Event() {
    @Serializable
    class Builder(
        private val items: List<Item>,
        private val order: Order,
    ) {
        private var cart: Cart? = null

        fun cart(`val`: Cart?): Builder {
            cart = `val`
            return this
        }

        fun build(): PurchaseEvent {
            Timber.d("PurchaseEvent: items: $items, order: $order, cart: $cart")
            return PurchaseEvent(items, order, cart)
        }
    }
}
