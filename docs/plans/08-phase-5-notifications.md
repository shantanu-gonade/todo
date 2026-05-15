# Phase 5 — Notifications

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans.
> Prerequisite: Phase 4 verification gate passed.

**Produces:** a WorkManager job that fires one end-of-day reminder while
incomplete tasks remain. This is the **only** background job in the app — and it
is a genuine time-triggered side effect, not correctness logic. The day-reset
itself never depends on it (see `01-architecture-and-design.md` §3).

**Package root:** `com.eulerity.todo`. Module path: `core/data`. Gradle commands
run from `Eulerity/todo/`.

---

## Task 5.1: Reminder worker — `:core:data`

To avoid a twelfth module for one worker, this lives in `:core:data`.

**Files:** under
`core/data/src/main/kotlin/com/eulerity/todo/core/data/notification/` —
`TodoNotificationChannel.kt`, `EndOfDayReminderWorker.kt`,
`ReminderScheduler.kt`. Modify: `app/src/main/AndroidManifest.xml`.

**Step 1: Write `TodoNotificationChannel.kt`** — creates a single
`NotificationChannel` ("Daily reminders", importance default) on first use.
Idempotent; safe to call repeatedly.

**Step 2: Write `EndOfDayReminderWorker.kt`** — a `@HiltWorker CoroutineWorker`
with `@AssistedInject` constructor taking the `TaskDao` (or `TaskRepository`) and
`DateTimeProvider`. In `doWork()`: read `taskDao.observeTasksForDate(today)`
once via `.first()`; if any task is incomplete, post a notification ("You still
have N task(s) left for today"). Always return `Result.success()`.

```kotlin
package com.eulerity.todo.core.data.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.eulerity.todo.core.common.DateTimeProvider
import com.eulerity.todo.core.database.TaskDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class EndOfDayReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val taskDao: TaskDao,
    private val dateTimeProvider: DateTimeProvider,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val today = dateTimeProvider.today()
        val incomplete = taskDao.observeTasksForDate(today).first().count { !it.isCompleted }
        if (incomplete > 0) {
            TodoNotificationChannel.ensure(applicationContext)
            // build + post the notification via NotificationManagerCompat
        }
        return Result.success()
    }
}
```

**Step 3: Write `ReminderScheduler.kt`** — schedules the worker as a
`OneTimeWorkRequest` with an `initialDelay` computed to a fixed evening time
(e.g. 21:00 local; if already past, schedule for tomorrow's 21:00). Use a unique
work name with `ExistingWorkPolicy.REPLACE`. Re-scheduled each app launch and
after the worker runs. Expose `schedule()` and `cancel()`. Injectable
(`@Inject constructor` taking `@ApplicationContext Context`).

**Step 4: Modify `app/src/main/AndroidManifest.xml`** — add the
`POST_NOTIFICATIONS` permission (API 33+). The Hilt `WorkerFactory` wiring lives
in `:app` (`TodoApplication`, Phase 6) — note that here so it is not forgotten.

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

Confirm there is still **no** `INTERNET` permission.

**Step 5: Instrumented smoke test** — WorkManager timing is awkward to unit
test. Add a small instrumented test under
`core/data/src/androidTest/kotlin/.../` using `WorkManagerTestInitHelper` that
enqueues `EndOfDayReminderWorker` with zero delay and asserts it returns
`Result.success()`. The notification firing itself is verified manually in the
demo.

**Step 6: Commit**

```bash
git add core/data app/src/main/AndroidManifest.xml
git commit -m "feat(notifications): add end-of-day reminder worker and scheduler"
```

## Task 5.2: Phase 5 verification gate

Run: `./gradlew :core:data:assembleDebug :core:data:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, existing core:data tests still green.

Commit any fixes, then proceed to `09-phase-6-app.md`.
