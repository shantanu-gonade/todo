package com.eulerity.todo.core.common

import app.cash.turbine.test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.Test
import kotlin.test.assertEquals

/** Never-emitting fake — tests the initial-value path without Android plumbing. */
private class FakeDateChangeBroadcaster : DateChangeBroadcaster {
    private val _changes = MutableSharedFlow<Unit>()
    override val changes: Flow<Unit> = _changes

    suspend fun emit() = _changes.emit(Unit)
}

class DefaultDateTimeProviderTest {

    private val zone = TimeZone.UTC
    private val broadcaster = FakeDateChangeBroadcaster()

    private fun provider(day: LocalDate) = DefaultDateTimeProvider(
        clock = FakeClock(day),
        zone = zone,
        dateChangeBroadcaster = broadcaster,
    )

    @Test
    fun `currentDay emits today's date on collection`() = runTest {
        val p = provider(LocalDate(2026, 5, 14))
        p.currentDay.test {
            assertEquals(LocalDate(2026, 5, 14), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `today reflects the injected clock`() {
        val p = provider(LocalDate(2026, 1, 1))
        assertEquals(LocalDate(2026, 1, 1), p.today())
    }
}
