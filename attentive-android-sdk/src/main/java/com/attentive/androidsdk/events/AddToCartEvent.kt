package com.attentive.androidsdk.events

import com.attentive.androidsdk.ParameterValidation
import kotlinx.serialization.Serializable

/**
 * An add-to-cart event. Fires when a user adds one or more items to their cart.
 *
 * @property items The items added to the cart. Must be non-empty.
 * @property deeplink Optional deeplink associated with the event. Propagated to the backend
 *   so campaigns can re-target the user with the same URL.
 */
@Serializable
data class AddToCartEvent(
    val items: List<Item>,
    val deeplink: String? = null,
) : Event() {
    init {
        ParameterValidation.verifyNotEmpty(items, "items")
    }

    companion object {
        @Deprecated(
            "As of release 1.0.0-beta01, replaced by standard builder methods",
            ReplaceWith("AddToCartEvent.Builder().items(items).build()"),
        )
        fun create(items: List<Item>): AddToCartEvent {
            ParameterValidation.verifyNotEmpty(items, "items")
            return AddToCartEvent(items)
        }
    }

    @Serializable
    class Builder {
        var items: List<Item> = emptyList()
            private set
        var deeplink: String? = null
            private set

        fun items(items: List<Item>): Builder {
            this.items = items
            return this
        }

        fun deeplink(deeplink: String?): Builder {
            this.deeplink = deeplink
            return this
        }

        /**
         * @throws IllegalArgumentException if [items] is empty.
         */
        fun build(): AddToCartEvent {
            return AddToCartEvent(items, deeplink)
        }
    }
}
