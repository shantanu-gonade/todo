package com.eulerity.todo.feature.history

import app.cash.turbine.test
import com.eulerity.todo.core.model.Task
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `uiState maps expired tasks to TaskUi and clears isLoading`() = runTest {
        val vm = HistoryViewModel(
            observeExpiredTasks = FakeObserveExpiredTasks(count = 2),
        )
        vm.uiState.test {
            // Skip the initialValue (isLoading=true) if it arrives before the upstream emits.
            val first = awaitItem()
            val state = if (first.isLoading) awaitItem() else first

            assertFalse("isLoading should be false after first domain emission", state.isLoading)
            assertEquals(2, state.tasks.size)
            assertEquals("Expired Task 1", state.tasks[0].title)
            assertEquals("Expired Task 2", state.tasks[1].title)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `empty expired list yields empty tasks and isLoading false`() = runTest {
        val vm = HistoryViewModel(
            observeExpiredTasks = FakeObserveExpiredTasks(count = 0),
        )
        vm.uiState.test {
            val first = awaitItem()
            val state = if (first.isLoading) awaitItem() else first

            assertFalse(state.isLoading)
            assertEquals(0, state.tasks.size)
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
            assertEquals(0, empty.tasks.size)

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
            assertEquals(1, updated.tasks.size)
            assertEquals("New Expired", updated.tasks[0].title)
            cancelAndConsumeRemainingEvents()
        }
    }
}
