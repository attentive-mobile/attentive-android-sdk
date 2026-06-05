package com.attentive.androidsdk.internal.network.buffer

import java.util.concurrent.atomic.AtomicLong

/** In-memory FIFO queue for tests. Mirrors the semantics of RoomBufferedRequestQueue. */
class FakeBufferedRequestQueue : BufferedRequestQueue {
    private val nextId = AtomicLong(1)
    val entries: MutableList<BufferedRequestEntity> = mutableListOf()

    override suspend fun enqueue(
        entity: BufferedRequestEntity,
        maxEntries: Int
    ): Long {
        val id = nextId.getAndIncrement()
        entries.add(entity.copy(id = id, pending = true))
        val overflow = entries.size - maxEntries
        if (overflow > 0) repeat(overflow) { entries.removeAt(0) }
        return id
    }

    override suspend fun peekOldestReady(limit: Int): List<BufferedRequestEntity> =
        entries.filter { !it.pending }.take(limit)

    override suspend fun deleteByIds(ids: List<Long>) {
        val set = ids.toSet()
        entries.removeAll { it.id in set }
    }

    override suspend fun markReady(id: Long) {
        val idx = entries.indexOfFirst { it.id == id }
        if (idx >= 0) entries[idx] = entries[idx].copy(pending = false)
    }

    override suspend fun markAllReady(): Int {
        var n = 0
        for (i in entries.indices) {
            if (entries[i].pending) {
                entries[i] = entries[i].copy(pending = false)
                n++
            }
        }
        return n
    }

    override suspend fun countReady(): Int = entries.count { !it.pending }

    /** Test helper: enqueue a row and immediately mark it ready (simulating retry-exhausted state). */
    suspend fun enqueueReady(
        entity: BufferedRequestEntity,
        maxEntries: Int
    ): Long {
        val id = enqueue(entity, maxEntries)
        markReady(id)
        return id
    }
}
