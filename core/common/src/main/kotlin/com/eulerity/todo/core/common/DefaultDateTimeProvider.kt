/*
 * Copyright 2026 Eulerity, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

        return merge(
            selfEmit,
            dateChangeBroadcaster.changes.map { today() },
        ).distinctUntilChanged()
    }
}
