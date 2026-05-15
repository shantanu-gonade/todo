# Phase 3 — Feature: Today

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans.
> Prerequisite: Phase 2 verification gate passed.

**Produces:** the main screen. Strict MVI: immutable `UiState`, sealed `Intent`,
sealed `Effect`, one `onIntent` entry point.

**Package root:** `com.eulerity.todo`. Module path: `feature/today`. Gradle
commands run from `Eulerity/todo/`.

**Compose rules in force this phase** (from `01-architecture-and-design.md` §4):
rule 2 (effects collected lifecycle-aware), rule 3 (sheet derives visibility from
state), rule 5 (list item lambdas stable), rule 6 (`asTaskUi` signature agreement),
rule 7 (`@Preview`s wrap in `TodoTheme`).

---

## Task 3.1: MVI contract — `:feature:today`

**Files:** under
`feature/today/src/main/kotlin/com/eulerity/todo/feature/today/` —
`TodayUiState.kt`, `TodayIntent.kt`, `TodayEffect.kt`. Delete the module's
`Placeholder.kt`.

**Step 1: Write the three contract files** exactly as in
`01-architecture-and-design.md` §4 (`TodayUiState`, `TodayIntent`,
`TodayEffect`), package `com.eulerity.todo.feature.today`.

**Step 2: Compile, then commit**

```bash
git add feature/today
git commit -m "feat(today): define MVI state, intent, and effect contracts"
```

## Task 3.2: ViewModel — `:feature:today`

**Files:**
- Create: `feature/today/src/main/kotlin/com/eulerity/todo/feature/today/TodayViewModel.kt`
- Test: `feature/today/src/test/kotlin/com/eulerity/todo/feature/today/TodayViewModelTest.kt`
- Test helper: `feature/today/src/test/kotlin/com/eulerity/todo/feature/today/MainDispatcherRule.kt`

**Step 1: Write the failing test `TodayViewModelTest.kt`**

```kotlin
package com.eulerity.todo.feature.today

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TodayViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test fun `initial combined state loads today's tasks`() = runTest {
        val vm = TodayViewModel(
            observeTodaysTasks = FakeObserveTodaysTasks(initialCount = 1),
            addTask = FakeAddTask(),
            toggleCompletion = FakeToggleCompletion(),
            deleteTask = FakeDeleteTask(),
        )
        vm.uiState.test {
            val state = awaitItem()   // Turbine's first item is the combined state
            assertEquals(1, state.tasks.size)
            assertTrue(!state.isLoading)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test fun `AddTaskClicked with blank draft surfaces a validation error`() = runTest {
        val vm = TodayViewModel(
            FakeObserveTodaysTasks(), FakeAddTask(), FakeToggleCompletion(), FakeDeleteTask(),
        )
        vm.onIntent(TodayIntent.DraftTitleChanged(""))
        vm.onIntent(TodayIntent.AddTaskClicked)
        vm.uiState.test {
            assertTrue(awaitItem().validationError != null)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test fun `toggling completion emits the haptic effect`() = runTest {
        val vm = TodayViewModel(
            FakeObserveTodaysTasks(initialCount = 1, firstId = "x"),
            FakeAddTask(), FakeToggleCompletion(), FakeDeleteTask(),
        )
        vm.effects.test {
            vm.onIntent(TodayIntent.TaskCompletionToggled("x"))
            assertEquals(TodayEffect.TaskCompletedHaptic, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }
}
```

`MainDispatcherRule` is a standard test rule swapping `Dispatchers.Main` for a
test dispatcher. The four `Fake*` use-case doubles are hand-written in the test
source set.

**Step 2: Run to verify it fails**

Run: `./gradlew :feature:today:testDebugUnitTest --tests "*TodayViewModelTest*"`
Expected: FAIL — `TodayViewModel` does not exist.

**Step 3: Implement `TodayViewModel.kt`**

```kotlin
package com.eulerity.todo.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eulerity.todo.core.domain.AddTaskUseCase
import com.eulerity.todo.core.domain.DeleteTaskUseCase
import com.eulerity.todo.core.domain.ObserveTodaysTasksUseCase
import com.eulerity.todo.core.domain.ToggleTaskCompletionUseCase
import com.eulerity.todo.core.ui.asTaskUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    observeTodaysTasks: ObserveTodaysTasksUseCase,
    private val addTask: AddTaskUseCase,
    private val toggleCompletion: ToggleTaskCompletionUseCase,
    private val deleteTask: DeleteTaskUseCase,
) : ViewModel() {

    private val localState = MutableStateFlow(TodayLocalState())
    private val effectChannel = Channel<TodayEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    val uiState: StateFlow<TodayUiState> =
        combine(observeTodaysTasks(), localState) { tasks, local ->
            TodayUiState(
                tasks = tasks.map { it.asTaskUi() },   // default formatter — Compose rule 6
                isLoading = false,
                addSheetVisible = local.addSheetVisible,
                draftTitle = local.draftTitle,
                draftExpiryTime = local.draftExpiryTime,
                validationError = local.validationError,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TodayUiState(isLoading = true),
        )

    fun onIntent(intent: TodayIntent) {
        when (intent) {
            is TodayIntent.DraftTitleChanged ->
                localState.update { it.copy(draftTitle = intent.value, validationError = null) }
            TodayIntent.OpenAddSheet ->
                localState.update { it.copy(addSheetVisible = true) }
            TodayIntent.AddTaskClicked -> submitDraft()
            TodayIntent.AddSheetDismissed ->
                localState.update { TodayLocalState() }
            is TodayIntent.DraftExpiryTimeChanged ->
                localState.update { it.copy(draftExpiryTime = intent.time) }
            is TodayIntent.TaskCompletionToggled -> toggle(intent.id)
            is TodayIntent.DeleteTask -> viewModelScope.launch { deleteTask(intent.id) }
        }
    }

    private fun submitDraft() = viewModelScope.launch {
        val local = localState.value
        addTask(local.draftTitle, local.draftExpiryTime)
            .onSuccess { localState.update { TodayLocalState() } }
            .onFailure { e ->
                localState.update { it.copy(validationError = e.message ?: "Invalid task") }
            }
    }

    private fun toggle(id: String) = viewModelScope.launch {
        toggleCompletion(id)
        effectChannel.send(TodayEffect.TaskCompletedHaptic)
    }
}

internal data class TodayLocalState(
    val addSheetVisible: Boolean = false,
    val draftTitle: String = "",
    val draftExpiryTime: kotlinx.datetime.LocalTime? = null,
    val validationError: String? = null,
)
```

**Step 4: Run to verify it passes**

Run: `./gradlew :feature:today:testDebugUnitTest --tests "*TodayViewModelTest*"`
Expected: PASS.

**Step 5: Commit**

```bash
git add feature/today
git commit -m "feat(today): add MVI ViewModel with combined state and effects"
```

## Task 3.3: Screen + add-task sheet — `:feature:today`

**Files:** under
`feature/today/src/main/kotlin/com/eulerity/todo/feature/today/` —
`TodayScreen.kt` (stateful `TodayRoute` + stateless `TodayScreen`),
`AddTaskSheet.kt`, `navigation/TodayNavigation.kt`. Test:
`feature/today/src/androidTest/kotlin/com/eulerity/todo/feature/today/TodayScreenTest.kt`.

**Step 1: Write `TodayScreen.kt`**

`TodayRoute` collects `uiState` with `collectAsStateWithLifecycle`, owns
`hiltViewModel()`, and — per Compose rule 2 — consumes `effects` inside a
`LaunchedEffect` using `repeatOnLifecycle(Lifecycle.State.STARTED)` (or
`flowWithLifecycle`), firing `HapticFeedback` on `TaskCompletedHaptic` and a
`SnackbarHost` message on `ShowError`. It renders the stateless
`TodayScreen(uiState, onIntent)`.

`TodayScreen` is a `Scaffold`: `TodoTopAppBar` ("Today") with a History action,
a `FloatingActionButton` sending `TodayIntent.OpenAddSheet`, body shows
`TodoEmptyState` ("Nothing for today yet") when `tasks.isEmpty()` else
`TaskList`. Per Compose rule 5, pass `TaskList` the typed callbacks
`onToggle = { id, checked -> onIntent(TodayIntent.TaskCompletionToggled(id)) }`
and `onDelete = { id -> onIntent(TodayIntent.DeleteTask(id)) }` — defined once
here, not per item. When `uiState.addSheetVisible`, render `AddTaskSheet`.

**Step 2: Write `AddTaskSheet.kt`** — a `ModalBottomSheet`. Per Compose rule 3,
its visibility is driven entirely by `uiState.addSheetVisible`;
`onDismissRequest` sends `TodayIntent.AddSheetDismissed`. Contents: an
`OutlinedTextField` bound to `draftTitle` (showing `validationError` as
`supportingText`), an optional `TimePicker` entry for `draftExpiryTime`, and a
filled "Add" button sending `TodayIntent.AddTaskClicked`.

**Step 3: Write `navigation/TodayNavigation.kt`**

```kotlin
package com.eulerity.todo.feature.today.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.eulerity.todo.feature.today.TodayRoute
import kotlinx.serialization.Serializable

@Serializable data object TodayRouteKey

fun NavGraphBuilder.todayScreen(onNavigateToHistory: () -> Unit) {
    composable<TodayRouteKey> {
        TodayRoute(onNavigateToHistory = onNavigateToHistory)
    }
}
```

**Step 4: Write the failing UI test `TodayScreenTest.kt`**

```kotlin
package com.eulerity.todo.feature.today

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.eulerity.todo.core.designsystem.theme.TodoTheme
import org.junit.Rule
import org.junit.Test

class TodayScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun `empty state is shown when there are no tasks`() {
        composeRule.setContent {
            TodoTheme {
                TodayScreen(uiState = TodayUiState(isLoading = false), onIntent = {})
            }
        }
        composeRule.onNodeWithText("Nothing for today yet").assertIsDisplayed()
    }
}
```

**Step 5: Run it, make it pass** (passes once `TodayScreen` renders
`TodoEmptyState` with that copy), **then commit**

```bash
git add feature/today
git commit -m "feat(today): add screen, add-task sheet, and navigation"
```

## Task 3.4: Phase 3 verification gate

Run: `./gradlew :feature:today:testDebugUnitTest :feature:today:assembleDebug`
Expected: `BUILD SUCCESSFUL`, all unit tests green.

Commit any fixes, then proceed to `07-phase-4-feature-history.md`.
