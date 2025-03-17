package com.attentive.androidsdk.events

import android.util.Log
import kotlinx.serialization.Serializable

@Serializable
data class PurchaseEvent(
    val items: List<Item>,
    val order: Order,
    val cart: Cart? = null
) : Event() {

    @Serializable
    class Builder(
        private val items: List<Item>,
        private val order: Order
    ) {
        private var cart: Cart? = null

        fun cart(`val`: Cart?): Builder {
            cart = `val`
            return this
        }

        fun build(): PurchaseEvent {
            Log.e("pfaff", "PurchaseEvent: items: $items, order: $order, cart: $cart")
            return PurchaseEvent(items, order, cart)
        }
    }
}