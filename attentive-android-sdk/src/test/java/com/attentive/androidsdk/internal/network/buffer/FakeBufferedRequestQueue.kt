package com.attentive.androidsdk.internal.network.buffer

import java.util.concurrent.atomic.AtomicLong

/** In-memory FIFO queue for tests. Mirrors the semantics of RoomBufferedRequestQueue. */
class FakeBufferedRequestQueue : BufferedRequestQueue {
    private val nextId = AtomicLong(1)
    val entries: MutableList<BufferedRequestEntity> = mutableListOf()

    override suspend fun enqueue(
        entity: BufferedRequestEntity,
        maxEntries: Int
    ) {
        entries.add(entity.copy(id = nextId.getAndIncrement()))
        val overflow = entries.size - maxEntries
        if (overflow > 0) repeat(overflow) { entries.removeAt(0) }
    }

    override suspend fun peekOldest(limit: Int): List<BufferedRequestEntity> = entries.take(limit)

    override suspend fun deleteByIds(ids: List<Long>) {
        val set = ids.toSet()
        entries.removeAll { it.id in set }
    }
}
