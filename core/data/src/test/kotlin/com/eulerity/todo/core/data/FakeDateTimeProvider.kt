package com.eulerity.todo.core.data

import com.eulerity.todo.core.common.DateTimeProvider
import com.eulerity.todo.core.common.FakeClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Test double for [DateTimeProvider] backed by a [FakeClock].
 * Call [rollToNextDay] to advance the clock and push a new date to [currentDay].
 */
class FakeDateTimeProvider(private val clock: FakeClock) : DateTimeProvider {
    private val zone = TimeZone.currentSystemDefault()
    private val _currentDay = MutableStateFlow(today())

    override fun now(): Instant = clock.now()

    override fun today(): LocalDate = clock.now().toLocalDateTime(zone).date

    override val currentDay: Flow<LocalDate> = _currentDay

    fun rollToNextDay() {
        clock.advanceToNextDay()
        _currentDay.value = today()
    }
}
