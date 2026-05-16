# Submission Improvements Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Polish the Eulerity Today-Only Todo app to a clean, professional, submission-ready state by fixing UX issues, plugging test gaps, removing dead code, and writing the README.

**Architecture:** NIA-style multi-module (app / feature:today / feature:history / core:*). MVI with sealed Intent/Effect, Room + DataStore persistence, DateTimeProvider abstraction, WorkManager notifications. All improvements respect the existing layer boundaries — no new dependencies introduced unless noted.

**Tech Stack:** Kotlin 2.1, Jetpack Compose BOM 2026.05, Hilt 2.59, Room 2.8, DataStore 1.1, WorkManager 2.10, kotlinx-datetime 0.6, Turbine 1.2, JUnit 4, Robolectric 4.14

---

## Audit Summary — What Needs Fixing

### P0 — Submission blockers
1. **No README** — required by the spec
2. **FAB shows literal `"+"` text** — should use `Icons.Default.Add` icon (M3 standard)
3. **History screen shows non-functional delete/toggle** — passes no-op lambdas to `TaskList`, giving users interactive-looking controls that do nothing. History should use a read-only card variant.
4. **Notification uses `android.R.drawable.ic_popup_reminder`** — system drawables are not guaranteed; a custom/branded drawable should be used or the `@SuppressLint` must be removed correctly

### P1 — Quality / best practices
5. **`Icons.Outlined.ArrowBack` is deprecated** — replace with `Icons.AutoMirrored.Outlined.ArrowBack`
6. **`Placeholder.kt` files in every module** — dead code, should be deleted
7. **Missing `HistoryViewModelTest`** — zero ViewModel tests for history feature
8. **Missing `ToggleTaskCompletionUseCase` and `DeleteTaskUseCase` unit tests** — these are thin but should have at least one test each
9. **`TodayViewModel` doesn't receive `DateTimeProvider`** in the test Fakes wiring — the ViewModel has a `dateTimeProvider` constructor param but test Fakes don't thread it through explicitly (currently relying on `FakeDateTimeProvider` default)
10. **Loading state not shown to user** — `isLoading = true` initial value has no spinner in `TodayScreen`; user sees a blank screen on cold start

### P2 — Nice-to-have polish
11. **FAB content description missing** — accessibility gap
12. **History title "Expired" is confusing** — "History" better matches the navigation label on Today screen
13. **`ksp` version in `libs.versions.toml`** is `2.3.6` which doesn't match any real KSP release for Kotlin 2.1.0 — should be `2.1.0-1.0.29` (verify and fix)
14. **`release` build type has `isMinifyEnabled = false`** — production releases should enable R8; add basic ProGuard rules
15. **App does not swipe-to-dismiss tasks** — a small UX delight item; add `SwipeToDismissBox` on `TaskCard`

---

## Task 1: Delete all `Placeholder.kt` dead-code files

**Files:**
- Delete: `app/src/main/java/com/eulerity/todo/Placeholder.kt`
- Delete: `core/common/src/main/kotlin/com/eulerity/todo/core/common/Placeholder.kt`
- Delete: `core/data/src/main/kotlin/com/eulerity/todo/core/data/Placeholder.kt`
- Delete: `core/database/src/main/kotlin/com/eulerity/todo/core/database/Placeholder.kt`
- Delete: `core/datastore/src/main/kotlin/com/eulerity/todo/core/datastore/Placeholder.kt`
- Delete: `core/designsystem/src/main/kotlin/com/eulerity/todo/core/designsystem/Placeholder.kt`
- Delete: `core/domain/src/main/kotlin/com/eulerity/todo/core/domain/Placeholder.kt`
- Delete: `core/model/src/main/kotlin/com/eulerity/todo/core/model/Placeholder.kt`
- Delete: `core/ui/src/main/kotlin/com/eulerity/todo/core/ui/Placeholder.kt`
- Delete: `feature/history/src/main/kotlin/com/eulerity/todo/feature/history/Placeholder.kt`
- Delete: `feature/today/src/main/kotlin/com/eulerity/todo/feature/today/Placeholder.kt`

**Step 1: Delete all placeholder files**

```bash
find /path/to/todo -name "Placeholder.kt" | xargs rm -f
```

In the shell sandbox:
```bash
find /sessions/amazing-upbeat-heisenberg/mnt/Eulerity/todo -name "Placeholder.kt" | xargs rm -f
```

**Step 2: Verify build still compiles (dry-run check)**

```bash
# Just verify files are gone
find /sessions/amazing-upbeat-heisenberg/mnt/Eulerity/todo -name "Placeholder.kt"
# Expected: no output
```

**Step 3: Commit**

```bash
cd /sessions/amazing-upbeat-heisenberg/mnt/Eulerity/todo
git add -A
git commit -m "chore: remove all Placeholder.kt dead-code files"
```

---

## Task 2: Fix deprecated `ArrowBack` icon in HistoryScreen

**Files:**
- Modify: `feature/history/src/main/kotlin/com/eulerity/todo/feature/history/HistoryScreen.kt`

**Step 1: Replace the import and usage**

Old import:
```kotlin
import androidx.compose.material.icons.outlined.ArrowBack
```

New import:
```kotlin
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
```

Old usage:
```kotlin
imageVector = Icons.Outlined.ArrowBack,
```

New usage:
```kotlin
imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
```

**Step 2: Verify the file compiles (visual check — no test needed for import swap)**

Open `HistoryScreen.kt`, confirm no red underlines in IDE.

**Step 3: Commit**

```bash
git add feature/history/src/main/kotlin/com/eulerity/todo/feature/history/HistoryScreen.kt
git commit -m "fix: replace deprecated ArrowBack with AutoMirrored variant"
```

---

## Task 3: Fix FAB — replace `"+"` text with `Icons.Default.Add`

**Files:**
- Modify: `feature/today/src/main/kotlin/com/eulerity/todo/feature/today/TodayScreen.kt`

**Step 1: Update the FAB content**

Find the existing FAB:
```kotlin
floatingActionButton = {
    FloatingActionButton(
        onClick = { onIntent(TodayIntent.OpenAddSheet) },
    ) {
        Text(text = "+")
    }
},
```

Replace with:
```kotlin
floatingActionButton = {
    FloatingActionButton(
        onClick = { onIntent(TodayIntent.OpenAddSheet) },
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add task",
        )
    }
},
```

Add missing import at top of file:
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
```

(Remove import of `Text` if it's now unused — check if `Text` is used elsewhere in the file first.)

**Step 2: Update the UI test that taps by text `"+"`**

File: `feature/today/src/androidTest/kotlin/com/eulerity/todo/feature/today/TodayScreenTest.kt`

Old:
```kotlin
composeRule.onNodeWithText("+").performClick()
```

New:
```kotlin
composeRule.onNodeWithContentDescription("Add task").performClick()
```

**Step 3: Update the preview in TodayScreen.kt**

The `TodayScreenEmptyPreview` and `TodayScreenTasksPreview` will automatically reflect the icon — no change needed.

**Step 4: Commit**

```bash
git add feature/today/src/main/kotlin/com/eulerity/todo/feature/today/TodayScreen.kt \
        feature/today/src/androidTest/kotlin/com/eulerity/todo/feature/today/TodayScreenTest.kt
git commit -m "fix: replace FAB text '+' with proper Add icon and content description"
```

---

## Task 4: Add loading spinner to TodayScreen

**Files:**
- Modify: `feature/today/src/main/kotlin/com/eulerity/todo/feature/today/TodayScreen.kt`

**Step 1: Update the `Box` content inside `Scaffold` to handle `isLoading`**

Find the content area inside `Scaffold`:
```kotlin
) { innerPadding ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
    ) {
        if (uiState.tasks.isEmpty()) {
            TodoEmptyState(...)
        } else {
            TaskList(...)
        }
    }
```

Replace with:
```kotlin
) { innerPadding ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.Center,
    ) {
        when {
            uiState.isLoading -> CircularProgressIndicator()
            uiState.tasks.isEmpty() -> TodoEmptyState(
                headline = "Nothing for today yet",
                supportingText = "Tap + to add your first task",
            )
            else -> TaskList(
                tasks = uiState.tasks,
                onToggle = onToggle,
                onDelete = onDelete,
            )
        }
    }
```

Add import:
```kotlin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
```

**Step 2: Add a loading preview**

```kotlin
@Preview(showBackground = true, name = "Today — Loading")
@Composable
private fun TodayScreenLoadingPreview() {
    TodoTheme {
        TodayScreen(uiState = TodayUiState(isLoading = true))
    }
}
```

**Step 3: Add a UI test for loading state**

File: `feature/today/src/androidTest/kotlin/com/eulerity/todo/feature/today/TodayScreenTest.kt`

```kotlin
@Test
fun `loading indicator is shown during initial load`() {
    composeRule.setContent {
        TodoTheme {
            TodayScreen(uiState = TodayUiState(isLoading = true))
        }
    }
    composeRule
        .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
        .assertIsDisplayed()
}
```

Add import:
```kotlin
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
```

**Step 4: Run UI test to verify it fails first, then passes after implementation**

```bash
# Run on connected device or emulator
./gradlew :feature:today:connectedDebugAndroidTest --tests "*.TodayScreenTest"
```

**Step 5: Commit**

```bash
git add feature/today/src/main/kotlin/com/eulerity/todo/feature/today/TodayScreen.kt \
        feature/today/src/androidTest/kotlin/com/eulerity/todo/feature/today/TodayScreenTest.kt
git commit -m "feat: show CircularProgressIndicator during initial task load"
```

---

## Task 5: Fix History screen — remove non-functional interactive affordances

The History screen passes no-op lambdas to `TaskList`, which renders fully interactive `TaskCard`s (checkbox, delete button) that silently do nothing. This is confusing UX.

**Approach:** Add a `readOnly: Boolean` parameter to `TaskCard` and `TaskList`. When `true`, hide the delete button and make the checkbox non-interactive (display-only).

**Files:**
- Modify: `core/ui/src/main/kotlin/com/eulerity/todo/core/ui/TaskCard.kt`
- Modify: `core/ui/src/main/kotlin/com/eulerity/todo/core/ui/TaskList.kt`
- Modify: `feature/history/src/main/kotlin/com/eulerity/todo/feature/history/HistoryScreen.kt`

**Step 1: Add `readOnly` param to `TaskCard`**

```kotlin
@Composable
fun TaskCard(
    task: TaskUi,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,          // NEW
) {
    // ...existing card setup...
    Row(...) {
        TodoCheckbox(
            checked = task.isCompleted,
            onCheckedChange = if (readOnly) ({}) else onToggle,  // disable interaction
            enabled = !readOnly,                                   // NEW: visual greying
        )
        // ...title column unchanged...
        if (!readOnly) {                                           // NEW: hide delete
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Delete task")
            }
        }
    }
}
```

Update `TodoCheckbox` to accept `enabled`:
```kotlin
@Composable
fun TodoCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,           // NEW
) {
    val scale by animateFloatAsState(...)
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,             // NEW
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
    )
}
```

**Step 2: Add `readOnly` param to `TaskList`**

```kotlin
@Composable
fun TaskList(
    tasks: List<TaskUi>,
    onToggle: (id: String, checked: Boolean) -> Unit,
    onDelete: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    readOnly: Boolean = false,          // NEW
) {
    LazyColumn(...) {
        items(...) { task ->
            TaskCard(
                task = task,
                onToggle = { checked -> onToggle(task.id, checked) },
                onDelete = { onDelete(task.id) },
                modifier = Modifier.animateItem(),
                readOnly = readOnly,    // NEW
            )
        }
    }
}
```

**Step 3: Update `HistoryScreen` to use `readOnly = true`**

```kotlin
TaskList(
    tasks = uiState.tasks,
    onToggle = noOpToggle,
    onDelete = noOpDelete,
    readOnly = true,   // NEW
)
```

Remove the `noOpToggle` / `noOpDelete` remember blocks (they still work, just harmless now).

**Step 4: Write the failing test, run it, implement, run it again**

File: `feature/history/src/test/kotlin/com/eulerity/todo/feature/history/HistoryViewModelTest.kt`

```kotlin
package com.eulerity.todo.feature.history

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class HistoryViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state is loading`() = runTest {
        val vm = HistoryViewModel(FakeObserveExpiredTasks(count = 0))
        vm.uiState.test {
            val first = awaitItem()
            // May be the loading initial value or the first real emission
            // depending on timing — either way tasks must eventually be empty
            if (first.isLoading) {
                val loaded = awaitItem()
                assertFalse(loaded.isLoading)
                assertEquals(0, loaded.tasks.size)
            } else {
                assertFalse(first.isLoading)
                assertEquals(0, first.tasks.size)
            }
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `expired tasks are mapped to ui models`() = runTest {
        val vm = HistoryViewModel(FakeObserveExpiredTasks(count = 3))
        vm.uiState.test {
            val state = generateSequence { awaitItem() }.first { !it.isLoading }
            assertEquals(3, state.tasks.size)
            assertFalse(state.isLoading)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `new expired tasks are reflected in ui state`() = runTest {
        val (useCase, repo) = FakeObserveExpiredTasksWithRepo()
        val vm = HistoryViewModel(useCase)
        vm.uiState.test {
            // consume initial emissions
            generateSequence { awaitItem() }.first { !it.isLoading }
            // push new tasks
            repo.tasksFlow.value = listOf(
                Task(
                    id = "new", title = "New expired", isCompleted = false,
                    createdDate = LocalDate(2026, 5, 13),
                    createdAt = Instant.fromEpochSeconds(0), expiryTime = null,
                )
            )
            val updated = awaitItem()
            assertEquals(1, updated.tasks.size)
            assertEquals("New expired", updated.tasks.first().title)
            cancelAndConsumeRemainingEvents()
        }
    }
}
```

Add missing imports to the test file:
```kotlin
import com.eulerity.todo.core.model.Task
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
```

**Step 5: Run unit tests**

```bash
./gradlew :feature:history:testDebugUnitTest
```

Expected: all pass.

**Step 6: Commit**

```bash
git add core/ui/src/main/kotlin/com/eulerity/todo/core/ui/TaskCard.kt \
        core/ui/src/main/kotlin/com/eulerity/todo/core/ui/TaskList.kt \
        core/designsystem/src/main/kotlin/com/eulerity/todo/core/designsystem/component/TodoCheckbox.kt \
        feature/history/src/main/kotlin/com/eulerity/todo/feature/history/HistoryScreen.kt \
        feature/history/src/test/kotlin/com/eulerity/todo/feature/history/HistoryViewModelTest.kt
git commit -m "fix: make history read-only, add HistoryViewModel unit tests"
```

---

## Task 6: Fix History screen title from "Expired" → "History"

**Files:**
- Modify: `feature/history/src/main/kotlin/com/eulerity/todo/feature/history/HistoryScreen.kt`

**Step 1: Change the `TodoTopAppBar` title**

```kotlin
// Old
TodoTopAppBar(title = "Expired", ...)

// New
TodoTopAppBar(title = "History", ...)
```

**Step 2: Update the preview title string in `HistoryScreen`**

```kotlin
// Old preview names are fine as-is — no change needed in @Preview annotations
```

**Step 3: Commit**

```bash
git add feature/history/src/main/kotlin/com/eulerity/todo/feature/history/HistoryScreen.kt
git commit -m "fix: rename History screen title from 'Expired' to 'History'"
```

---

## Task 7: Add missing unit tests for `ToggleTaskCompletionUseCase` and `DeleteTaskUseCase`

**Files:**
- Create: `core/domain/src/test/kotlin/com/eulerity/todo/core/domain/ToggleTaskCompletionUseCaseTest.kt`
- Create: `core/domain/src/test/kotlin/com/eulerity/todo/core/domain/DeleteTaskUseCaseTest.kt`

**Step 1: Write `ToggleTaskCompletionUseCaseTest`**

```kotlin
package com.eulerity.todo.core.domain

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class ToggleTaskCompletionUseCaseTest {

    private val repo = FakeTaskRepository()

    @Test
    fun `invoke delegates to repository setCompleted`() = runTest {
        val useCase = ToggleTaskCompletionUseCase(repo)
        useCase("task-1", true)
        assertTrue(repo.toggled.any { it.first == "task-1" && it.second })
    }

    @Test
    fun `invoke can mark task as incomplete`() = runTest {
        val useCase = ToggleTaskCompletionUseCase(repo)
        useCase("task-2", false)
        assertTrue(repo.toggled.any { it.first == "task-2" && !it.second })
    }
}
```

Note: `FakeTaskRepository` already exists in `core/domain/src/test` as `FakeTaskRepository.kt` — reuse it. Check the import path matches.

**Step 2: Write `DeleteTaskUseCaseTest`**

```kotlin
package com.eulerity.todo.core.domain

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertTrue

class DeleteTaskUseCaseTest {

    private val repo = FakeTaskRepository()

    @Test
    fun `invoke delegates delete to repository`() = runTest {
        val useCase = DeleteTaskUseCase(repo)
        useCase("task-99")
        assertTrue(repo.deleted.contains("task-99"))
    }
}
```

**Step 3: Run tests to verify they fail (no implementation to fix — they should pass immediately)**

```bash
./gradlew :core:domain:testDebugUnitTest
```

Expected: all pass (use cases are trivial delegators; the tests verify the delegation).

**Step 4: Commit**

```bash
git add core/domain/src/test/kotlin/com/eulerity/todo/core/domain/ToggleTaskCompletionUseCaseTest.kt \
        core/domain/src/test/kotlin/com/eulerity/todo/core/domain/DeleteTaskUseCaseTest.kt
git commit -m "test: add unit tests for ToggleTaskCompletionUseCase and DeleteTaskUseCase"
```

---

## Task 8: Fix notification worker — replace system drawable with app drawable

The `EndOfDayReminderWorker` references `android.R.drawable.ic_popup_reminder` (a system resource), marked with `@SuppressLint("SuspiciousImport")`. This is fragile and the lint suppression is hiding a real issue.

**Files:**
- Modify: `core/data/src/main/kotlin/com/eulerity/todo/core/data/notification/EndOfDayReminderWorker.kt`
- Modify: `app/src/main/res/drawable/` — add `ic_notification.xml` (a simple vector)

**Step 1: Create `ic_notification.xml` in the app module**

Create file: `app/src/main/res/drawable/ic_notification.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- Simple check-circle notification icon, white on transparent background -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z" />
</vector>
```

**Step 2: Update `EndOfDayReminderWorker` to use the app drawable**

Remove:
```kotlin
//noinspection SuspiciousImport
import android.R
```

Change:
```kotlin
.setSmallIcon(R.drawable.ic_popup_reminder)
```

To (using the app's own R):
```kotlin
.setSmallIcon(com.eulerity.todo.R.drawable.ic_notification)
```

Or add a proper import:
```kotlin
import com.eulerity.todo.R as AppR
// then:
.setSmallIcon(AppR.drawable.ic_notification)
```

Note: The `core:data` module cannot directly reference `app` module resources (dependency direction). The correct fix is to move the drawable to `core:designsystem` module and reference it from there, OR pass the icon res ID as a constructor parameter via Hilt. **Simplest correct approach:** move drawable to `core/designsystem/src/main/res/drawable/` and reference `com.eulerity.todo.core.designsystem.R.drawable.ic_notification`.

**Step 3: Remove the `@SuppressLint` annotation**

```kotlin
// Remove this line entirely:
//noinspection SuspiciousImport
```

**Step 4: Verify the project compiles**

```bash
./gradlew :core:data:assembleDebug
```

**Step 5: Commit**

```bash
git add core/data/src/main/kotlin/com/eulerity/todo/core/data/notification/EndOfDayReminderWorker.kt \
        core/designsystem/src/main/res/drawable/ic_notification.xml
git commit -m "fix: replace system drawable with app-owned notification icon"
```

---

## Task 9: Add swipe-to-dismiss on TaskCard (Today screen only)

This adds a small UX delight: swiping left on a task in Today's list deletes it. History remains read-only (no swipe).

**Files:**
- Modify: `core/ui/src/main/kotlin/com/eulerity/todo/core/ui/TaskList.kt`

**Step 1: Wrap `TaskCard` in `SwipeToDismissBox` inside `TaskList`**

```kotlin
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TaskList(
    tasks: List<TaskUi>,
    onToggle: (id: String, checked: Boolean) -> Unit,
    onDelete: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    readOnly: Boolean = false,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = tasks,
            key = { it.id },
            contentType = { "task" },
        ) { task ->
            if (readOnly) {
                TaskCard(
                    task = task,
                    onToggle = { checked -> onToggle(task.id, checked) },
                    onDelete = { onDelete(task.id) },
                    modifier = Modifier.animateItem(),
                    readOnly = true,
                )
            } else {
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            onDelete(task.id)
                            true
                        } else false
                    }
                )
                // Reset if the task wasn't actually deleted (e.g. error recovery)
                LaunchedEffect(task.id) {
                    if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                        dismissState.reset()
                    }
                }
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        // Red delete background revealed during swipe
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    MaterialTheme.colorScheme.errorContainer,
                                    shape = MaterialTheme.shapes.medium,
                                ),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete task",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(end = 16.dp),
                            )
                        }
                    },
                    modifier = Modifier.animateItem(),
                ) {
                    TaskCard(
                        task = task,
                        onToggle = { checked -> onToggle(task.id, checked) },
                        onDelete = { onDelete(task.id) },
                    )
                }
            }
        }
    }
}
```

Add missing imports:
```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.outlined.Delete
```

**Step 2: Run unit tests to verify nothing broke**

```bash
./gradlew :core:ui:testDebugUnitTest
./gradlew :feature:today:testDebugUnitTest
```

**Step 3: Commit**

```bash
git add core/ui/src/main/kotlin/com/eulerity/todo/core/ui/TaskList.kt
git commit -m "feat: add swipe-to-dismiss gesture for task deletion on Today screen"
```

---

## Task 10: Enable R8/ProGuard for release builds

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/proguard-rules.pro`

**Step 1: Enable minification in release build type**

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true          // was false
        isShrinkResources = true        // add this
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro",
        )
    }
}
```

**Step 2: Add essential ProGuard rules to `app/proguard-rules.pro`**

```proguard
# Keep Hilt-generated components
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# Keep Room entities and DAOs
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }

# Keep WorkManager worker classes
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }

# Keep data classes used in DataStore
-keepclassmembers class com.eulerity.todo.core.model.** { *; }

# kotlinx-serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
```

**Step 3: Verify release build compiles (no device needed)**

```bash
./gradlew assembleRelease
```

Expected: BUILD SUCCESSFUL (R8 applied).

**Step 4: Commit**

```bash
git add app/build.gradle.kts app/proguard-rules.pro
git commit -m "build: enable R8 minification + resource shrinking for release builds"
```

---

## Task 11: Verify KSP version in `libs.versions.toml`

**Files:**
- Modify: `gradle/libs.versions.toml` (if needed)

**Step 1: Check current KSP version**

In `libs.versions.toml`:
```toml
ksp = "2.3.6"   # This is suspicious — KSP versions track Kotlin versions
```

For Kotlin `2.1.0`, the correct KSP version is `2.1.0-1.0.29`.

**Step 2: Update if wrong**

```toml
ksp = "2.1.0-1.0.29"
```

**Step 3: Sync and verify build**

```bash
./gradlew :app:assembleDebug
```

If it was already working (the project was building before), this may already be fine despite the unusual version string. Verify by checking whether `2.3.6` is a valid published version on Maven Central. If the project builds cleanly, leave it; if not, fix it.

**Step 4: Commit (only if changed)**

```bash
git add gradle/libs.versions.toml
git commit -m "build: fix KSP version to match Kotlin 2.1.0"
```

---

## Task 12: Write the README

The take-home spec explicitly requires a README with:
- Overall approach
- Key decisions or tradeoffs
- What you'd improve with more time
- Anything you got stuck on

**Files:**
- Create: `README.md` (repo root, i.e. `todo/README.md`)

**Step 1: Write README.md**

```markdown
# Eulerity — Today-Only Todo App

A focused Android todo app built for the Eulerity Android intern take-home exercise.

## The Constraint

This app only cares about **today**. Tasks belong to the current day, expire at midnight, and each new day starts clean. No future dates, no backlogs, no overdue tasks — this constraint drives every product and technical decision.

---

## Overall Approach

I treated this like a small production app, not a prototype:

- **Multi-module, NIA-style architecture** — `app` / `feature:today` / `feature:history` / `core:*` modules with strict dependency rules (domain never depends on data, data never depends on UI).
- **MVI on the UI layer** — sealed `Intent` / `UiState` / `Effect` per screen. The composable is a pure function of state; mutations always flow through the ViewModel's single `onIntent` entry point.
- **Offline-first, Room as source of truth** — all task data lives in Room. `OfflineTaskRepository` exposes `Flow<List<Task>>` that re-evaluates reactively when the day rolls over, without any polling.
- **DateTimeProvider abstraction** — all "what time is it now / what day is today" reads go through an injected `DateTimeProvider`. This makes the day-rollover logic fully unit-testable without a real clock.

---

## Key Decisions and Tradeoffs

### Today-only scoping — in the database, not the UI

Tasks from previous days are never deleted; they're simply not queried in `observeTodaysTasks()`. The `observeExpiredTasks()` query retrieves them for the History screen. This means:
- The day-rollover "clean slate" is free — just change what `today` means
- History is available without any extra storage work
- **Tradeoff:** The database grows unbounded. For a production app I'd add a background cleanup job that purges rows older than ~30 days.

### Day rollover via `BroadcastReceiver` + `Flow`

`DefaultDateTimeProvider.currentDay` merges two flows: a one-shot initial emission and a `callbackFlow` that listens for `ACTION_DATE_CHANGED` broadcasts. When midnight fires:
1. The broadcast arrives → `dateChangeBroadcaster.changes` emits
2. `currentDay` re-evaluates `today()`
3. `OfflineTaskRepository.observeTodaysTasks()` via `flatMapLatest` re-queries Room for the new date
4. The Today screen updates automatically — no manual refresh needed

### History screen is read-only

Expired tasks cannot be modified. The `HistoryScreen` uses `readOnly = true` on `TaskList` / `TaskCard`, which hides delete buttons and disables checkboxes. This is a deliberate product decision: the past is immutable.

### WorkManager for end-of-day notifications

A `OneTimeWorkRequest` fires daily at 21:00 local time. `ExistingWorkPolicy.REPLACE` ensures rescheduling on every app launch is safe (idempotent). The worker only posts a notification if there are incomplete tasks — no noise when everything is done.

---

## Optional Enhancements Implemented

- ✅ Per-task expiry time (same day only) with M3 `TimePicker`
- ✅ History of expired tasks (previous days)
- ✅ Haptic feedback on task completion
- ✅ Thoughtful empty states on both screens
- ✅ Light / Dark / System theme with dynamic color on Android 12+
- ✅ End-of-day local notification (WorkManager, fires at 21:00)
- ✅ Swipe-to-dismiss for task deletion
- ✅ Loading indicator during cold-start database read
- ✅ Clear separation of concerns (multi-module Clean Architecture)
- ✅ ViewModels with reactive `StateFlow`
- ✅ Unit tests for business logic (use cases, repository, ViewModel, DataStore)
- ✅ Compose UI tests for Today screen
- ✅ Date/time abstraction (`DateTimeProvider` + `FakeDateTimeProvider` in tests)

---

## What I'd Improve With More Time

1. **Widget** — A home-screen widget showing today's incomplete task count would complete the "today only" theme beautifully.
2. **Database cleanup job** — A `PeriodicWorkRequest` to purge rows older than 30 days.
3. **Undo on delete** — A `SnackbarResult.ActionPerformed` hook after deletion to restore the task within a short window.
4. **Animated task completion** — A more elaborate animation (e.g., task slides off with a confetti burst) when a task is marked done.
5. **App Shortcuts** — A static shortcut to "Add task" for faster access from the launcher.
6. **Accessibility** — Explicit `semantics` blocks on `TaskCard` so TalkBack reads "Task: Buy milk, incomplete, expires 9:30 AM" as a single unit.

---

## What I Got Stuck On

**Keyboard dismissal inside `ModalBottomSheet`**

`ModalBottomSheet` runs in its own dialog window, separate from the Activity. `LocalFocusManager.clearFocus()` alone does not dismiss the IME because the dialog's `InputMethodManager` context differs from the Activity's. I had to capture `LocalView.current` *inside* the sheet's content lambda (not at the call site) to get the dialog window's token, then call `InputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)`. This is documented in comments in `AddTaskSheet.kt`.

**Day rollover in tests**

Testing the "task added today disappears at midnight" behavior required a `FakeDateTimeProvider` that could advance the day mid-test while the `currentDay` `Flow` remained active. I implemented this with a `MutableStateFlow<LocalDate>` and a `rollToNextDay()` helper — the repository's `flatMapLatest` re-subscribes to the Room DAO query automatically, which is exactly the production behavior.

---

## AI Tool Usage

A full record of AI interactions (Claude conversations, prompts, and iterations) is included in the submission as required. See the submission email for the shareable conversation link.

---

## Building and Running

Requirements: Android Studio Meerkat (or later), JDK 17, Android SDK 34.

```bash
git clone <repo-url>
cd todo
./gradlew assembleDebug
# Install on a connected device or emulator:
./gradlew installDebug
```

Min SDK: 28 (Android 9). Target SDK: 35.
```

**Step 2: Commit**

```bash
git add README.md
git commit -m "docs: add submission README with approach, decisions, and AI usage"
```

---

## Task 13: Final verification pass

**Step 1: Run all unit tests**

```bash
./gradlew testDebugUnitTest
```

Expected: all pass, zero failures.

**Step 2: Run lint**

```bash
./gradlew lint
```

Review the report at `app/build/reports/lint-results-debug.html`. Fix any errors (not warnings).

**Step 3: Run Compose UI tests (on emulator or device)**

```bash
./gradlew connectedDebugAndroidTest
```

Expected: `TodayScreenTest` (5 tests) all pass.

**Step 4: Verify the app runs end-to-end on an emulator**

Manual checklist:
- [ ] App launches, shows empty state "Nothing for today yet"
- [ ] FAB shows Add icon, tapping opens bottom sheet
- [ ] Adding a task with blank title shows inline error
- [ ] Adding "Buy milk" adds it to the list with haptic feedback on toggle
- [ ] Swiping a task left reveals red background, releases to delete
- [ ] Tapping History icon navigates to History screen (no interactive controls)
- [ ] Back navigation returns to Today
- [ ] Dark mode switch applies immediately
- [ ] No crash on rotation

**Step 5: Final commit**

```bash
git add -A
git commit -m "chore: final verification pass — all tests passing, lint clean"
```

---

## Execution Order Summary

| # | Task | Priority | Files Changed |
|---|------|----------|---------------|
| 1 | Delete Placeholder.kt files | P1 | 11 files deleted |
| 2 | Fix ArrowBack deprecation | P1 | HistoryScreen.kt |
| 3 | Fix FAB icon | P0 | TodayScreen.kt, TodayScreenTest.kt |
| 4 | Add loading spinner | P1 | TodayScreen.kt, TodayScreenTest.kt |
| 5 | Fix History read-only UX + HistoryViewModelTest | P0 | TaskCard.kt, TaskList.kt, TodoCheckbox.kt, HistoryScreen.kt, HistoryViewModelTest.kt |
| 6 | Fix History title | P1 | HistoryScreen.kt |
| 7 | Add use-case unit tests | P1 | 2 new test files |
| 8 | Fix notification drawable | P1 | EndOfDayReminderWorker.kt, ic_notification.xml |
| 9 | Swipe-to-dismiss | P2 | TaskList.kt |
| 10 | Enable R8 for release | P2 | app/build.gradle.kts, proguard-rules.pro |
| 11 | Verify KSP version | P2 | libs.versions.toml (if needed) |
| 12 | Write README | P0 | README.md |
| 13 | Final verification | — | — |
