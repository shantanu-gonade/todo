package com.eulerity.todo.core.common

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.assertEquals

class DefaultDateTimeProviderTest {

    @Test
    fun `currentDay emits today's date on collection`() = runTest {
        val provider = DefaultDateTimeProvider(clock = FakeClock(LocalDate(2026, 5, 14)))
        provider.currentDay.test {
            assertEquals(LocalDate(2026, 5, 14), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `today reflects the injected clock`() {
        val provider = DefaultDateTimeProvider(clock = FakeClock(LocalDate(2026, 1, 1)))
        assertEquals(LocalDate(2026, 1, 1), provider.today())
    }
}
