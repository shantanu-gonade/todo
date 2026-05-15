# Architecture & Design — Today-Only Todo App

**Date:** 2026-05-14
**Status:** Approved
**App location:** `Eulerity/todo/`  ·  **Package:** `com.eulerity.todo`
**Companion:** `00-PRD.md` (product requirements), `02-plan-index.md` (build plan)

---

## 1. Approach

The app follows the architecture of Google's **Now in Android** reference app,
scaled to this app's real surface area: a multi-module Gradle build, convention
plugins in `build-logic`, MVI feature modules, an offline-first data layer with
Room, Hilt for dependency injection, and Compose with Material 3.

Three approaches were considered. **Approach A — NIA-faithful, scaled to app
size** was chosen: eleven modules, each with a distinct responsibility, with
add-task implemented as a modal sheet inside `:feature:today` rather than its own
module. Approach B (split every concern into its own module, `api`/`impl` per
feature) crosses into over-engineering for a todo app. Approach C (skip
convention plugins) loses the single-source-of-truth build story.

The exercise says a small clean implementation is acceptable. This build goes
further to demonstrate production architecture, while keeping every structural
choice defensible in an interview.

## 2. Module Structure

Eleven Gradle modules, strict downward-only dependencies. Root project name is
`todo`; every module namespace is under `com.eulerity.todo.*`.

```
:app  ──────────────► :feature:today, :feature:history
                      :core:designsystem, :core:ui, :core:data, :core:common

:feature:today ─────► :core:{ui, designsystem, domain, model, common}
:feature:history ───► :core:{ui, designsystem, domain, model, common}

:core:domain ───────► :core:{data, model, common}
:core:data ─────────► :core:{database, datastore, model, common}
:core:database ─────► :core:model
:core:datastore ────► :core:model
:core:ui ───────────► :core:{designsystem, model}
:core:designsystem ─► (no internal dependencies)
:core:model ────────► (pure Kotlin, no dependencies)
:core:common ───────► :core:model
```

`build-logic/convention` is an included build providing nine convention plugins:
`todoapp.android.application`, `.android.library`,
`.android.application.compose`, `.android.library.compose`, `.android.hilt`,
`.android.room`, `.android.feature`, `.jvm.library`, and `.android.test`. Every
module's `build.gradle.kts` becomes three to six lines.

Boundary rules: features never depend on each other or on `:app`. `:core:domain`
is the only entry point features call for business logic. `:core:model` and
`:core:domain` are pure JVM modules — unit-testable with no Android runtime.

### Module responsibilities

| Module | Namespace | Responsibility |
|---|---|---|
| `:app` | `com.eulerity.todo` | Entry point, `MainActivity`, `NavHost`, `Application`, app-level DI |
| `:core:model` | `com.eulerity.todo.core.model` | Pure Kotlin domain types: `Task`, `ThemeMode`, `UserData` |
| `:core:common` | `com.eulerity.todo.core.common` | `DateTimeProvider` (Clock abstraction), dispatchers, `Result` helpers |
| `:core:database` | `com.eulerity.todo.core.database` | Room: `TaskEntity`, `TaskDao`, `TodoDatabase`, type converters |
| `:core:datastore` | `com.eulerity.todo.core.datastore` | DataStore Preferences: theme mode, exposed as `Flow<UserData>` |
| `:core:data` | `com.eulerity.todo.core.data` | `TaskRepository`, `UserDataRepository`; entity↔domain mapping; reminder worker |
| `:core:domain` | `com.eulerity.todo.core.domain` | Use cases — pure, Flow/suspend returning |
| `:core:designsystem` | `com.eulerity.todo.core.designsystem` | Material 3 theme, color, typography, reusable components |
| `:core:ui` | `com.eulerity.todo.core.ui` | Shared composables that depend on `:core:model` |
| `:feature:today` | `com.eulerity.todo.feature.today` | Today screen + add-task modal sheet (MVI) |
| `:feature:history` | `com.eulerity.todo.feature.history` | Expired/past todos screen (MVI) |

## 3. The Day-Reset Strategy

This is the core of the exercise. The chosen approach is **query-time filtering
with a reactive day provider** — not a midnight deletion job.

Each task row stores a `createdDate: LocalDate`. A `DateTimeProvider` in
`:core:common` exposes the current day as a Flow that re-emits when the day rolls
over (computed delay to next local midnight) and on `ACTION_TIME_CHANGED` /
`ACTION_TIMEZONE_CHANGED` / `ACTION_DATE_CHANGED` broadcasts:

```kotlin
interface DateTimeProvider {
    fun now(): Instant
    fun today(): LocalDate
    val currentDay: Flow<LocalDate>   // re-emits when the local day changes
}
```

The repository composes the DAO query with the day Flow:

```kotlin
override fun observeTodaysTasks(): Flow<List<Task>> =
    dateTimeProvider.currentDay.flatMapLatest { day ->
        taskDao.observeTasksForDate(day).map { it.map(TaskEntity::asDomain) }
    }
```

When the day flips while the app is open, `flatMapLatest` swaps to the new query
and the list clears itself.

Why this beats a deletion job:

- **Correct even if the app is never opened.** Old tasks stop matching the
  query. A `WorkManager` job can be deferred indefinitely by Doze or simply not
  run because the app was not launched.
- **Non-destructive.** Rows persist, which the "view expired todos" feature
  requires. A deletion job would fight that feature.
- **Resilient to clock and timezone changes.** "Today" is always recomputed from
  the current zone. A job keyed to a fixed timestamp can wipe the wrong day.
- **Trivially testable.** Inject a fake `DateTimeProvider`, advance it to
  tomorrow, assert the list emptied.

Expiration is a property of the query, not a side effect of a scheduled job.
`WorkManager` still appears — but only for the optional end-of-day notification,
which is a genuine time-triggered side effect.

## 4. MVI & UI Layer

Each feature follows strict MVI with three types: an immutable `UiState`, a
sealed `Intent` for every user action (flowing up), and a sealed `Effect` for
one-shot events such as haptics and snackbars (not state).

```kotlin
data class TodayUiState(
    val tasks: List<TaskUi> = emptyList(),
    val isLoading: Boolean = true,
    val addSheetVisible: Boolean = false,
    val draftTitle: String = "",
    val draftExpiryTime: LocalTime? = null,
    val validationError: String? = null,
)

sealed interface TodayIntent {
    data class DraftTitleChanged(val value: String) : TodayIntent
    data object OpenAddSheet : TodayIntent
    data object AddTaskClicked : TodayIntent
    data object AddSheetDismissed : TodayIntent
    data class DraftExpiryTimeChanged(val time: LocalTime?) : TodayIntent
    data class TaskCompletionToggled(val id: String) : TodayIntent
    data class DeleteTask(val id: String) : TodayIntent
}

sealed interface TodayEffect {
    data object TaskCompletedHaptic : TodayEffect
    data class ShowError(val message: String) : TodayEffect
}
```

The `@HiltViewModel` exposes `StateFlow<TodayUiState>`, a `Channel`-backed
`Flow<TodayEffect>`, and a single `onIntent(intent)` entry point. State is
produced by combining domain use-case Flows with local UI state via `combine` +
`stateIn(WhileSubscribed(5_000))`.

UI is split into a stateful `TodayRoute` (collects state with
`collectAsStateWithLifecycle`, owns `hiltViewModel()`, handles effects) wrapping
a stateless `TodayScreen(uiState, onIntent)` that is fully previewable and
testable. Material 3 throughout: `Scaffold`, center-aligned top app bar, a FAB
that opens the add-task `ModalBottomSheet`, `Card`-based task rows with an
animated checkbox, dynamic color on API 31+ with a static light/dark fallback,
and a thoughtful empty state.

### Compose correctness rules (from the Compose review)

These rules were identified in a Compose-expert review of the planned code and
are baked into the phase plans:

1. **System bar appearance syncs with the theme.** `TodoTheme` runs a
   `DisposableEffect` setting `isAppearanceLightStatusBars` from `darkTheme`, so
   edge-to-edge does not leave unreadable status-bar icons.
2. **Effects are collected lifecycle-aware.** `TodayRoute` consumes the effect
   Flow inside `repeatOnLifecycle(STARTED)` (via `flowWithLifecycle`), so a
   one-shot haptic never fires while the app is backgrounded.
3. **The bottom sheet derives visibility from state.** `addSheetVisible` in
   `UiState` drives the `ModalBottomSheet`; `onDismissRequest` sends
   `AddSheetDismissed`. The sheet holds no independent visibility state.
4. **Completion scale animates in the draw phase.** `TodoCheckbox` animates via
   `Modifier.graphicsLayer { scaleX = …; scaleY = … }` reading
   `animateFloatAsState`, not by recomposing per frame.
5. **List item lambdas are stable.** `TaskList` receives `onIntent` (or
   remembered lambdas) and `TaskCard` builds the typed intent, so
   recomposition does not allocate fresh lambdas and defeat skipping.
6. **`Task.asTaskUi()` formatting is a pure function** in `:core:ui` taking an
   injected/default formatter — never `LocalContext`. The ViewModel and the
   mapper agree on one signature.
7. **`@Preview`s wrap content in `TodoTheme`** so colors resolve correctly.

## 5. Data Layer & Error Handling

Room is the source of truth. `TaskEntity` stores `id` (UUID string), `title`,
`isCompleted`, `createdDate` (LocalDate as epoch-day Long via type converter),
`createdAt` (Instant), and nullable `expiryTime` (LocalTime). `TaskDao` exposes
`observeTasksForDate(day)`, `observeTasksBeforeDate(day)`, suspend `upsert`,
`updateCompletion`, `deleteById`. The database starts at version 1; the README
documents how schema export and `Migration` objects would extend it.

DataStore Preferences holds the one setting worth persisting — theme mode
(system/light/dark) — exposed as `Flow<UserData>`. This demonstrates clean
data-source separation without a settings screen.

`TaskRepository` lives in `:core:data`, maps `TaskEntity` to `Task`, and composes
the DAO with `DateTimeProvider`. Because the app is fully offline, there is no
network failure surface. Persistence calls return a `Result`; a database failure
becomes a transient `TodayEffect.ShowError` snackbar rather than a crash. Input
validation — blank title, expiry time already past — is handled in the use case
and reflected in `TodayUiState` as inline validation, not exceptions.

Use cases in `:core:domain`: `ObserveTodaysTasksUseCase`,
`ObserveExpiredTasksUseCase`, `AddTaskUseCase`, `ToggleTaskCompletionUseCase`,
`DeleteTaskUseCase`.

## 6. Testing Strategy

Four layers:

- **Domain (pure JVM, fastest).** Use-case tests with a fake `DateTimeProvider`
  and an in-memory fake `TaskRepository`. The headline test: add a task, advance
  the fake clock to tomorrow, assert `observeTodaysTasks` emits empty while
  `observeExpiredTasks` emits the task.
- **Data.** `TaskDao` tests against an in-memory Room database; repository tests
  with a fake DAO and fake `DateTimeProvider` verifying the `flatMapLatest`
  day-rollover behavior.
- **ViewModel.** MVI reducer and intent tests using Turbine for StateFlow and
  effect assertions, with a `MainDispatcherRule`.
- **UI (light).** A couple of Compose UI tests for `TodayScreen` (empty state
  renders, completing a task updates the row), plus `@Preview`s.

Test doubles are hand-written fakes, not mocking libraries — per NIA convention.

## 7. Tech Stack

Kotlin 2.x, AGP 9, Gradle 9.1 (already in the scaffold), Compose BOM 2026.05,
Hilt, Room, DataStore, `androidx.navigation` (Compose), `kotlinx-datetime`,
`kotlinx-serialization`, `kotlinx-coroutines`, WorkManager (notifications only),
Turbine + JUnit4 + Robolectric for tests. `minSdk` corrected to 28 (the scaffold
ships 24), `targetSdk` 36, Compose enabled, toolchains aligned.
`gradle/libs.versions.toml` is the single source for all versions; convention
plugins consume it.

## 8. What Would Be Improved With More Time

Documented here so the README can draw on it: schema migrations with real
version history, per-feature `api`/`impl` splits if the app grew more screens,
a baseline profile for startup performance, screenshot tests, and richer
adaptive layouts for tablets and foldables.
