# Phase 6 — App Module

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans.
> Prerequisite: Phase 5 verification gate passed.

**Produces:** a running app — `Application`, `MainActivity`, `NavHost`, theme
application, reminder scheduling.

**Package root:** `com.eulerity.todo`. Module path: `app`. Gradle commands run
from `Eulerity/todo/`.

---

## Task 6.1: Application + Hilt setup — `:app`

**Files:**
- Create: `app/src/main/kotlin/com/eulerity/todo/TodoApplication.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Delete: any leftover placeholder/example sources under `app/src`

**Step 1: Write `TodoApplication.kt`**

```kotlin
package com.eulerity.todo

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TodoApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
```

**Step 2: Update `app/src/main/AndroidManifest.xml`** — set
`android:name=".TodoApplication"` on `<application>`, add `MainActivity` with a
`LAUNCHER` `<intent-filter>`, keep `android:theme="@style/Theme.Todo"`. Remove
the default WorkManager initializer (the `Configuration.Provider` replaces it):

```xml
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    android:exported="false"
    tools:node="merge">
    <meta-data
        android:name="androidx.work.WorkManagerInitializer"
        android:value="androidx.startup"
        tools:node="remove" />
</provider>
```

Confirm there is still **no** `INTERNET` permission; `POST_NOTIFICATIONS` (added
in Phase 5) remains.

**Step 3: Commit**

```bash
git add app
git commit -m "feat(app): add Hilt Application with WorkManager configuration"
```

## Task 6.2: Navigation host + MainActivity — `:app`

**Files:**
- Create: `app/src/main/kotlin/com/eulerity/todo/navigation/TodoNavHost.kt`
- Create: `app/src/main/kotlin/com/eulerity/todo/MainActivity.kt`
- Create: `app/src/main/kotlin/com/eulerity/todo/MainActivityViewModel.kt`

**Step 1: Write `TodoNavHost.kt`**

```kotlin
package com.eulerity.todo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.eulerity.todo.feature.history.navigation.HistoryRouteKey
import com.eulerity.todo.feature.history.navigation.historyScreen
import com.eulerity.todo.feature.today.navigation.TodayRouteKey
import com.eulerity.todo.feature.today.navigation.todayScreen

@Composable
fun TodoNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = TodayRouteKey) {
        todayScreen(onNavigateToHistory = { navController.navigate(HistoryRouteKey) })
        historyScreen(onBack = { navController.popBackStack() })
    }
}
```

**Step 2: Write `MainActivityViewModel.kt`** — a `@HiltViewModel` reading
`UserDataRepository.userData` and exposing it as
`StateFlow<UserData>` so the theme honors the persisted `ThemeMode`. Use
`stateIn(SharingStarted.WhileSubscribed(5_000))` with an initial `UserData()`.

**Step 3: Write `MainActivity.kt`**

```kotlin
package com.eulerity.todo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eulerity.todo.core.data.notification.ReminderScheduler
import com.eulerity.todo.core.designsystem.theme.TodoTheme
import com.eulerity.todo.core.model.ThemeMode
import com.eulerity.todo.navigation.TodoNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var reminderScheduler: ReminderScheduler
    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        reminderScheduler.schedule()
        setContent {
            val userData by viewModel.userData.collectAsStateWithLifecycle()
            val darkTheme = when (userData.themeMode) {
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            TodoTheme(darkTheme = darkTheme) {
                TodoNavHost()
            }
        }
    }
}
```

**Step 4: Build and run**

Run: `./gradlew installDebug`
Expected: `BUILD SUCCESSFUL`. Launch on an API 28+ emulator: add a task, set an
expiry time, mark it complete (observe the animation + haptic), open History,
confirm the empty state, rotate the device and confirm state survives, kill and
relaunch the app and confirm the task persisted.

**Step 5: Commit**

```bash
git add app
git commit -m "feat(app): wire NavHost, MainActivity, theme, and reminders"
```

## Task 6.3: Phase 6 verification gate

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`. Manually confirm the full add → complete → history
flow on device, plus persistence across process death.

Commit any fixes, then proceed to `10-phase-7-verification.md`.
