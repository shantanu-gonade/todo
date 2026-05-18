# Architecture

This document explains the architectural decisions, data flow, and component design
in the Eulerity Todo app. The app follows the layered architecture recommended in
[Now in Android](https://github.com/android/nowinandroid) with MVI in the UI layer,
use cases in the domain layer, and reactive repository implementations in the data layer.

---

## Table of Contents

1. [Layered Architecture](#1-layered-architecture)
2. [Module Dependency Graph](#2-module-dependency-graph)
3. [UI Layer — MVI Pattern](#3-ui-layer--mvi-pattern)
4. [Domain Layer — Use Cases](#4-domain-layer--use-cases)
5. [Data Layer — Repositories](#5-data-layer--repositories)
6. [Day-Reset Mechanism](#6-day-reset-mechanism)
7. [Room Database](#7-room-database)
8. [Notification System](#8-notification-system)
9. [Dependency Injection (Hilt)](#9-dependency-injection-hilt)
10. [Navigation](#10-navigation)
11. [Theming and Design System](#11-theming-and-design-system)

---

## 1. Layered Architecture

```
┌──────────────────────────────────────────────────────┐
│                     UI Layer                          │
│  TodayRoute / HistoryRoute  (Jetpack Compose)         │
│  TodayViewModel / HistoryViewModel  (MVI)             │
│  TodayUiState  TodayIntent  TodayEffect               │
├──────────────────────────────────────────────────────┤
│                  Domain Layer                         │
│  AddTaskUseCase         UpdateTaskUseCase             │
│  DeleteTaskUseCase      ToggleTaskCompletionUseCase   │
│  ObserveTodaysTasksUseCase                            │
│  ObserveExpiredTasksUseCase                           │
├──────────────────────────────────────────────────────┤
│                   Data Layer                          │
│  OfflineTaskRepository                               │
│  OfflineFirstUserDataRepository                      │
│  TaskExpiryScheduler  (AlarmManager)                 │
│  ReminderScheduler    (WorkManager)                   │
├──────────────────────────────────────────────────────┤
│               Persistence                             │
│  Room  (todo.db v2)                                  │
│  Preferences DataStore  (theme_mode)                 │
└──────────────────────────────────────────────────────┘
```

Each layer depends only on the layer below it. UI never touches DAOs; domain never
touches Android platform APIs; data never imports Compose. The `core:model` module
sits outside all layers — it is pure Kotlin with no dependencies and is imported freely
by every layer.

---

## 2. Module Dependency Graph

```
app
 ├── feature:today
 │    ├── core:ui
 │    ├── core:designsystem
 │    ├── core:domain
 │    ├── core:model
 │    └── core:common
 ├── feature:history
 │    └── (same as today)
 ├── core:data
 │    ├── core:database
 │    ├── core:datastore
 │    ├── core:model
 │    └── core:common
 └── core:designsystem
      └── core:model
```

`build-logic:convention` is a build-time dependency only; it is not included in the
compiled app.

---

## 3. UI Layer — MVI Pattern

The UI layer uses a strict MVI (Model-View-Intent) contract. Every screen has three
associated types:

| Type | Role |
|---|---|
| `UiState` | Immutable snapshot of everything the screen needs to render |
| `Intent` | User or lifecycle actions the ViewModel responds to |
| `Effect` | One-shot events (haptics, Snackbar) that should not be replayed |

### State production

Each ViewModel produces state by combining a domain flow with a local ephemeral state
flow:

```kotlin
// TodayViewModel (simplified)
private val localState = MutableStateFlow(TodayLocalState())

val uiState: StateFlow<TodayUiState> = combine(
    observeTodaysTasks(),   // upstream: domain flow
    localState,             // local: sheet visibility, draft fields
) { tasks, local ->
    TodayUiState(
        tasks = tasks,
        addSheetVisible = local.addSheetVisible,
        draftTitle = local.draftTitle,
        // …
    )
}.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = TodayUiState(isLoading = true),
)
```

`SharingStarted.WhileSubscribed(5_000)` keeps the upstream alive for 5 seconds after
the last subscriber disappears — long enough to survive a configuration change without
restarting the flow.

### Intent dispatch

All user interactions funnel through a single `onIntent(TodayIntent)` entry point:

```kotlin
fun onIntent(intent: TodayIntent) {
    when (intent) {
        is DraftTitleChanged       -> localState.update { it.copy(draftTitle = intent.title) }
        is OpenAddSheet            -> localState.update { it.copy(addSheetVisible = true) }
        is AddTaskClicked          -> launchAddTask()
        is TaskCompletionToggled   -> launchToggleCompletion(intent.taskId, intent.completed)
        // …
    }
}
```

This single entry point makes the ViewModel trivially testable: feed intents, assert
state and effects.

### One-shot effects

Effects (haptics, Snackbar messages) travel through a `Channel.BUFFERED` so they are
never lost during configuration changes and never re-delivered after the screen returns
to the foreground:

```kotlin
// ViewModel
private val effectChannel = Channel<TodayEffect>(Channel.BUFFERED)
val effects: Flow<TodayEffect> = effectChannel.receiveAsFlow()

// Screen
LaunchedEffect(Unit) {
    viewModel.effects
        .flowWithLifecycle(lifecycle)
        .collect { effect ->
            when (effect) {
                TaskCompletedHaptic -> hapticFeedback.performHapticFeedback(…)
                is ShowError        -> snackbarHostState.showSnackbar(effect.message)
                is ShowMessage      -> snackbarHostState.showSnackbar(effect.message)
            }
        }
}
```

`flowWithLifecycle` suspends collection when the lifecycle drops below `STARTED`,
which means effects are buffered in the Channel rather than dropped. On return to
`STARTED` the screen drains the buffer in order.

### Stateless composables

`TodayScreen` is stateless — it accepts `uiState: TodayUiState` and
`onIntent: (TodayIntent) -> Unit` with no internal `remember` blocks (other than
stable references). This makes it previewable and unit-testable without a ViewModel.

---

## 4. Domain Layer — Use Cases

The domain layer contains thin use-case classes that hold business logic and
validation. They have no Android framework dependencies.

| Use Case | Responsibility |
|---|---|
| `AddTaskUseCase` | Validates title (non-blank) and expiry (not in the past); delegates to repository |
| `UpdateTaskUseCase` | Same validation as Add; cancels old alarm, schedules new one |
| `DeleteTaskUseCase` | Cancels expiry alarm, removes from DB |
| `ToggleTaskCompletionUseCase` | Cancels alarm on completion; reschedules if un-completing |
| `ObserveTodaysTasksUseCase` | Thin delegation to `TaskRepository.observeTodaysTasks()` |
| `ObserveExpiredTasksUseCase` | Thin delegation to `TaskRepository.observeExpiredTasks()` |

Validation returns `Result<Unit>` so the ViewModel can surface errors without
catching exceptions:

```kotlin
class AddTaskUseCase @Inject constructor(…) {
    suspend operator fun invoke(title: String, expiryTime: LocalTime?, category: TaskCategory): Result<Unit> {
        if (title.isBlank()) return Result.failure(IllegalArgumentException("Title cannot be empty"))
        val now = dateTimeProvider.now()
        if (expiryTime != null && expiryTime < dateTimeProvider.today().atTime(expiryTime)) {
            return Result.failure(IllegalArgumentException("Expiry time is in the past"))
        }
        return runCatching { repository.addTask(title, expiryTime, category) }
    }
}
```

---

## 5. Data Layer — Repositories

### OfflineTaskRepository

The repository is the single source of truth. It wraps `TaskDao` (Room) and
`TaskExpiryScheduler` (AlarmManager). All public methods are either `suspend fun` or
return `Flow`.

```
OfflineTaskRepository
 ├── observeTodaysTasks(): Flow<List<Task>>   ← reactive, day-aware
 ├── observeExpiredTasks(): Flow<List<Task>>  ← history screen
 ├── addTask(…)                               ← suspend
 ├── updateTask(…)                            ← suspend
 ├── deleteTask(taskId)                       ← suspend
 └── toggleCompletion(taskId, completed)      ← suspend
```

All domain models (`Task`) are mapped from Room entities (`TaskEntity`) using
extension functions in `core:data/mapper/TaskMapper.kt`. The mapping is pure and
tested independently.

### OfflineFirstUserDataRepository

Wraps `UserPreferencesDataSource` (DataStore) and exposes a `Flow<UserData>` for the
current theme mode. Write operations (`setThemeMode`) are `suspend` functions.

---

## 6. Day-Reset Mechanism

The day-reset is implemented entirely as **query-time filtering**. Tasks are never
deleted when midnight arrives.

### DateTimeProvider

```kotlin
interface DateTimeProvider {
    fun now(): Instant
    fun today(): LocalDate
    val currentDay: Flow<LocalDate>   // emits on every calendar-day change
}
```

`DefaultDateTimeProvider` constructs `currentDay` by merging two sources:

```kotlin
val currentDay: Flow<LocalDate> = merge(
    flow { emit(today()) },          // 1. emit immediately on subscription
    dateChangeBroadcaster.changes    // 2. re-emit on system date-change events
).map { today() }
 .distinctUntilChanged()
```

### DateChangeBroadcaster

`SystemDateChangeBroadcaster` is a `@Singleton` that registers a dynamic
`BroadcastReceiver` for:

- `Intent.ACTION_DATE_CHANGED`
- `Intent.ACTION_TIME_CHANGED`
- `Intent.ACTION_TIMEZONE_CHANGED`

It wraps the receiver in `callbackFlow`, emitting `Unit` on each broadcast. This Flow
is long-lived and shared across all collectors via `shareIn`.

### flatMapLatest in the repository

```kotlin
override fun observeTodaysTasks(): Flow<List<Task>> =
    dateTimeProvider.currentDay.flatMapLatest { day ->
        taskDao.observeTasksForDate(day)
            .map { entities -> entities.map(TaskEntity::asDomain) }
    }
```

When `currentDay` emits a new `LocalDate`, `flatMapLatest` cancels the previous
`observeTasksForDate` flow and subscribes to the new one. The Today screen
automatically shows only today's tasks — no midnight job, no deletion, no data loss.

**Why this is correct:**
- Non-destructive: tasks from previous days remain in the database and are visible in History.
- Always accurate: even if the app is never opened at midnight, the query is correct the next time the app is opened.
- Timezone-safe: `ACTION_TIMEZONE_CHANGED` triggers a re-evaluation even when the wall-clock time hasn't changed.

---

## 7. Room Database

### Schema

```
tasks
 ├── id           TEXT PRIMARY KEY
 ├── title        TEXT NOT NULL
 ├── isCompleted  INTEGER NOT NULL (0/1)
 ├── createdDate  INTEGER NOT NULL  (epoch days)
 ├── createdAt    INTEGER NOT NULL  (epoch milliseconds)
 ├── expiryTime   INTEGER           (second-of-day, nullable)
 └── category     TEXT NOT NULL DEFAULT 'NONE'  (added in v2)
```

### TypeConverters

Room cannot store `kotlinx-datetime` types natively. `Converters.kt` handles the
mapping:

| Kotlin type | Room column type | Conversion |
|---|---|---|
| `LocalDate` | `INTEGER` | `toEpochDays()` / `fromEpochDays()` |
| `Instant` | `INTEGER` | `toEpochMilliseconds()` / `fromEpochMilliseconds()` |
| `LocalTime` | `INTEGER` (nullable) | `toSecondOfDay()` / `fromSecondOfDay()` |

### Migration

**Version 1 → 2** adds the `category` column:

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE tasks ADD COLUMN category TEXT NOT NULL DEFAULT 'NONE'"
        )
    }
}
```

Schema JSON exports are stored in `core/database/schemas/` and are verified in CI
to detect accidental schema drift.

---

## 8. Notification System

The app has two notification channels and two scheduling mechanisms.

### Channels

| Channel ID | Importance | Purpose |
|---|---|---|
| `todo_daily_reminder` | DEFAULT | End-of-day reminder at 21:00 |
| `todo_task_expiry` | HIGH | Per-task expiry alert |

Both channels are created in `TodoApplication.onCreate()` via
`TodoNotificationChannel.ensure(context)`. Creating a channel that already exists
is a no-op on API 26+.

### Per-task expiry — AlarmManager

`AlarmManagerTaskExpiryScheduler` uses `setExactAndAllowWhileIdle` to fire even when
the device is in Doze mode. On API 31+ it checks `canScheduleExactAlarms()` before
scheduling.

Each task's alarm uses `taskId.hashCode()` as the `PendingIntent` request code,
which guarantees a unique pending intent per task and makes cancellation straightforward:

```kotlin
fun cancel(taskId: String) {
    val intent = buildIntent(taskId)
    val pending = PendingIntent.getBroadcast(context, taskId.hashCode(), intent, flags)
    alarmManager.cancel(pending)
    pending.cancel()
}
```

`TaskExpiryReceiver` is the target `BroadcastReceiver`. It posts a `PRIORITY_HIGH`
notification with the task title, using `taskId.hashCode()` as the notification ID.

**Lifecycle of an alarm:**

```
addTask()        → schedule(taskId, title, expiryTime)
updateTask()     → cancel(taskId)  →  schedule(taskId, title, newExpiryTime)
toggleComplete() → cancel(taskId)   [if completing]
deleteTask()     → cancel(taskId)
```

### End-of-day reminder — WorkManager

`ReminderScheduler` enqueues a `OneTimeWorkRequest` targeting `EndOfDayReminderWorker`
with `ExistingWorkPolicy.REPLACE`. The initial delay is computed as:

```kotlin
fun computeDelayMs(): Long {
    val now = dateTimeProvider.now()
    val targetToday = today().atTime(21, 0).toInstant(timeZone)
    val target = if (now < targetToday) targetToday else targetToday + 1.days
    return (target - now).inWholeMilliseconds.coerceAtLeast(60_000)
}
```

This ensures the reminder always fires at 21:00 (or the next day if already past),
with a minimum delay of 1 minute to avoid accidental immediate firing.

`EndOfDayReminderWorker` queries the repository for any incomplete tasks today before
posting the notification, so no reminder is shown if all tasks are already done.

---

## 9. Dependency Injection (Hilt)

Hilt is the DI framework. Convention plugin `todoapp.android.hilt` applies
`com.google.dagger.hilt.android` and `com.google.devtools.ksp` to any module that
needs DI.

### Module overview

| Hilt Module | Provides / Binds |
|---|---|
| `CommonModule` | `DateTimeProvider`, `DateChangeBroadcaster`, `Clock`, `TimeZone`, `@IoDispatcher`, `@DefaultDispatcher` |
| `DatabaseModule` | `TodoDatabase`, `TaskDao` |
| `DataModule` | `TaskRepository` → `OfflineTaskRepository`, `UserDataRepository` → `OfflineFirstUserDataRepository` |
| `NotificationModule` | `TaskExpiryScheduler` → `AlarmManagerTaskExpiryScheduler`, `ReminderScheduler` |

### Scoping

- `@Singleton`: `TodoDatabase`, `TaskDao`, `OfflineTaskRepository`, `SystemDateChangeBroadcaster`, all schedulers.
- `@HiltViewModel`: `TodayViewModel`, `HistoryViewModel` — scoped to the ViewModel lifecycle.
- No custom components are used; the standard `SingletonComponent` and `ViewModelComponent` are sufficient.

### WorkManager integration

`TodoApplication` implements `Configuration.Provider` and injects `HiltWorkerFactory`
so that `EndOfDayReminderWorker` can receive its own `@Inject` constructor dependencies:

```kotlin
@HiltAndroidApp
class TodoApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
```

---

## 10. Navigation

Navigation uses Navigation Compose 2.8+ with type-safe routes backed by
`@Serializable` Kotlin objects. There are no string-based route definitions.

```kotlin
@Serializable data object TodayRouteKey
@Serializable data object HistoryRouteKey
```

`TodoNavHost` is the single navigation host:

```kotlin
NavHost(
    navController = navController,
    startDestination = TodayRouteKey,
) {
    todayScreen(onNavigateToHistory = { navController.navigate(HistoryRouteKey) })
    historyScreen(onBack = { navController.popBackStack() })
}
```

The `NavController` never leaks into ViewModels or feature modules — navigation
callbacks are passed as lambdas. Feature modules expose their graph registration as
extension functions (`NavGraphBuilder.todayScreen()`), keeping navigation logic
co-located with the feature it belongs to.

---

## 11. Theming and Design System

### TodoTheme

`TodoTheme` wraps `MaterialTheme` and applies dynamic color (Android 12+) or the
static `LightColors`/`DarkColors` palette for older devices:

```kotlin
@Composable
fun TodoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
        dynamicColor             -> dynamicLightColorScheme(LocalContext.current)
        darkTheme                -> DarkColors
        else                     -> LightColors
    }
    // sync status bar icon tint with theme
    DisposableEffect(darkTheme) { … }
    MaterialTheme(colorScheme = colorScheme, typography = TodoTypography, content = content)
}
```

### TaskCategory color mapping

`TaskCategory.colorRole` is a string token (`"primary"`, `"secondary"`, etc.) that is
resolved to an actual `Color` inside composable scope:

```kotlin
@Composable
fun TaskCategory.categoryColor(): Color = when (colorRole) {
    "primary"            -> MaterialTheme.colorScheme.primary
    "secondary"          -> MaterialTheme.colorScheme.secondary
    "tertiary"           -> MaterialTheme.colorScheme.tertiary
    "tertiaryContainer"  -> MaterialTheme.colorScheme.tertiaryContainer
    "secondaryContainer" -> MaterialTheme.colorScheme.secondaryContainer
    else                 -> MaterialTheme.colorScheme.surfaceVariant
}
```

Keeping color role as a string in the domain model means the domain layer has zero
Compose or Material dependency — it remains pure Kotlin.

### User-selectable theme

`ThemeMode` (`SYSTEM` / `LIGHT` / `DARK`) is persisted in DataStore and observed in
`MainActivity`:

```kotlin
val userData by userDataRepository.userData.collectAsStateWithLifecycle()
TodoTheme(darkTheme = when (userData.themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT  -> false
    ThemeMode.DARK   -> true
}) { … }
```
