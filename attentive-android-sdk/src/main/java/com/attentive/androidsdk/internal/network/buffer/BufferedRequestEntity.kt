package com.attentive.androidsdk.internal.network.buffer

import androidx.annotation.RestrictTo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A buffered HTTP request awaiting replay.
 *
 * The [pending] flag distinguishes rows that are still being attempted by the original-call
 * [com.attentive.androidsdk.internal.network.RetryInterceptor] (true) from rows that are
 * ready to be replayed by [FlushWorker] (false). [FlushWorker] only picks up `pending=false`
 * rows, so a worker that runs while an original call is mid-retry won't double-send. On cold
 * start any leftover `pending=true` rows are by definition orphaned by a prior process death
 * and get flipped to `pending=false` so they're replayed.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Entity(tableName = "buffered_requests")
data class BufferedRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val method: String,
    val contentType: String,
    val body: ByteArray,
    val createdAtMs: Long,
    val pending: Boolean = true,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BufferedRequestEntity) return false
        return id == other.id &&
            url == other.url &&
            method == other.method &&
            contentType == other.contentType &&
            body.contentEquals(other.body) &&
            createdAtMs == other.createdAtMs &&
            pending == other.pending
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + method.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + body.contentHashCode()
        result = 31 * result + createdAtMs.hashCode()
        result = 31 * result + pending.hashCode()
        return result
    }
}
