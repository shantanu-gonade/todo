package com.eulerity.todo.core.domain

import com.eulerity.todo.core.common.DateTimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

/** Minimal test double for [DateTimeProvider] — fixed clock at midnight on [date]. */
class FakeDateTimeProvider(private val date: LocalDate) : DateTimeProvider {
    private val zone = TimeZone.currentSystemDefault()

    override fun now(): Instant = date.atStartOfDayIn(zone)
    override fun today(): LocalDate = date
    override val currentDay: Flow<LocalDate> = MutableStateFlow(date)
}
