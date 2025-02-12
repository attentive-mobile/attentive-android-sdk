package com.attentive.androidsdk.events

import com.attentive.androidsdk.ParameterValidation
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder

@JsonDeserialize(builder = AddToCartEvent.Builder::class)
class AddToCartEvent : Event {
    @JvmField
    val items: List<Item>
    val deeplink: String?

    private constructor(items: List<Item>) {
        this.items = items
        this.deeplink = null
    }

    private constructor(builder: Builder) {
        this.items = builder.items
        this.deeplink = builder.deeplink
    }

    @JsonPOJOBuilder(withPrefix = "")
    class Builder {
        var items: List<Item> = ArrayList()
        var deeplink: String? = null

        @JsonCreator
        @Deprecated("As of release 1.0.0-beta01, replaced by standard builder methods")
        constructor(@JsonProperty("items") items: List<Item>) {
            ParameterValidation.verifyNotEmpty(items, "items")

            this.items = items
            this.deeplink = null
        }

        constructor()

        fun items(items: List<Item>): Builder {
            this.items = items
            return this
        }

        fun deeplink(deeplink: String?): Builder {
            this.deeplink = deeplink
            return this
        }

        fun buildIt(): AddToCartEvent {
            return AddToCartEvent(this)
        }

        @Deprecated(
            """As of release 1.0.0-beta01, replaced by {@link ProductViewEvent.Builder#buildIt}
          Only to be used if using the new builder approach not the deprecated one."""
        )
        fun build(): AddToCartEvent {
            return AddToCartEvent(items)
        }
    }
}
