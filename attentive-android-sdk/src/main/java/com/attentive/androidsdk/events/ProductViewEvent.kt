package com.attentive.androidsdk.events

import com.attentive.androidsdk.ParameterValidation
import kotlinx.serialization.Serializable

@Serializable
data class ProductViewEvent(
    val items: List<Item>,
    val deeplink: String? = null
) : Event() {
    init {
        ParameterValidation.verifyNotEmpty(items, "items")
    }

    companion object {
        @Deprecated(
            "As of release 1.0.0-beta01, replaced by standard builder methods",
            ReplaceWith("ProductViewEvent.Builder().items(items).build()")
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

        fun build(): ProductViewEvent {
            return ProductViewEvent(items, deeplink)
        }
    }
}