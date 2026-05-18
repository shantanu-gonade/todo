# :core:data

The data layer. Contains repositories, data-source mappers, and the notification
scheduling infrastructure (AlarmManager + WorkManager). This module bridges raw
persistence (`core:database`, `core:datastore`) with the domain and UI layers.

## Repositories

### `TaskRepository` (interface)

Defined in this module; consumed by `core:domain` and feature ViewModels.

```kotlin
interface TaskRepository {
    fun observeTodaysTasks(): Flow<List<Task>>
    fun observeExpiredTasks(): Flow<List<Task>>
    suspend fun addTask(title: String, expiryTime: LocalTime?, category: TaskCategory): Task
    suspend fun updateTask(id: String, title: String, expiryTime: LocalTime?, category: TaskCategory)
    suspend fun deleteTask(taskId: String)
    suspend fun toggleCompletion(taskId: String, completed: Boolean)
    suspend fun getTaskById(taskId: String): Task?
}
```

### `OfflineTaskRepository` (implementation)

The reactive day-reset pattern lives here:

```kotlin
override fun observeTodaysTasks(): Flow<List<Task>> =
    dateTimeProvider.currentDay.flatMapLatest { day ->
        taskDao.observeTasksForDate(day).map { it.map(TaskEntity::asDomain) }
    }
```

Every write operation that touches a task with an expiry time also interacts with
`TaskExpiryScheduler` — cancelling stale alarms and scheduling new ones.

### `OfflineFirstUserDataRepository`

Wraps `UserPreferencesDataSource` and exposes a `Flow<UserData>`. Write operations
are `suspend` and delegate to DataStore's `edit {}`.

## Notification scheduling

### `TaskExpiryScheduler` (interface + AlarmManager impl)

```kotlin
interface TaskExpiryScheduler {
    fun schedule(taskId: String, taskTitle: String, expiryTime: LocalTime)
    fun cancel(taskId: String)
}
```

`AlarmManagerTaskExpiryScheduler` uses `setExactAndAllowWhileIdle` so alarms fire
even in Doze mode. Each task's `PendingIntent` uses `taskId.hashCode()` as the
request code for uniqueness and easy cancellation.

On API 31+ it checks `AlarmManager.canScheduleExactAlarms()` before scheduling.
If the permission is not granted, the alarm is silently skipped (the task still saves
normally).

### `ReminderScheduler`

Enqueues a `OneTimeWorkRequest` for `EndOfDayReminderWorker` at 21:00 using
`ExistingWorkPolicy.REPLACE`. The delay computation:

```kotlin
val targetToday = today.atTime(21, 0).toInstant(timeZone)
val target = if (now < targetToday) targetToday else targetToday + 1.days
val delayMs = (target - now).inWholeMilliseconds.coerceAtLeast(60_000)
```

### `TaskExpiryReceiver`

`BroadcastReceiver` triggered by AlarmManager. Posts a `PRIORITY_HIGH` notification
on the `todo_task_expiry` channel using the task title from the Intent extras. The
notification ID is `taskId.hashCode()` — the same value used for the `PendingIntent`
request code, which makes programmatic dismissal straightforward.

### `EndOfDayReminderWorker`

`CoroutineWorker` injected by Hilt. Before posting the 21:00 reminder it queries the
repository for incomplete tasks today; the notification is suppressed if all tasks
are already done.

### `TodoNotificationChannel`

Creates both notification channels in `Application.onCreate()`. Channel IDs:
- `todo_daily_reminder` — importance DEFAULT
- `todo_task_expiry` — importance HIGH

## Mapper

`TaskMapper.kt` contains pure extension functions (`TaskEntity.asDomain()`,
`Task.asEntity()`) that convert between Room entities and domain models. These are
tested independently with no Android dependencies.

## Hilt module

`DataModule` binds `TaskRepository` → `OfflineTaskRepository` and
`UserDataRepository` → `OfflineFirstUserDataRepository`, both as `@Singleton`.

`NotificationModule` binds `TaskExpiryScheduler` → `AlarmManagerTaskExpiryScheduler`
and provides `ReminderScheduler` as `@Singleton`.

## Dependencies

```
:core:data
 ├── :core:database
 ├── :core:datastore
 ├── :core:model
 └── :core:common
```

## Convention plugin

```kotlin
apply plugin: 'todoapp.android.library'
apply plugin: 'todoapp.android.hilt'
```
