# :feature:history

The History screen — a read-only view of tasks from all days prior to today, grouped
by date with sticky section headers and sorted newest-first.

## Design decisions

- **Read-only by intent** — history is an audit trail, not an editable list. No add
  sheet, no swipe-to-delete, no drag-to-move. `TaskCard` is rendered with
  `readOnly = true`.
- **No intents or effects** — `HistoryViewModel` exposes only a `StateFlow<HistoryUiState>`.
  There are no user-driven mutations, so the full MVI intent/effect apparatus is
  omitted. This keeps the ViewModel to ~30 lines.
- **Derived state only** — the ViewModel's sole job is grouping and sorting the domain
  flow. No local mutable state is needed.

## `HistoryUiState`

```kotlin
data class HistoryUiState(
    val tasksByDate: List<Pair<LocalDate, List<TaskUi>>> = emptyList(),
    val isLoading: Boolean = true,
) {
    val isEmpty: Boolean get() = !isLoading && tasksByDate.isEmpty()
}
```

`tasksByDate` is sorted descending by date so the most recent past day appears at
the top.

## `HistoryViewModel`

```kotlin
@HiltViewModel
class HistoryViewModel @Inject constructor(
    observeExpiredTasks: ObserveExpiredTasksUseCase,
    dateTimeProvider: DateTimeProvider,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = observeExpiredTasks()
        .map { tasks ->
            val grouped = tasks
                .map { it.asTaskUi(LocalTime(23, 59)) }   // history = never "active expired"
                .groupBy { it.createdDate }
                .entries
                .sortedByDescending { it.key }
                .map { it.key to it.value }
            HistoryUiState(tasksByDate = grouped, isLoading = false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())
}
```

The `LocalTime(23, 59)` sentinel means no task in history shows as "active expired"
(the red highlight is only meaningful for tasks on today's screen).

## Screen composables

### `HistoryRoute`

Collects `uiState` from the ViewModel. Handles the back navigation callback.

### `HistoryScreen`

Stateless. Renders three states:

- **Loading**: `CircularProgressIndicator` centered on screen
- **Empty**: `TodoEmptyState` with "Nothing here yet" messaging
- **Content**: `LazyColumn` with sticky headers

```kotlin
tasksByDate.forEach { (date, tasks) ->
    stickyHeader(key = "date_$date") {
        DateHeader(text = formatHistoryDate(date))
    }
    items(tasks, key = { it.id }) { task ->
        TaskCard(task = task, readOnly = true, onToggleCompletion = {})
    }
}
```

Sticky headers scroll naturally with the list content and pin at the top of the
viewport as the section scrolls past.

### Date formatting

```kotlin
fun formatHistoryDate(date: LocalDate): String =
    "%s, %s %d".format(
        date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
        date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
        date.dayOfMonth,
    )
// → "Sat, May 16"
```

## Navigation

```kotlin
@Serializable data object HistoryRouteKey

fun NavGraphBuilder.historyScreen(onBack: () -> Unit) {
    composable<HistoryRouteKey> {
        HistoryRoute(onBack = onBack)
    }
}
```

## Dependencies

Same as `:feature:today` (provided by `todoapp.android.feature` convention plugin):

```
:feature:history
 ├── :core:ui
 ├── :core:designsystem
 ├── :core:domain
 ├── :core:model
 └── :core:common
```

## Convention plugin

```kotlin
apply plugin: 'todoapp.android.feature'
apply plugin: 'todoapp.android.library.compose'
```
