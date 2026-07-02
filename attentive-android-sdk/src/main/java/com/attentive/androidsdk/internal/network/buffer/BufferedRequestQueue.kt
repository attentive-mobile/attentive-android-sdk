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
     * Flips pending rows older than [cutoffMs] to ready. Call once on SDK init with the
     * process-start wall-clock time as the cutoff: rows older than that are orphans from
     * a prior process; rows newer are still in-flight in this process and must not be
     * touched (would race the original call and double-send on replay).
     */
    suspend fun markAllReadyOlderThan(cutoffMs: Long): Int

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

    override suspend fun markAllReadyOlderThan(cutoffMs: Long): Int =
        dao.markAllReadyOlderThan(cutoffMs)

    override suspend fun countReady(): Int = dao.countReady()
}
