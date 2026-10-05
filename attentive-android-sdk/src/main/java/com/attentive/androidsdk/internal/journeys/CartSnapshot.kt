package com.attentive.androidsdk.internal.journeys

import com.attentive.androidsdk.PersistentStorage
import com.attentive.androidsdk.events.Item
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

@Serializable
internal data class CartSnapshot(
    val items: List<Item>,
    val deeplink: String? = null,
    val lastAddedAtMillis: Long,
    val lastActivityAtMillis: Long,
    val checkAtMillis: Long,
    val addToCartCount: Int,
    val appOpensSinceAdd: Int = 0,
    val outcome: Outcome = Outcome.PENDING,
) {
    @Serializable
    enum class Outcome { PENDING, NOTIFIED, BELOW_THRESHOLD, PUSH_UNAVAILABLE }

    val itemCount: Int get() = items.sumOf { it.quantity }
}

/** Combines items that share a variant, summing their quantities. */
internal fun mergeByVariant(items: List<Item>): List<Item> {
    val merged = LinkedHashMap<String, Item>()
    items.forEach { item ->
        val existing = merged[item.productVariantId]
        merged[item.productVariantId] = existing?.copy(quantity = existing.quantity + item.quantity) ?: item
    }
    return merged.values.toList()
}

/** Persists the tracked cart, recent product views and purchase history across process death. */
internal class CartSnapshotStore(private val storage: PersistentStorage) {
    private val json = Json { ignoreUnknownKeys = true }

    var snapshot: CartSnapshot?
        get() = decode(storage.read(SNAPSHOT_KEY))
        set(value) {
            if (value == null) storage.delete(SNAPSHOT_KEY) else storage.save(SNAPSHOT_KEY, json.encodeToString(value))
        }

    /** View counts keyed by product ID, capped at [MAX_TRACKED_VIEWS] products. */
    var productViews: Map<String, Int>
        get() = storage.read(VIEWS_KEY)?.let { decode<Map<String, Int>>(it) } ?: emptyMap()
        set(value) {
            storage.save(VIEWS_KEY, json.encodeToString(value.entries.toList().takeLast(MAX_TRACKED_VIEWS).associate { it.toPair() }))
        }

    var hasPurchased: Boolean
        get() = storage.readBoolean(PURCHASED_KEY)
        set(value) = storage.save(PURCHASED_KEY, value)

    fun clear() {
        storage.delete(SNAPSHOT_KEY)
        storage.delete(VIEWS_KEY)
        storage.delete(PURCHASED_KEY)
    }

    private inline fun <reified T> decode(value: String?): T? {
        if (value == null) return null
        return try {
            json.decodeFromString<T>(value)
        } catch (e: SerializationException) {
            Timber.w(e, "Discarding unreadable cart abandonment data")
            null
        } catch (e: IllegalArgumentException) {
            Timber.w(e, "Discarding unreadable cart abandonment data")
            null
        }
    }

    companion object {
        private const val SNAPSHOT_KEY = "com.attentive.androidsdk.cartAbandonment.snapshot"
        private const val VIEWS_KEY = "com.attentive.androidsdk.cartAbandonment.productViews"
        private const val PURCHASED_KEY = "com.attentive.androidsdk.cartAbandonment.hasPurchased"
        internal const val MAX_TRACKED_VIEWS = 100
    }
}
