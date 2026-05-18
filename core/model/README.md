# :core:model

Pure Kotlin domain models. This module has **no Android, Room, or Compose
dependencies**. It is imported freely by every other module.

## Types

### `Task`

```kotlin
data class Task(
    val id: String,
    val title: String,
    val isCompleted: Boolean,
    val createdDate: LocalDate,
    val createdAt: Instant,
    val expiryTime: LocalTime?,
    val category: TaskCategory = TaskCategory.NONE,
)
```

The canonical representation of a todo item. `createdDate` (not `createdAt`) is the
day-grouping key — it determines which "today" a task belongs to. `expiryTime` is
optional; when set, an AlarmManager alarm fires at that time.

### `TaskCategory`

```kotlin
enum class TaskCategory(val label: String, val colorRole: String) {
    NONE("None", "surfaceVariant"),
    PERSONAL("Personal", "tertiary"),
    WORK("Work", "primary"),
    ERRANDS("Errands", "secondary"),
    HEALTH("Health", "tertiaryContainer"),
    HOME("Home", "secondaryContainer"),
}
```

`colorRole` is a string token that maps to a `MaterialTheme.colorScheme` property.
It is resolved to an actual `Color` inside composable scope, keeping this model free
of any UI framework dependency.

### `UserData` / `ThemeMode`

```kotlin
enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserData(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)
```

Holds user preferences. Currently only theme mode, but can be extended without
changing the DataStore schema key.

## Dependencies

None — pure Kotlin only (`kotlinx-datetime` is the only transitive dependency).

## Convention plugin

```kotlin
apply plugin: 'todoapp.kotlin.library'
```
