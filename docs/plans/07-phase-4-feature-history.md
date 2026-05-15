# Phase 4 — Feature: History

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans.
> Prerequisite: Phase 3 verification gate passed.

**Produces:** the expired-todos view. Smaller than Today — read-only, no add, no
toggle.

**Package root:** `com.eulerity.todo`. Module path: `feature/history`. Gradle
commands run from `Eulerity/todo/`.

**Compose rules in force this phase:** rule 5 (stable list callbacks), rule 6
(`asTaskUi` signature), rule 7 (`@Preview`s wrap in `TodoTheme`).

---

## Task 4.1: MVI contract + ViewModel — `:feature:history`

**Files:** under
`feature/history/src/main/kotlin/com/eulerity/todo/feature/history/` —
`HistoryUiState.kt`, `HistoryViewModel.kt`. Test:
`feature/history/src/test/kotlin/com/eulerity/todo/feature/history/HistoryViewModelTest.kt`.
Delete the module's `Placeholder.kt`.

**Step 1: Write `HistoryUiState.kt`**

```kotlin
package com.eulerity.todo.feature.history

import com.eulerity.todo.core.ui.TaskUi

// History is read-only: there are no user intents beyond navigation, so this
// feature intentionally has no Intent/Effect files. The asymmetry with
// :feature:today is deliberate — do not add an Intent type "for consistency".
data class HistoryUiState(
    val tasks: List<TaskUi> = emptyList(),
    val isLoading: Boolean = true,
)
```

**Step 2: Write the failing test `HistoryViewModelTest.kt`** — `HistoryViewModel`
exposes a `uiState: StateFlow<HistoryUiState>` derived from
`ObserveExpiredTasksUseCase`. Assert it maps domain tasks to `TaskUi` and clears
`isLoading`. Use `MainDispatcherRule` and a hand-written
`FakeObserveExpiredTasks`.

**Step 3: Run to verify it fails, then implement `HistoryViewModel.kt`** — same
`stateIn(SharingStarted.WhileSubscribed(5_000))` pattern as `TodayViewModel`, but
with a single source Flow and no local state:

```kotlin
package com.eulerity.todo.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eulerity.todo.core.domain.ObserveExpiredTasksUseCase
import com.eulerity.todo.core.ui.asTaskUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    observeExpiredTasks: ObserveExpiredTasksUseCase,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> =
        observeExpiredTasks()
            .map { tasks -> HistoryUiState(tasks = tasks.map { it.asTaskUi() }, isLoading = false) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = HistoryUiState(isLoading = true),
            )
}
```

**Step 4: Run the test green, commit**

```bash
git add feature/history
git commit -m "feat(history): add ViewModel observing expired tasks"
```

## Task 4.2: Screen + navigation — `:feature:history`

**Files:** under
`feature/history/src/main/kotlin/com/eulerity/todo/feature/history/` —
`HistoryScreen.kt` (stateful `HistoryRoute` + stateless `HistoryScreen`),
`navigation/HistoryNavigation.kt`.

**Step 1: Write `HistoryScreen.kt`** — `HistoryRoute` collects `uiState` with
`collectAsStateWithLifecycle` and owns `hiltViewModel()`. `HistoryScreen` is a
`Scaffold` with `TodoTopAppBar` ("Expired") and a back navigation icon; body is
`TodoEmptyState` ("No expired tasks") when empty, else a read-only `TaskList`.
Because history is read-only, pass `TaskList` no-op callbacks
(`onToggle = { _, _ -> }`, `onDelete = {}`) — or render a read-only variant of
`TaskCard` without the checkbox/delete affordances. Keep callbacks defined once
(Compose rule 5).

**Step 2: Write `navigation/HistoryNavigation.kt`**

```kotlin
package com.eulerity.todo.feature.history.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.eulerity.todo.feature.history.HistoryRoute
import kotlinx.serialization.Serializable

@Serializable data object HistoryRouteKey

fun NavGraphBuilder.historyScreen(onBack: () -> Unit) {
    composable<HistoryRouteKey> {
        HistoryRoute(onBack = onBack)
    }
}
```

**Step 3: Compile, commit**

```bash
git add feature/history
git commit -m "feat(history): add expired-tasks screen and navigation"
```

## Task 4.3: Phase 4 verification gate

Run: `./gradlew :feature:history:testDebugUnitTest :feature:history:assembleDebug`
Expected: `BUILD SUCCESSFUL`, all tests green.

Commit any fixes, then proceed to `08-phase-5-notifications.md`.
