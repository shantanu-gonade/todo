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

package com.eulerity.todo.feature.history

import app.cash.turbine.test
import com.eulerity.todo.core.model.Task
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state has isLoading true before first emission`() = runTest {
        val vm = HistoryViewModel(
            observeExpiredTasks = FakeObserveExpiredTasks(count = 0),
        )
        // initialValue is always isLoading=true, tasks=[]
        assertEquals(HistoryUiState(isLoading = true), vm.uiState.value)
    }

    @Test
    fun `uiState maps expired tasks to TaskUi grouped by date and clears isLoading`() = runTest {
        val vm = HistoryViewModel(
            observeExpiredTasks = FakeObserveExpiredTasks(count = 2),
        )
        vm.uiState.test {
            // Skip the initialValue (isLoading=true) if it arrives before the upstream emits.
            val first = awaitItem()
            val state = if (first.isLoading) awaitItem() else first

            assertFalse("isLoading should be false after first domain emission", state.isLoading)
            // All fake tasks share the same date — expect 1 date group with 2 tasks.
            assertEquals(1, state.tasksByDate.size)
            val (_, tasks) = state.tasksByDate.first()
            assertEquals(2, tasks.size)
            assertEquals("Expired Task 1", tasks[0].title)
            assertEquals("Expired Task 2", tasks[1].title)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `empty expired list yields empty tasksByDate and isLoading false`() = runTest {
        val vm = HistoryViewModel(
            observeExpiredTasks = FakeObserveExpiredTasks(count = 0),
        )
        vm.uiState.test {
            val first = awaitItem()
            val state = if (first.isLoading) awaitItem() else first

            assertFalse(state.isLoading)
            assertTrue(state.isEmpty)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `uiState reacts to new emissions from the use case`() = runTest {
        val (useCase, repo) = FakeObserveExpiredTasksWithRepo()
        val vm = HistoryViewModel(observeExpiredTasks = useCase)

        vm.uiState.test {
            // First emission: empty list
            val first = awaitItem()
            val empty = if (first.isLoading) awaitItem() else first
            assertTrue(empty.isEmpty)

            // Push a new task into the repository
            repo.tasksFlow.value = listOf(
                Task(
                    id = "new",
                    title = "New Expired",
                    isCompleted = true,
                    createdDate = LocalDate(2026, 5, 13),
                    createdAt = Instant.fromEpochSeconds(0),
                    expiryTime = null,
                ),
            )

            val updated = awaitItem()
            assertFalse(updated.isLoading)
            assertEquals(1, updated.tasksByDate.size)
            assertEquals("New Expired", updated.tasksByDate.first().second.first().title)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `tasks are sorted by date descending with most recent group first`() = runTest {
        val olderDate = LocalDate(2026, 5, 14)
        val newerDate = LocalDate(2026, 5, 16)
        val (useCase, repo) = FakeObserveExpiredTasksWithRepo()
        val vm = HistoryViewModel(observeExpiredTasks = useCase)

        vm.uiState.test {
            val first = awaitItem()
            if (first.isLoading) awaitItem() // drain loading state

            repo.tasksFlow.value = listOf(
                Task(
                    id = "old",
                    title = "Older task",
                    isCompleted = false,
                    createdDate = olderDate,
                    createdAt = Instant.fromEpochSeconds(100),
                    expiryTime = null,
                ),
                Task(
                    id = "new",
                    title = "Newer task",
                    isCompleted = false,
                    createdDate = newerDate,
                    createdAt = Instant.fromEpochSeconds(200),
                    expiryTime = null,
                ),
            )

            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(2, state.tasksByDate.size)
            // Most recent date first
            assertEquals(newerDate, state.tasksByDate[0].first)
            assertEquals(olderDate, state.tasksByDate[1].first)
            cancelAndConsumeRemainingEvents()
        }
    }
}
