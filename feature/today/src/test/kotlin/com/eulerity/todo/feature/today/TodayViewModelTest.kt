package com.eulerity.todo.feature.today

import app.cash.turbine.test
import com.eulerity.todo.core.domain.AddTaskUseCase
import com.eulerity.todo.core.domain.ObserveTodaysTasksUseCase
import com.eulerity.todo.core.domain.UpdateTaskUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class TodayViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val fakeDateTime = FakeDateTimeProvider()

    private fun buildVm(
        tasks: ObserveTodaysTasksUseCase = FakeObserveTodaysTasks(),
        addTask: AddTaskUseCase = FakeAddTask(fakeDateTime),
        updateTask: UpdateTaskUseCase = FakeUpdateTask(fakeDateTime),
    ) = TodayViewModel(
        observeTodaysTasks = tasks,
        addTask = addTask,
        updateTask = updateTask,
        toggleCompletion = FakeToggleCompletion(),
        deleteTask = FakeDeleteTask(),
        dateTimeProvider = fakeDateTime,
    )

    // -----------------------------------------------------------------------
    // Existing tests
    // -----------------------------------------------------------------------

    @Test fun `initial combined state loads today's tasks`() = runTest {
        val vm = buildVm(tasks = FakeObserveTodaysTasks(initialCount = 1))
        vm.uiState.test {
            val state = awaitItem()
            if (state.isLoading) {
                val combined = awaitItem()
                assertEquals(1, combined.tasks.size)
                assertFalse(combined.isLoading)
            } else {
                assertEquals(1, state.tasks.size)
                assertFalse(state.isLoading)
            }
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test fun `AddTaskClicked with blank draft surfaces a validation error`() = runTest {
        val vm = buildVm()
        vm.uiState.test {
            awaitItem()
            vm.onIntent(TodayIntent.DraftTitleChanged(""))
            vm.onIntent(TodayIntent.AddTaskClicked)
            assertNotNull(awaitItem().validationError)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test fun `toggling completion emits the haptic effect`() = runTest {
        val vm = buildVm(tasks = FakeObserveTodaysTasks(initialCount = 1, firstId = "x"))
        vm.effects.test {
            vm.onIntent(TodayIntent.TaskCompletionToggled("x"))
            assertEquals(TodayEffect.TaskCompletedHaptic, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test fun `OpenAddSheet sets addSheetVisible true`() = runTest {
        val vm = buildVm()
        vm.uiState.test {
            awaitItem()
            vm.onIntent(TodayIntent.OpenAddSheet)
            assertTrue(awaitItem().addSheetVisible)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test fun `AddSheetDismissed resets local state`() = runTest {
        val vm = buildVm()
        vm.uiState.test {
            awaitItem()
            vm.onIntent(TodayIntent.OpenAddSheet)
            awaitItem()
            vm.onIntent(TodayIntent.DraftTitleChanged("some text"))
            awaitItem()
            vm.onIntent(TodayIntent.AddSheetDismissed)
            val state = awaitItem()
            assertFalse(state.addSheetVisible)
            assertEquals("", state.draftTitle)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test fun `successful add task clears the sheet`() = runTest {
        val vm = buildVm()
        vm.uiState.test {
            awaitItem()
            vm.onIntent(TodayIntent.OpenAddSheet)
            vm.onIntent(TodayIntent.DraftTitleChanged("Buy milk"))
            vm.onIntent(TodayIntent.AddTaskClicked)
            var state = awaitItem()
            while (state.addSheetVisible || state.draftTitle.isNotEmpty()) {
                state = awaitItem()
            }
            assertFalse(state.addSheetVisible)
            assertEquals("", state.draftTitle)
            assertNull(state.validationError)
            cancelAndConsumeRemainingEvents()
        }
    }

    // -----------------------------------------------------------------------
    // Task-editing tests (spec E1-E5)
    // -----------------------------------------------------------------------

    @Test fun `EditTaskClicked with known id opens sheet in edit mode`() = runTest {
        val vm = buildVm(tasks = FakeObserveTodaysTasks(initialCount = 1, firstId = "t1"))
        vm.uiState.test {
            // Wait for the task list to be populated before dispatching the intent.
            var state = awaitItem()
            while (state.tasks.isEmpty()) state = awaitItem()

            vm.onIntent(TodayIntent.EditTaskClicked("t1"))
            val editState = awaitItem()

            assertTrue(editState.addSheetVisible)
            assertEquals("t1", editState.editingTaskId)
            // Draft title pre-populated from the fake task ("Task t1")
            assertEquals("Task t1", editState.draftTitle)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test fun `EditTaskClicked with unknown id does not open sheet`() = runTest {
        val vm = buildVm()
        vm.uiState.test {
            awaitItem() // initial state
            vm.onIntent(TodayIntent.EditTaskClicked("does-not-exist"))
            // No new emission expected for an unknown id — the ViewModel returns early.
            // We give it a moment and verify the sheet is still closed.
            expectNoEvents()
            assertFalse(vm.uiState.value.addSheetVisible)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test fun `SaveEditClicked with blank title surfaces validation error`() = runTest {
        val vm = buildVm(tasks = FakeObserveTodaysTasks(initialCount = 1, firstId = "t2"))
        vm.uiState.test {
            var state = awaitItem()
            while (state.tasks.isEmpty()) state = awaitItem()

            vm.onIntent(TodayIntent.EditTaskClicked("t2"))
            awaitItem() // edit-mode open

            vm.onIntent(TodayIntent.DraftTitleChanged(""))
            awaitItem() // draft cleared

            vm.onIntent(TodayIntent.SaveEditClicked)
            val errorState = awaitItem()
            assertNotNull(errorState.validationError)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test fun `SaveEditClicked emits ShowMessage effect on success`() = runTest {
        // Share one repo so observeTodaysTasks and updateTask see the same tasks.
        val (repo, observeUC) = FakeRepoWithTasks(initialCount = 1, firstId = "t3")
        val vm = buildVm(
            tasks = observeUC,
            updateTask = FakeUpdateTask(fakeDateTime, repo),
        )

        vm.uiState.test {
            // Advance past Loading → consume the first emission with tasks.
            var state = awaitItem()
            while (state.tasks.isEmpty()) state = awaitItem()

            vm.onIntent(TodayIntent.EditTaskClicked("t3"))
            awaitItem() // sheet opens

            vm.onIntent(TodayIntent.DraftTitleChanged("Updated title"))

            vm.effects.test {
                vm.onIntent(TodayIntent.SaveEditClicked)
                assertEquals(TodayEffect.ShowMessage("Task updated"), awaitItem())
                cancelAndConsumeRemainingEvents()
            }
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test fun `AddSheetDismissed in edit mode clears editingTaskId`() = runTest {
        val vm = buildVm(tasks = FakeObserveTodaysTasks(initialCount = 1, firstId = "t4"))
        vm.uiState.test {
            var state = awaitItem()
            while (state.tasks.isEmpty()) state = awaitItem()

            vm.onIntent(TodayIntent.EditTaskClicked("t4"))
            awaitItem() // sheet open

            vm.onIntent(TodayIntent.AddSheetDismissed)
            val dismissedState = awaitItem()

            assertFalse(dismissedState.addSheetVisible)
            assertNull(dismissedState.editingTaskId)
            cancelAndConsumeRemainingEvents()
        }
    }
}
