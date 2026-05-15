package com.eulerity.todo.core.common

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Single source of truth for "now" and "today". Injected everywhere time is
 * read so tests can substitute a deterministic fake.
 */
interface DateTimeProvider {
    fun now(): Instant
    fun today(): LocalDate
    /** Emits the current day, then re-emits whenever the local day changes. */
    val currentDay: Flow<LocalDate>
}
