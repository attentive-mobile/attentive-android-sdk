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
    /**
     * Builder for [PurchaseEvent].
     *
     * @param items The items in the order. Required.
     * @param order The order identifier. Required.
     */
    @Serializable
    class Builder(
        private val items: List<Item>,
        private val order: Order,
    ) {
        private var cart: Cart? = null

        /**
         * Sets the cart state at checkout.
         *
         * @param `val` The cart, or `null` if unavailable.
         */
        fun cart(`val`: Cart?): Builder {
            cart = `val`
            return this
        }

        /**
         * Builds the [PurchaseEvent].
         */
        fun build(): PurchaseEvent {
            Timber.d("PurchaseEvent: items: $items, order: $order, cart: $cart")
            return PurchaseEvent(items, order, cart)
        }
    }
}
