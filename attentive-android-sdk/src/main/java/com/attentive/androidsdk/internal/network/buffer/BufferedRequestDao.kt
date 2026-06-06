package com.attentive.androidsdk.internal.network.buffer

import androidx.annotation.RestrictTo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Dao
interface BufferedRequestDao {
    @Insert
    suspend fun insert(entity: BufferedRequestEntity): Long

    /** Returns rows ready to be replayed by [FlushWorker] (pending=false). */
    @Query("SELECT * FROM buffered_requests WHERE pending = 0 ORDER BY id ASC LIMIT :limit")
    suspend fun peekOldestReady(limit: Int): List<BufferedRequestEntity>

    @Query("DELETE FROM buffered_requests WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    /** Flips a row from pending=true to pending=false so [FlushWorker] picks it up. */
    @Query("UPDATE buffered_requests SET pending = 0 WHERE id = :id")
    suspend fun markReady(id: Long)

    /**
     * On cold start, flip any pending rows whose `createdAtMs` predates [cutoffMs] to ready.
     * The cutoff is the wall-clock time at SDK init, so rows preflighted by *this* process
     * (which haven't completed yet) are skipped — only orphans from prior processes are flipped.
     */
    @Query("UPDATE buffered_requests SET pending = 0 WHERE pending = 1 AND createdAtMs < :cutoffMs")
    suspend fun markAllReadyOlderThan(cutoffMs: Long): Int

    @Query("SELECT COUNT(*) FROM buffered_requests")
    suspend fun count(): Int

    /** Number of rows ready to replay (excludes in-flight `pending=true` rows). */
    @Query("SELECT COUNT(*) FROM buffered_requests WHERE pending = 0")
    suspend fun countReady(): Int

    @Query(
        "DELETE FROM buffered_requests WHERE id IN " +
            "(SELECT id FROM buffered_requests ORDER BY id ASC LIMIT :overflow)",
    )
    suspend fun deleteOldest(overflow: Int)
}
