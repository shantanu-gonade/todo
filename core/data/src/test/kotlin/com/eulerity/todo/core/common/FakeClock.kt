package com.eulerity.todo.core.common

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

// Test double — copy in core:data test sources so no cross-module test dependency needed.
class FakeClock(private var day: LocalDate) : Clock {
    fun advanceToNextDay() {
        day = LocalDate.fromEpochDays(day.toEpochDays() + 1)
    }

    override fun now(): Instant = day.atStartOfDayIn(TimeZone.currentSystemDefault())
}
