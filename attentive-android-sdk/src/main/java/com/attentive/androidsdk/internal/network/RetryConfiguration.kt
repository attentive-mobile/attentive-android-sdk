package com.attentive.androidsdk.internal.network

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
data class RetryConfiguration(
    val initialDelayMs: Long = 1_000L,
    val maxRetries: Int = 5,
    val jitterRangeMs: LongRange = -500L..500L,
    val maxCumulativeDelayMs: Long = 300_000L,
)
