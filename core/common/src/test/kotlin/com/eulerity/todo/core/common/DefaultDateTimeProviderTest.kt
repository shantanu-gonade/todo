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
