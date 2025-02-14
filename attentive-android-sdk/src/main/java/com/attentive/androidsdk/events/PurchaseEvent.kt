package com.attentive.androidsdk.events

import com.attentive.androidsdk.ParameterValidation
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder

@JsonDeserialize(builder = PurchaseEvent.Builder::class)
class PurchaseEvent private constructor(builder: Builder) :
    Event() {
    @JvmField
    val items: List<Item?>
    @JvmField
    val order: Order
    @JvmField
    val cart: Cart?

    init {
        items = builder.items
        order = builder.order
        cart = builder.cart
    }

    @JsonPOJOBuilder(withPrefix = "")
    class Builder @JsonCreator constructor(
        @JsonProperty("items") items: List<Item?>,
        @JsonProperty("order") order: Order
    ) {
        val items: List<Item?>
        val order: Order
        var cart: Cart? = null

        init {
            ParameterValidation.verifyNotEmpty(items, "items")
            ParameterValidation.verifyNotNull(order, "order")

            this.items = items
            this.order = order
        }

        fun cart(`val`: Cart?): Builder {
            cart = `val`
            return this
        }

        fun build(): PurchaseEvent {
            return PurchaseEvent(this)
        }
    }
}
