# :feature:today

The Today screen — the primary screen of the app. Shows all tasks for the current
calendar day, allows adding, editing, completing, and deleting tasks, and
automatically presents a clean slate when the day rolls over.

## MVI contract

| Type | Purpose |
|---|---|
| `TodayUiState` | Complete rendering snapshot — task list, sheet state, draft fields, validation error |
| `TodayIntent` | All user interactions and lifecycle events |
| `TodayEffect` | One-shot events: haptic feedback, Snackbar messages |

### `TodayUiState`

```kotlin
data class TodayUiState(
    val tasks: List<TaskUi> = emptyList(),
    val isLoading: Boolean = true,
    val addSheetVisible: Boolean = false,
    val editingTaskId: String? = null,
    val draftTitle: String = "",
    val draftExpiryTime: LocalTime? = null,
    val draftCategory: TaskCategory = TaskCategory.NONE,
    val validationError: String? = null,
)
```

`editingTaskId != null` signals edit mode in the sheet. The sheet composable reads
this to switch its title, pre-populate fields, and change the action button label.

### `TodayIntent`

```
DraftTitleChanged(title)          — text field keystroke
DraftCategoryChanged(category)    — chip tap
DraftExpiryTimeChanged(time?)     — time picker confirmed/cleared
OpenAddSheet                      — FAB tap
AddSheetDismissed                 — sheet dismissed (back or tap outside)
AddTaskClicked                    — "Add" button
EditTaskClicked(taskId)           — task title tap
SaveEditClicked                   — "Save" button in edit mode
TaskCompletionToggled(id, value)  — checkbox
DeleteTask(id)                    — swipe-to-delete confirmed
TaskDroppedToCategory(id, cat)    — drag-and-drop to different section
```

### `TodayEffect`

```
TaskCompletedHaptic    — vibrate on check-off
ShowError(message)     — Snackbar with validation error
ShowMessage(message)   — Snackbar with success/info message
```

## ViewModel

`TodayViewModel` merges domain data with ephemeral local state via `combine`:

```kotlin
val uiState = combine(
    observeTodaysTasks().map { tasks -> tasks.map { it.asTaskUi(nowLocalTime()) } },
    localState,
) { tasks, local ->
    TodayUiState(tasks = tasks, addSheetVisible = local.addSheetVisible, …)
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())
```

Effects travel through `Channel.BUFFERED` so they survive configuration changes:

```kotlin
private val effectChannel = Channel<TodayEffect>(Channel.BUFFERED)
val effects = effectChannel.receiveAsFlow()
```

## Screen composables

### `TodayRoute`

Stateful composable. Collects `uiState` and `effects` from the ViewModel:

```kotlin
LaunchedEffect(Unit) {
    viewModel.effects
        .flowWithLifecycle(lifecycle)
        .collect { effect -> /* handle haptic, snackbar */ }
}
```

### `TodayScreen`

Stateless. Receives `uiState` and `onIntent` lambda. Contains the `Scaffold`,
`TaskList`, FAB, and conditionally the `AddEditTaskSheet`.

### `AddEditTaskSheet`

`ModalBottomSheet` with `skipPartiallyExpanded = true`. Dual mode:

- **Add mode**: blank title, no expiry, category NONE
- **Edit mode**: pre-populated from the task being edited via `editingTaskId`

Contains:
- `OutlinedTextField` for title (auto-focused on open)
- `CategoryChipRow` — horizontal `FilterChip` row, tapping selected chip deselects to NONE
- Optional `TimePicker` in a dialog (triggered by an "Add expiry time" chip)
- Bottom padding: `imePadding() + navigationBarsPadding()` + `verticalScroll` so the
  keyboard never obscures the action button

### `CategoryChipRow`

`LazyRow` of `FilterChip` for each `TaskCategory` except NONE. The selected chip
calls `DraftCategoryChanged(TaskCategory.NONE)` when tapped again (deselect).
Each chip uses `TaskCategory.categoryColor()` for its selected container color.

## Navigation

`TodayRouteKey` is a `@Serializable data object`. The feature registers itself via
a `NavGraphBuilder` extension function:

```kotlin
fun NavGraphBuilder.todayScreen(onNavigateToHistory: () -> Unit) {
    composable<TodayRouteKey> {
        TodayRoute(onNavigateToHistory = onNavigateToHistory)
    }
}
```

## Dependencies

Provided by the `todoapp.android.feature` convention plugin:

```
:feature:today
 ├── :core:ui
 ├── :core:designsystem
 ├── :core:domain
 ├── :core:model
 └── :core:common
```

Plus Hilt Navigation Compose and Navigation Compose (transitive from convention plugin).

## Convention plugin

```kotlin
apply plugin: 'todoapp.android.feature'
apply plugin: 'todoapp.android.library.compose'
```
