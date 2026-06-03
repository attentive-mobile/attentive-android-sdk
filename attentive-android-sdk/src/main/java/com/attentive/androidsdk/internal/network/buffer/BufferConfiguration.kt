package com.attentive.androidsdk.internal.network.buffer

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
data class BufferConfiguration(
    val maxEntries: Int = 500,
    val batchSize: Int = 20,
    val maxBodyBytes: Int = 256 * 1024,
) {
    companion object {
        const val REPLAY_HEADER: String = "X-Attentive-Replay"
        const val REPLAY_HEADER_VALUE: String = "1"
    }
}
