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

package com.eulerity.todo.core.data

import app.cash.turbine.test
import com.eulerity.todo.core.common.FakeClock
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OfflineTaskRepositoryTest {

    /**
     * THE headline test. Proves the today-only constraint works end-to-end:
     * a task added today is present in observeTodaysTasks() and absent after
     * the day rolls over, then appears in observeExpiredTasks().
     */
    @Test
    fun `a task added today disappears from today and appears in history tomorrow`() = runTest {
        val dao = FakeTaskDao()
        val dateTimeProvider = FakeDateTimeProvider(FakeClock(LocalDate(2026, 5, 14)))
        val repo = OfflineTaskRepository(dao, dateTimeProvider, FakeTaskExpiryScheduler())

        repo.addTask("ship the build", expiryTime = null)

        repo.observeTodaysTasks().test {
            assertEquals(1, awaitItem().size)   // present today
            dateTimeProvider.rollToNextDay()    // simulate midnight
            assertTrue(awaitItem().isEmpty())   // gone from "today"
            cancelAndConsumeRemainingEvents()
        }

        repo.observeExpiredTasks().test {
            assertEquals(1, awaitItem().size)   // now in history
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setCompleted toggles the completion flag`() = runTest {
        val dao = FakeTaskDao()
        val dateTimeProvider = FakeDateTimeProvider(FakeClock(LocalDate(2026, 5, 14)))
        val repo = OfflineTaskRepository(dao, dateTimeProvider, FakeTaskExpiryScheduler())

        repo.addTask("buy milk", expiryTime = null)

        var taskId: String? = null
        repo.observeTodaysTasks().test {
            val tasks = awaitItem()
            assertEquals(1, tasks.size)
            taskId = tasks.first().id
            cancelAndConsumeRemainingEvents()
        }

        repo.setCompleted(taskId!!, true)

        repo.observeTodaysTasks().test {
            assertTrue(awaitItem().first().isCompleted)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `deleteTask removes the task`() = runTest {
        val dao = FakeTaskDao()
        val dateTimeProvider = FakeDateTimeProvider(FakeClock(LocalDate(2026, 5, 14)))
        val repo = OfflineTaskRepository(dao, dateTimeProvider, FakeTaskExpiryScheduler())

        repo.addTask("to delete", expiryTime = null)

        var taskId: String? = null
        repo.observeTodaysTasks().test {
            taskId = awaitItem().first().id
            cancelAndConsumeRemainingEvents()
        }

        repo.deleteTask(taskId!!)

        repo.observeTodaysTasks().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndConsumeRemainingEvents()
        }
    }
}
