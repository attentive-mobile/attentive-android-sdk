package com.attentive.androidsdk.events

import com.attentive.androidsdk.ParameterValidation
import kotlinx.serialization.Serializable

/**
 * A product item within a [PurchaseEvent], [AddToCartEvent], or [ProductViewEvent].
 *
 * @property productId Your canonical product identifier. Required, non-empty.
 * @property productVariantId The variant identifier (e.g. size/color SKU). Required, non-empty.
 * @property price The price (with currency). Required.
 * @property productImage URL of a product image. Optional.
 * @property name The product display name. Optional.
 * @property quantity The quantity. Defaults to 1.
 * @property category The product category. Optional.
 */
@Serializable
data class Item(
    val productId: String,
    val productVariantId: String,
    val price: Price,
    val productImage: String? = null,
    val name: String? = null,
    val quantity: Int = 1,
    val category: String? = null,
) {
    init {
        ParameterValidation.verifyNotEmpty(productId, "productId")
        ParameterValidation.verifyNotEmpty(productVariantId, "productVariantId")
        ParameterValidation.verifyNotNull(price, "price")
    }

    @Serializable
    class Builder(
        private val productId: String,
        private val productVariantId: String,
        private val price: Price,
    ) {
        private var productImage: String? = null
        private var name: String? = null
        private var quantity: Int = 1
        private var category: String? = null

        init {
            ParameterValidation.verifyNotEmpty(productId, "productId")
            ParameterValidation.verifyNotEmpty(productVariantId, "productVariantId")
            ParameterValidation.verifyNotNull(price, "price")
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
            return Item(
                productId = productId,
                productVariantId = productVariantId,
                price = price,
                productImage = productImage,
                name = name,
                quantity = quantity,
                category = category,
            )
        }
    }
}
