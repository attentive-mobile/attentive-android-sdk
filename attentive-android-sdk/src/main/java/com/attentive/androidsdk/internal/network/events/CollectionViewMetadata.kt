package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("CollectionView")
data class CollectionViewMetadata(
    val collectionId: String? = null,
    val collectionTitle: String? = null,
    val products: List<Product>? = null,
) : EventMetadata()
