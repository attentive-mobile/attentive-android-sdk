package com.attentive.androidsdk.internal.network.buffer

import androidx.annotation.RestrictTo
import androidx.room.Entity
import androidx.room.PrimaryKey

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Entity(tableName = "buffered_requests")
data class BufferedRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val method: String,
    val contentType: String,
    val body: ByteArray,
    val createdAtMs: Long,
    val attemptCount: Int = 0,
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
            attemptCount == other.attemptCount
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + method.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + body.contentHashCode()
        result = 31 * result + createdAtMs.hashCode()
        result = 31 * result + attemptCount
        return result
    }
}
