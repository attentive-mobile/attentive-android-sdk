package com.attentive.androidsdk.internal.network.buffer

import androidx.annotation.RestrictTo
import androidx.room.withTransaction

@RestrictTo(RestrictTo.Scope.LIBRARY)
interface BufferedRequestQueue {
    suspend fun enqueue(
        entity: BufferedRequestEntity,
        maxEntries: Int
    )

    suspend fun peekOldest(limit: Int): List<BufferedRequestEntity>

    suspend fun deleteByIds(ids: List<Long>)
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
class RoomBufferedRequestQueue(
    private val database: BufferDatabase,
) : BufferedRequestQueue {
    private val dao = database.bufferedRequestDao()

    override suspend fun enqueue(
        entity: BufferedRequestEntity,
        maxEntries: Int
    ) {
        database.withTransaction {
            dao.insert(entity)
            val overflow = dao.count() - maxEntries
            if (overflow > 0) dao.deleteOldest(overflow)
        }
    }

    override suspend fun peekOldest(limit: Int): List<BufferedRequestEntity> = dao.peekOldest(limit)

    override suspend fun deleteByIds(ids: List<Long>) = dao.deleteByIds(ids)
}
