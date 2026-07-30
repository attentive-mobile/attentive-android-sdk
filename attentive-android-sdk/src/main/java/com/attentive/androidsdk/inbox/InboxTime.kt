package com.attentive.androidsdk.inbox

import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Time utilities for inbox messages — parsing server ISO-8601 timestamps and
 * formatting them for display. Extracted so both can be unit-tested with a
 * deterministic clock instead of relying on `System.currentTimeMillis()`.
 */
internal object InboxTime {
    private val ISO_PATTERNS = arrayOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
    )

    /**
     * Parses an ISO-8601 timestamp (e.g. "2026-05-01T12:00:00Z" or with
     * fractional seconds and any offset) into epoch millis. Returns 0L for
     * inputs the parser cannot handle.
     */
    fun parseIso8601ToMillis(iso: String): Long {
        for (pattern in ISO_PATTERNS) {
            try {
                val parsed = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(iso)
                if (parsed != null) return parsed.time
            } catch (_: Exception) { /* try next */ }
        }
        Timber.w("Failed to parse timestamp: $iso")
        return 0L
    }

    /**
     * Renders [timestamp] as a coarse relative string ("Just now", "Nm ago",
     * "Nh ago", "Nd ago") for the first week, then absolute "MMM d" thereafter.
     * [now] defaults to the wall clock; pass it in for deterministic tests.
     */
    fun formatRelative(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        val diff = now - timestamp
        return when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            diff < 604_800_000 -> "${diff / 86_400_000}d ago"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
