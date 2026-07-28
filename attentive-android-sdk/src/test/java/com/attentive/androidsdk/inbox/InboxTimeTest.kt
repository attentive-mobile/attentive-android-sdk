package com.attentive.androidsdk.inbox

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class InboxTimeTest {

    // --- parseIso8601ToMillis ---

    @Test
    fun parseIso8601_withZuluOffset_returnsEpochMillis() {
        // 2026-05-01T12:00:00Z → 1777377600000 ms since epoch.
        val ms = InboxTime.parseIso8601ToMillis("2026-05-01T12:00:00Z")
        assertEquals(1777636800000L, ms)
    }

    @Test
    fun parseIso8601_withFractionalSeconds_returnsEpochMillis() {
        val ms = InboxTime.parseIso8601ToMillis("2026-05-01T12:00:00.500Z")
        assertEquals(1777636800500L, ms)
    }

    @Test
    fun parseIso8601_withNumericOffset_returnsEpochMillis() {
        // 2026-05-01T14:00:00+02:00 == 2026-05-01T12:00:00Z
        val ms = InboxTime.parseIso8601ToMillis("2026-05-01T14:00:00+02:00")
        assertEquals(1777636800000L, ms)
    }

    @Test
    fun parseIso8601_returns0_forUnparseableString() {
        assertEquals(0L, InboxTime.parseIso8601ToMillis("not-a-timestamp"))
    }

    @Test
    fun parseIso8601_returns0_forEmptyString() {
        assertEquals(0L, InboxTime.parseIso8601ToMillis(""))
    }

    // --- formatRelative ---

    private val fixedNow = 1_770_000_000_000L // arbitrary; we always pass now explicitly

    @Test
    fun formatRelative_underOneMinute_returnsJustNow() {
        assertEquals("Just now", InboxTime.formatRelative(fixedNow, fixedNow))
        assertEquals("Just now", InboxTime.formatRelative(fixedNow - 30_000, fixedNow))
        assertEquals("Just now", InboxTime.formatRelative(fixedNow - 59_999, fixedNow))
    }

    @Test
    fun formatRelative_underOneHour_returnsMinutesAgo() {
        assertEquals("1m ago", InboxTime.formatRelative(fixedNow - 60_000, fixedNow))
        assertEquals("30m ago", InboxTime.formatRelative(fixedNow - 30 * 60_000, fixedNow))
        assertEquals("59m ago", InboxTime.formatRelative(fixedNow - 59 * 60_000 - 999, fixedNow))
    }

    @Test
    fun formatRelative_underOneDay_returnsHoursAgo() {
        assertEquals("1h ago", InboxTime.formatRelative(fixedNow - 3_600_000, fixedNow))
        assertEquals("5h ago", InboxTime.formatRelative(fixedNow - 5 * 3_600_000, fixedNow))
        assertEquals("23h ago", InboxTime.formatRelative(fixedNow - 23 * 3_600_000, fixedNow))
    }

    @Test
    fun formatRelative_underOneWeek_returnsDaysAgo() {
        assertEquals("1d ago", InboxTime.formatRelative(fixedNow - 86_400_000, fixedNow))
        assertEquals("6d ago", InboxTime.formatRelative(fixedNow - 6 * 86_400_000, fixedNow))
    }

    @Test
    fun formatRelative_overOneWeek_returnsAbsoluteMonthDay() {
        // 8 days before fixedNow — should render "MMM d" in the default locale.
        val eightDaysAgo = fixedNow - 8 * 86_400_000L
        val expected = SimpleDateFormat("MMM d", Locale.getDefault())
            .apply { timeZone = TimeZone.getDefault() }
            .format(java.util.Date(eightDaysAgo))
        assertEquals(expected, InboxTime.formatRelative(eightDaysAgo, fixedNow))
    }

    @Test
    fun formatRelative_boundary_atExactlyOneMinute_returnsMinutesAgo() {
        // Exactly 60_000ms diff must roll into "1m ago", not "Just now".
        assertEquals("1m ago", InboxTime.formatRelative(fixedNow - 60_000, fixedNow))
    }
}
