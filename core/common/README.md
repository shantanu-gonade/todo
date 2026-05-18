# :core:common

Cross-cutting utilities shared by the data and domain layers. Contains the
`DateTimeProvider` abstraction, the `DateChangeBroadcaster` for reactive day
detection, and Hilt dispatcher qualifiers.

## Key components

### `DateTimeProvider`

```kotlin
interface DateTimeProvider {
    fun now(): Instant
    fun today(): LocalDate
    val currentDay: Flow<LocalDate>
}
```

The `currentDay` flow is the pivot for the app's day-reset mechanism. The repository
uses `flatMapLatest` on this flow to automatically switch to a fresh query whenever
the calendar day changes — no deletion job required.

`DefaultDateTimeProvider` constructs `currentDay` by merging an immediate emit with
the `DateChangeBroadcaster.changes` flow, then applying `distinctUntilChanged()`.

### `DateChangeBroadcaster`

```kotlin
interface DateChangeBroadcaster {
    val changes: Flow<Unit>
}
```

`SystemDateChangeBroadcaster` is a `@Singleton` that registers a dynamic
`BroadcastReceiver` for `ACTION_DATE_CHANGED`, `ACTION_TIME_CHANGED`, and
`ACTION_TIMEZONE_CHANGED`. The receiver is wrapped in `callbackFlow` and kept alive
as a `SharedFlow` so registration happens only once regardless of how many collectors
subscribe.

### Dispatcher qualifiers

```kotlin
@Qualifier @Retention(AnnotationRetention.RUNTIME)
annotation class IoDispatcher

@Qualifier @Retention(AnnotationRetention.RUNTIME)
annotation class DefaultDispatcher
```

Used throughout the data and domain layers to inject the correct coroutine dispatcher
without hard-coding `Dispatchers.IO`.

### `CommonModule`

Hilt module that binds `DateTimeProvider` → `DefaultDateTimeProvider`, binds
`DateChangeBroadcaster` → `SystemDateChangeBroadcaster`, and provides `Clock.System`,
`TimeZone.currentSystemDefault()`, `@IoDispatcher`, and `@DefaultDispatcher`.

## Dependencies

```
:core:common
 └── :core:model   (imports ThemeMode, Task for broadcast context)
```

Plus `kotlinx-datetime`, `kotlinx-coroutines-core`, and Hilt.

## Convention plugin

```kotlin
apply plugin: 'todoapp.android.library'
apply plugin: 'todoapp.android.hilt'
```
