package com.attentive.androidsdk.internal.network.buffer

import androidx.annotation.RestrictTo
import androidx.room.withTransaction

@RestrictTo(RestrictTo.Scope.LIBRARY)
interface BufferedRequestQueue {
    /**
     * Inserts the entity (initial state: `pending=true`), evicts the oldest rows if over
     * [maxEntries], returns the inserted row id.
     */
    suspend fun enqueue(
        entity: BufferedRequestEntity,
        maxEntries: Int
    ): Long

    /** Returns rows ready to replay (`pending=false`). */
    suspend fun peekOldestReady(limit: Int): List<BufferedRequestEntity>

    suspend fun deleteByIds(ids: List<Long>)

    /** Flips a single row from `pending=true` to `pending=false`. */
    suspend fun markReady(id: Long)

    /**
     * Flips ALL pending rows to ready. Call once on SDK init: any rows still pending must
     * be orphans of a prior process that died mid-retry.
     */
    suspend fun markAllReady(): Int

    suspend fun countReady(): Int
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
class RoomBufferedRequestQueue(
    private val database: BufferDatabase,
) : BufferedRequestQueue {
    private val dao = database.bufferedRequestDao()

    override suspend fun enqueue(
        entity: BufferedRequestEntity,
        maxEntries: Int
    ): Long =
        database.withTransaction {
            val id = dao.insert(entity)
            val overflow = dao.count() - maxEntries
            if (overflow > 0) dao.deleteOldest(overflow)
            id
        }

    override suspend fun peekOldestReady(limit: Int): List<BufferedRequestEntity> =
        dao.peekOldestReady(limit)

    override suspend fun deleteByIds(ids: List<Long>) = dao.deleteByIds(ids)

    override suspend fun markReady(id: Long) = dao.markReady(id)

    override suspend fun markAllReady(): Int = dao.markAllReady()

    override suspend fun countReady(): Int = dao.countReady()
}
