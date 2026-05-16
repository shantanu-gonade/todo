package com.eulerity.todo.core.common

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

class DefaultDateTimeProvider @Inject constructor(
    private val clock: Clock,
    private val zone: TimeZone,
    private val dateChangeBroadcaster: DateChangeBroadcaster,
) : DateTimeProvider {

    override fun now(): Instant = clock.now()

    override fun today(): LocalDate = clock.now().toLocalDateTime(zone).date

    override val currentDay: Flow<LocalDate> by lazy {
        buildCurrentDayFlow()
    }

    private fun buildCurrentDayFlow(): Flow<LocalDate> {
        val selfEmit: Flow<LocalDate> = callbackFlow {
            trySend(today())
            awaitClose { }
        }

        val flows = buildList {
            add(selfEmit)
            add(dateChangeBroadcaster.changes.map { today() })
        }

        return merge(*flows.toTypedArray()).distinctUntilChanged()
    }
}
