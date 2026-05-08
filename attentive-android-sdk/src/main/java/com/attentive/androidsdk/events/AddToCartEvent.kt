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
        /**
         * Deprecated factory method. Use [Builder] instead.
         *
         * @param items The items added to the cart.
         */
        @Deprecated(
            "As of release 1.0.0-beta01, replaced by standard builder methods",
            ReplaceWith("AddToCartEvent.Builder().items(items).build()"),
        )
        fun create(items: List<Item>): AddToCartEvent {
            ParameterValidation.verifyNotEmpty(items, "items")
            return AddToCartEvent(items)
        }
    }

    /**
     * Builder for [AddToCartEvent].
     */
    @Serializable
    class Builder {
        var items: List<Item> = emptyList()
            private set
        var deeplink: String? = null
            private set

        /**
         * Sets the items added to the cart. Required.
         *
         * @param items A non-empty list of [Item]s.
         */
        fun items(items: List<Item>): Builder {
            this.items = items
            return this
        }

        /**
         * Sets an optional deeplink for the event.
         *
         * @param deeplink A deeplink URL, or `null`.
         */
        fun deeplink(deeplink: String?): Builder {
            this.deeplink = deeplink
            return this
        }

        /**
         * Builds the [AddToCartEvent].
         *
         * @throws IllegalArgumentException if [items] is empty.
         */
        fun build(): AddToCartEvent {
            return AddToCartEvent(items, deeplink)
        }
    }
}
