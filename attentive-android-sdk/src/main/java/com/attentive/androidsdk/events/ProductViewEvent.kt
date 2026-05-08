package com.attentive.androidsdk.events

import com.attentive.androidsdk.ParameterValidation
import kotlinx.serialization.Serializable

/**
 * A product view event. Fires when the user views one or more products (e.g. lands on a PDP).
 *
 * @property items The items viewed. Must be non-empty.
 * @property deeplink Optional deeplink associated with the event. Propagated to the backend
 *   so campaigns can re-target the user with the same URL.
 */
@Serializable
data class ProductViewEvent(
    val items: List<Item>,
    val deeplink: String? = null,
) : Event() {
    init {
        ParameterValidation.verifyNotEmpty(items, "items")
    }

    companion object {
        @Deprecated(
            "As of release 1.0.0-beta01, replaced by standard builder methods",
            ReplaceWith("ProductViewEvent.Builder().items(items).build()"),
        )
        fun create(items: List<Item>): ProductViewEvent {
            ParameterValidation.verifyNotEmpty(items, "items")
            return ProductViewEvent(items)
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
        fun build(): ProductViewEvent {
            return ProductViewEvent(items, deeplink)
        }
    }
}
