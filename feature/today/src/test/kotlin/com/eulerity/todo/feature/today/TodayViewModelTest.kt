package com.eulerity.todo.feature.today

import app.cash.turbine.test
import com.eulerity.todo.core.domain.AddTaskUseCase
import com.eulerity.todo.core.domain.ObserveTodaysTasksUseCase
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

    // Convenience — all tests share the same fake date/time provider.
    private val fakeDateTime = FakeDateTimeProvider()

    private fun buildVm(
        tasks: ObserveTodaysTasksUseCase = FakeObserveTodaysTasks(),
        addTask: AddTaskUseCase = FakeAddTask(fakeDateTime),
    ) = TodayViewModel(
        observeTodaysTasks = tasks,
        addTask = addTask,
        toggleCompletion = FakeToggleCompletion(),
        deleteTask = FakeDeleteTask(),
        dateTimeProvider = fakeDateTime,
    )

    @Test fun `initial combined state loads today's tasks`() = runTest {
        val vm = buildVm(tasks = FakeObserveTodaysTasks(initialCount = 1))
        vm.uiState.test {
            // WhileSubscribed: subscribing here starts the upstream combine.
            // First emission may be initialValue (isLoading=true, tasks=[]) before the
            // fake repository Flow delivers its item. Skip until we see tasks populated.
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
        // Subscribe first so WhileSubscribed starts the upstream combine before intents run.
        vm.uiState.test {
            awaitItem() // consume initial emission (initialValue or first combined state)
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
        // Subscribe first so WhileSubscribed starts the upstream before the intent runs.
        vm.uiState.test {
            awaitItem() // consume initial emission
            vm.onIntent(TodayIntent.OpenAddSheet)
            assertTrue(awaitItem().addSheetVisible)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test fun `AddSheetDismissed resets local state`() = runTest {
        val vm = buildVm()
        vm.uiState.test {
            awaitItem() // consume initial emission
            vm.onIntent(TodayIntent.OpenAddSheet)
            awaitItem() // sheet-open emission
            vm.onIntent(TodayIntent.DraftTitleChanged("some text"))
            awaitItem() // draft-changed emission
            vm.onIntent(TodayIntent.AddSheetDismissed)
            val state = awaitItem() // dismiss emission
            assertFalse(state.addSheetVisible)
            assertEquals("", state.draftTitle)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test fun `successful add task clears the sheet`() = runTest {
        val vm = buildVm()
        vm.uiState.test {
            awaitItem() // consume initial emission
            vm.onIntent(TodayIntent.OpenAddSheet)
            vm.onIntent(TodayIntent.DraftTitleChanged("Buy milk"))
            vm.onIntent(TodayIntent.AddTaskClicked)
            // Skip intermediate emissions until we get the post-add emission where
            // the sheet has been dismissed.
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
}
