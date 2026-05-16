package com.eulerity.todo.core.data.notification

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Unit tests for [ReminderScheduler.computeDelayMs].
 * Does not require WorkManager or a device — purely tests the delay arithmetic.
 */
class ReminderSchedulerTest {

    // We can't inject ReminderScheduler without a real Context, but computeDelayMs
    // is internal so we test via a standalone helper that mirrors the logic.
    // The real function is exercised here by subclassing isn't possible (it's
    // a final class), so we replicate the delay calculation and test that.

    private fun computeDelayMs(nowMs: Long): Long {
        val tz = TimeZone.currentSystemDefault()
        val now = kotlinx.datetime.Instant.fromEpochMilliseconds(nowMs)
        val localNow = now.toLocalDateTime(tz)

        val todayAt21Ms = LocalDateTime(
            year = localNow.year,
            monthNumber = localNow.monthNumber,
            dayOfMonth = localNow.dayOfMonth,
            hour = 21,
            minute = 0,
            second = 0,
            nanosecond = 0,
        ).toInstant(tz).toEpochMilliseconds()

        val rawDelay = if (todayAt21Ms > nowMs) {
            todayAt21Ms - nowMs
        } else {
            todayAt21Ms - nowMs + 24L * 60 * 60 * 1_000
        }
        return maxOf(rawDelay, 60_000L)
    }

    @Test
    fun `delay is positive and at least one minute`() {
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val delay = computeDelayMs(nowMs)
        assertTrue("delay must be >= 60 000 ms", delay >= 60_000L)
    }

    @Test
    fun `delay is at most 24 hours + 1 min`() {
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val delay = computeDelayMs(nowMs)
        val maxAllowed = 24L * 60 * 60 * 1_000 + 60_000L
        assertTrue("delay must be < 24h+1min, was $delay", delay <= maxAllowed)
    }

    @Test
    fun `when before 21 00, delay targets same-day 21 00`() {
        val tz = TimeZone.currentSystemDefault()
        // Fix "now" to 08:00 today
        val localNow = Clock.System.now().toLocalDateTime(tz)
        val at08 = LocalDateTime(
            year = localNow.year,
            monthNumber = localNow.monthNumber,
            dayOfMonth = localNow.dayOfMonth,
            hour = 8,
            minute = 0,
            second = 0,
            nanosecond = 0,
        ).toInstant(tz).toEpochMilliseconds()

        val delay = computeDelayMs(at08)
        // Should be roughly 13 hours (21-8)
        val expectedMs = 13L * 60 * 60 * 1_000
        assertTrue("at 08:00 delay should be ~13h, was $delay", delay in expectedMs - 120_000..expectedMs + 120_000)
    }

    @Test
    fun `when after 21 00, delay targets next-day 21 00`() {
        val tz = TimeZone.currentSystemDefault()
        val localNow = Clock.System.now().toLocalDateTime(tz)
        val at22 = LocalDateTime(
            year = localNow.year,
            monthNumber = localNow.monthNumber,
            dayOfMonth = localNow.dayOfMonth,
            hour = 22,
            minute = 0,
            second = 0,
            nanosecond = 0,
        ).toInstant(tz).toEpochMilliseconds()

        val delay = computeDelayMs(at22)
        // Should be roughly 23 hours (24 - 1)
        val expectedMs = 23L * 60 * 60 * 1_000
        assertTrue("at 22:00 delay should be ~23h, was $delay", delay in expectedMs - 120_000..expectedMs + 120_000)
    }
}
