# :app

The application module. It is the single entry point for the compiled APK and owns
three responsibilities:

1. **Single-activity host** — `MainActivity` bootstraps edge-to-edge display, observes
   the user's theme preference, and requests the `POST_NOTIFICATIONS` permission on
   API 33+.
2. **Navigation root** — `TodoNavHost` wires together the two feature screens
   (`TodayRoute`, `HistoryRoute`) in a single `NavHost` using type-safe serializable
   route keys.
3. **Application-level wiring** — `TodoApplication` initialises notification channels,
   provides the Hilt-aware `WorkManager` configuration, and schedules the end-of-day
   reminder on every app launch.

## Files

| File | Purpose |
|---|---|
| `MainActivity.kt` | Single activity; `enableEdgeToEdge()`, theme observation, permission request |
| `TodoApplication.kt` | `@HiltAndroidApp`; WorkManager + notification channel setup |
| `navigation/TodoNavHost.kt` | `NavHost` registration; `navController` passed as lambdas only |

## Dependencies

```
:app
 ├── :feature:today
 ├── :feature:history
 ├── :core:data          (UserDataRepository, ReminderScheduler)
 ├── :core:designsystem  (TodoTheme)
 └── :core:model         (ThemeMode)
```

## Convention plugins applied

```kotlin
apply plugin: 'todoapp.android.application'
apply plugin: 'todoapp.android.application.compose'
apply plugin: 'todoapp.android.hilt'
```

## Notes

- The `NavController` is created inside `TodoNavHost` and never exposed to ViewModels
  or feature modules; navigation actions are passed as lambda callbacks.
- `ReminderScheduler.schedule()` is called in `MainActivity.onCreate()` with
  `ExistingWorkPolicy.REPLACE`, so the 21:00 reminder is always aligned to the current
  calendar day even after device reboots.
- Notification channels are created idempotently in `TodoApplication.onCreate()`.
  Creating an already-existing channel is a safe no-op on API 26+.
