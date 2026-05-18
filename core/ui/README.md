# :core:ui

Shared presentation-layer components and the `TaskUi` presentation model. Both
feature modules import from here; nothing in this module is feature-specific.

## `TaskUi` — presentation model

```kotlin
data class TaskUi(
    val id: String,
    val title: String,
    val isCompleted: Boolean,
    val category: TaskCategory,
    val expiryLabel: String?,     // formatted for display, e.g. "Expires 3:30 PM"
    val expiryTime: LocalTime?,   // raw value for edit pre-population
    val createdDate: LocalDate,
    val isExpired: Boolean,
)
```

`TaskUi` is intentionally separate from the domain `Task` to:

- hold derived display strings (`expiryLabel`) without polluting the domain model
- carry the `isExpired` flag (computed at mapping time from current time)
- avoid exposing raw `Instant`/`LocalTime` fields directly to composables

### Mapper

`Task.asTaskUi(now: LocalTime)` is a pure extension function in `TaskMapper.kt`:

```kotlin
fun Task.asTaskUi(nowLocalTime: LocalTime): TaskUi = TaskUi(
    id = id,
    title = title,
    isCompleted = isCompleted,
    category = category,
    expiryLabel = expiryTime?.to12hLabel(),
    expiryTime = expiryTime,
    createdDate = createdDate,
    isExpired = expiryTime != null && !isCompleted && expiryTime < nowLocalTime,
)

private fun LocalTime.to12hLabel(): String {
    val hour = if (hour % 12 == 0) 12 else hour % 12
    val amPm = if (hour < 12) "AM" else "PM"
    return "Expires %d:%02d %s".format(hour, minute, amPm)
}
```

## `TaskCard`

The primary list item composable. Renders a single `TaskUi` with:

- **4 dp colored left strip** for non-NONE categories (using `TaskCategory.categoryColor()`)
- **Animated title color** via `animateColorAsState` — struck-through and dimmed when completed
- **Expired highlight** — card background switches to `MaterialTheme.colorScheme.errorContainer`
  when `isExpired = true`
- **Tap-to-edit** — tapping the title column triggers `onEdit` if `readOnly = false`
- **Read-only mode** — used in the History screen; tap and checkbox are disabled

```kotlin
@Composable
fun TaskCard(
    task: TaskUi,
    onToggleCompletion: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    readOnly: Boolean = false,
)
```

## `TaskList`

Smart layout composable that adapts based on how many distinct categories are present:

```kotlin
@Composable
fun TaskList(
    tasks: List<TaskUi>,
    onToggleCompletion: (String, Boolean) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onTaskDroppedToCategory: (String, TaskCategory) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
)
```

**Flat layout** (single category or all NONE): tasks in a plain `LazyColumn`.

**Grouped layout** (2+ distinct categories):
- `stickyHeader` for each category section header
- Long-press drag-to-move via `detectDragGesturesAfterLongPress` — dropping a card
  in a different section fires `onTaskDroppedToCategory`

**Swipe-to-delete**: `SwipeToDismissBox` with `enableDismissFromStartToEnd = false`
(end-to-start swipe only) reveals a red delete background before calling `onDelete`.

## Dependencies

```
:core:ui
 ├── :core:model        (Task, TaskCategory)
 ├── :core:designsystem (TodoTheme, category colors)
 └── :core:common       (DateTimeProvider for isExpired computation)
```

## Convention plugin

```kotlin
apply plugin: 'todoapp.android.library'
apply plugin: 'todoapp.android.library.compose'
apply plugin: 'todoapp.android.hilt'
```
