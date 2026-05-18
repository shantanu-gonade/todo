# :core:domain

Business logic and validation use cases. This module sits between the data layer
and the UI layer. It contains **no Android framework code** — use cases are pure
Kotlin classes that can be unit tested on the JVM without Robolectric.

## Why a domain layer?

The domain layer is optional in simple apps, but it pays off here because:

- **Shared validation** — `AddTaskUseCase` and `UpdateTaskUseCase` share the same
  title and expiry-time validation logic. Without a use case the ViewModel would
  duplicate or inline this logic.
- **Testability** — use cases are injected interfaces; faking them in ViewModel tests
  is trivial.
- **Single responsibility** — ViewModels become coordinators, not validators.

## Use cases

| Class | Inputs | Returns | Side effects |
|---|---|---|---|
| `AddTaskUseCase` | `title`, `expiryTime?`, `category` | `Result<Unit>` | Saves to DB, schedules alarm |
| `UpdateTaskUseCase` | `id`, `title`, `expiryTime?`, `category` | `Result<Unit>` | Updates DB, cancels/reschedules alarm |
| `DeleteTaskUseCase` | `taskId` | `Unit` | Deletes from DB, cancels alarm |
| `ToggleTaskCompletionUseCase` | `taskId`, `completed` | `Unit` | Updates completion, cancels/restores alarm |
| `ObserveTodaysTasksUseCase` | — | `Flow<List<Task>>` | None (read-only) |
| `ObserveExpiredTasksUseCase` | — | `Flow<List<Task>>` | None (read-only) |

## Validation

`AddTaskUseCase` and `UpdateTaskUseCase` share the same validation rules:

```kotlin
if (title.isBlank())
    return Result.failure(IllegalArgumentException("Title cannot be empty"))

if (expiryTime != null) {
    val nowTime = dateTimeProvider.now().toLocalDateTime(timeZone).time
    if (expiryTime <= nowTime)
        return Result.failure(IllegalArgumentException("Expiry time must be in the future"))
}
```

Returning `Result<Unit>` (rather than throwing) lets the ViewModel surface errors
without try/catch at the call site:

```kotlin
val result = addTaskUseCase(title, expiry, category)
result.onFailure { error ->
    effectChannel.send(TodayEffect.ShowError(error.message ?: "Unknown error"))
}
```

## Dependencies

```
:core:domain
 ├── :core:data    (TaskRepository interface)
 ├── :core:model   (Task, TaskCategory)
 └── :core:common  (DateTimeProvider, dispatchers)
```

The domain module depends on the `TaskRepository` **interface**, not the Room
implementation. This means use cases can be tested with a `FakeTaskRepository` and
never touch a real database.

## Convention plugin

```kotlin
apply plugin: 'todoapp.kotlin.library'
apply plugin: 'todoapp.android.hilt'
```
