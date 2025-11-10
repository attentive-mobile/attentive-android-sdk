package com.attentive.androidsdk.internal.network.events

import kotlinx.serialization.Serializable

@Serializable
data class CollectionViewMetadata(
    val eventType: String = "CollectionView",
    val collectionId: String? = null,
    val collectionTitle: String? = null,
    val products: List<Product>? = null
) : EventMetadata()
