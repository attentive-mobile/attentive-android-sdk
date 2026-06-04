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

    @Query("SELECT * FROM buffered_requests ORDER BY id ASC LIMIT :limit")
    suspend fun peekOldest(limit: Int): List<BufferedRequestEntity>

    @Query("DELETE FROM buffered_requests WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM buffered_requests")
    suspend fun count(): Int

    @Query(
        "DELETE FROM buffered_requests WHERE id IN " +
            "(SELECT id FROM buffered_requests ORDER BY id ASC LIMIT :overflow)",
    )
    suspend fun deleteOldest(overflow: Int)
}
