package com.attentive.androidsdk.internal.network.buffer

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
data class BufferConfiguration(
    val maxEntries: Int = 500,
    val batchSize: Int = 20,
    val maxBodyBytes: Int = 256 * 1024,
)

/**
 * Marker tagged on replayed requests so [OfflineBufferInterceptor] can short-circuit
 * its enqueue path and avoid re-buffering a request that already came from the buffer.
 * Using a tag instead of a header keeps this purely client-side and out of network logs.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal object ReplayMarker
