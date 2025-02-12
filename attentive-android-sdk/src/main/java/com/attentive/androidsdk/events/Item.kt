package com.attentive.androidsdk.events

import com.attentive.androidsdk.ParameterValidation
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder

@JsonDeserialize(builder = Item.Builder::class)
class Item private constructor(builder: Builder) {
    @JvmField
    val productId: String
    @JvmField
    val productVariantId: String
    @JvmField
    val productImage: String?
    @JvmField
    val name: String?
    @JvmField
    val price: Price
    @JvmField
    val quantity: Int
    @JvmField
    val category: String?

    init {
        productId = builder.productId
        productVariantId = builder.productVariantId
        price = builder.price
        productImage = builder.productImage
        name = builder.name
        quantity = builder.quantity
        category = builder.category
    }

    @JsonPOJOBuilder(withPrefix = "")
    class Builder @JsonCreator constructor(
        @JsonProperty("productId") productId: String,
        @JsonProperty("productVariantId") productVariantId: String,
        @JsonProperty("price") price: Price
    ) {
        val productId: String
        val productVariantId: String
        val price: Price
        var productImage: String? = null
        var name: String? = null
        var quantity: Int = 1
        var category: String? = null

        init {
            ParameterValidation.verifyNotEmpty(productId, "productId")
            ParameterValidation.verifyNotEmpty(productVariantId, "productVariantId")
            ParameterValidation.verifyNotNull(price, "price")

            this.productId = productId
            this.productVariantId = productVariantId
            this.price = price
        }

        fun productImage(`val`: String?): Builder {
            productImage = `val`
            return this
        }

        fun name(`val`: String?): Builder {
            name = `val`
            return this
        }

        fun quantity(`val`: Int): Builder {
            quantity = `val`
            return this
        }

        fun category(`val`: String?): Builder {
            category = `val`
            return this
        }

        fun build(): Item {
            return Item(this)
        }
    }
}
