# Eulerity Todo — Today-Only Todo App

A production-quality Android todo application built as an Eulerity internship
take-home exercise. The app demonstrates NowInAndroid-style multi-module
architecture, MVI, Jetpack Compose with Material 3, Room, DataStore, Hilt DI,
and a reactive day-reset mechanism that requires no background jobs.

---

## Demo Video

https://github.com/user-attachments/assets/fbdebc49-4368-4c15-b1ea-ee9a4ef50c45

---

## Features

- **Today-only tasks** — each task belongs to the current calendar day; a new day automatically shows a clean slate without any deletion job
- **Task categories** — Personal, Work, Errands, Health, Home (with M3 color roles)
- **Expiry time** — optional per-task expiry time with an exact AlarmManager notification when it arrives
- **Edit & delete** — tap a task title to edit; swipe left to delete
- **Drag-to-move** — long-press and drag a task to a different category section
- **History screen** — read-only view of tasks from previous days, grouped by date with sticky headers
- **End-of-day reminder** — WorkManager fires a notification at 21:00 if any tasks remain incomplete
- **Material 3 theming** — dynamic color (Android 12+) with static light/dark fallback; user-selectable theme via DataStore
- **Fully offline** — no network dependency; Room is the single source of truth

---

## Quick Start

### Prerequisites

- Android Studio Meerkat (or later) with AGP 9+ support
- JDK 17
- Android device / emulator with API 28+

### Build and run

```bash
# Clone the repo (the todo/ subdirectory is the Android project root)
cd Eulerity/todo

# Debug build
./gradlew :app:assembleDebug

# Install on connected device
./gradlew :app:installDebug

# Run all unit tests
./gradlew test

# Run static analysis (Detekt)
./gradlew detekt

# Run Spotless code formatter check
./gradlew spotlessCheck

# Apply Spotless formatting
./gradlew spotlessApply
```

---

## Module Map

```
todo/
├── app/                        → Single-activity host; navigation root; theme wiring
├── build-logic/
│   └── convention/             → Gradle convention plugins (shared build config)
├── core/
│   ├── model/                  → Pure Kotlin domain models (Task, TaskCategory, UserData)
│   ├── common/                 → DateTimeProvider, dispatchers, broadcast utilities
│   ├── database/               → Room database, DAO, TypeConverters, migrations
│   ├── datastore/              → Preferences DataStore (theme setting)
│   ├── data/                   → Repositories, mappers, notification scheduler
│   ├── domain/                 → Use cases (business logic, validation)
│   ├── designsystem/           → TodoTheme (M3), color palette, typography, shared components
│   └── ui/                     → Shared composables: TaskCard, TaskList, TaskUi model
└── feature/
    ├── today/                  → Today screen: MVI ViewModel, add/edit sheet, category chips
    └── history/                → History screen: expired tasks grouped by date (read-only)
```

For a deep architectural explanation see **[docs/architecture.md](docs/architecture.md)**.  
For a comprehensive plans see **[docs/plans](docs/plans)**.
For build system details see **[docs/build-logic.md](docs/build-logic.md)**.  
For testing strategy see **[docs/testing.md](docs/testing.md)**.
For screenshots see **[docs/screenshots](docs/screenshots)**.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                    UI Layer                          │
│  TodayRoute / HistoryRoute (Compose)                 │
│  TodayViewModel / HistoryViewModel (MVI)             │
│  TodayUiState + TodayIntent + TodayEffect            │
├─────────────────────────────────────────────────────┤
│                  Domain Layer                        │
│  AddTaskUseCase     UpdateTaskUseCase                │
│  ObserveTodaysTasksUseCase                           │
│  ObserveExpiredTasksUseCase                          │
│  ToggleTaskCompletionUseCase  DeleteTaskUseCase       │
├─────────────────────────────────────────────────────┤
│                   Data Layer                         │
│  OfflineTaskRepository  OfflineFirstUserDataRepository│
│  TaskExpiryScheduler (AlarmManager)                  │
│  ReminderScheduler (WorkManager)                     │
├─────────────────────────────────────────────────────┤
│              Persistence                             │
│  Room (todo.db v2 — tasks table)                     │
│  Preferences DataStore (theme_mode)                  │
└─────────────────────────────────────────────────────┘
```

**Key design decisions:**
- Day-reset is **query-time filtering** via `DateTimeProvider.currentDay` flow +
  `flatMapLatest` — tasks are never deleted when the day rolls over; the query
  simply switches to the new date. This is non-destructive and correct even if
  the app is never opened at midnight.
- State flows through a **single `combine`** in each ViewModel: one upstream
  from the domain, one local `MutableStateFlow` for ephemeral UI state (sheet
  visibility, draft fields). No shared mutable state leaks into composables.
- One-shot events (haptics, Snackbar messages) travel through a `Channel`
  collected with `flowWithLifecycle` so they are never lost during configuration
  changes and never re-delivered after the screen returns to the foreground.

---

## Tech Stack

| Layer | Library | Version |
|---|---|---|
| UI | Jetpack Compose + Material 3 | BOM 2026.05.00 |
| Navigation | Navigation Compose | 2.8.5 |
| DI | Hilt | 2.59.2 |
| Database | Room | 2.8.4 |
| Preferences | DataStore Preferences | 1.1.1 |
| Date/Time | kotlinx-datetime | 0.6.1 |
| Async | Kotlin Coroutines + Flow | 1.9.0 |
| Background | WorkManager | 2.10.0 |
| Build | AGP | 9.0.1 |
| Kotlin | Kotlin | 2.1.0 |

---

## Project Configuration

| Property | Value |
|---|---|
| `minSdk` | 28 |
| `targetSdk` | 35 |
| `compileSdk` | 36 |
| Java source/target | 17 |
| Package | `com.eulerity.todo` |

---

## Contributing

1. All modules use convention plugins from `build-logic/`. Adding a new library module:
   `apply plugin: 'todoapp.android.library'` (plus `todoapp.android.library.compose`
   if Compose is needed).
2. Code style is enforced by **Spotless** (Kotlin + ktlint) and **Detekt** static
   analysis. Both run in CI via `.github/workflows/ci.yml`. Run `./gradlew spotlessApply`
   before committing.
3. New features follow the same MVI pattern as `:feature:today`. See
   [docs/architecture.md](docs/architecture.md) for the template.
4. Tests use real fakes (no Mockito). See [docs/testing.md](docs/testing.md) for patterns.
