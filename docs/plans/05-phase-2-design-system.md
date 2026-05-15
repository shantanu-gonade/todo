# Phase 2 — Design System

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans.
> Prerequisite: Phase 1 verification gate passed.

**Produces:** `:core:designsystem` (Material 3 theme, tokens, reusable
components) and `:core:ui` (composables that need the domain model). Follows the
material-3 skill: tokens via `MaterialTheme`, no hardcoded colors, tonal
elevation, dynamic color on API 31+.

**Package root:** `com.eulerity.todo`. Gradle commands run from `Eulerity/todo/`.

**Compose rules in force this phase** (from `01-architecture-and-design.md` §4):
rule 1 (status bar appearance syncs with theme), rule 4 (scale animates in the
draw phase), rule 6 (`asTaskUi` is a pure function), rule 7 (`@Preview`s wrap in
`TodoTheme`).

---

## Task 2.1: Material 3 theme — `:core:designsystem`

**Files:** under
`core/designsystem/src/main/kotlin/com/eulerity/todo/core/designsystem/theme/` —
`Color.kt`, `Type.kt`, `Shape.kt`, `Theme.kt`. Delete this module's
`Placeholder.kt`.

**Step 1: Write `Color.kt`** — static light and dark `ColorScheme`s as the
fallback for pre-API-31 devices. Generate values from a single seed color using
the Material Theme Builder palette; define `LightColors` and `DarkColors` as
`lightColorScheme(...)` / `darkColorScheme(...)`.

**Step 2: Write `Type.kt`** — a `Typography` instance named `TodoTypography`
using the M3 type scale roles (Display, Headline, Title, Body, Label). Roboto is
correct here — do not substitute, per the material-3 skill note.

**Step 3: Write `Shape.kt`** — a `Shapes` instance named `TodoShapes`: cards
`medium` (12dp), bottom sheet `extraLarge` (28dp), buttons `full`.

**Step 4: Write `Theme.kt`** — note the `DisposableEffect` syncing status-bar
icon appearance with `darkTheme` (Compose rule 1):

```kotlin
package com.eulerity.todo.core.designsystem.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun TodoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(darkTheme) {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
            onDispose { }
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = TodoTypography,
        shapes = TodoShapes,
        content = content,
    )
}
```

**Step 5: Verify it compiles**

Run: `./gradlew :core:designsystem:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`. (`androidx.core:core-ktx` already provides
`WindowCompat`.)

**Step 6: Commit**

```bash
git add core/designsystem
git commit -m "feat(designsystem): add Material 3 theme with dynamic color"
```

## Task 2.2: Reusable components — `:core:designsystem`

**Files:** under
`core/designsystem/src/main/kotlin/com/eulerity/todo/core/designsystem/component/`
— `TodoTopAppBar.kt`, `TodoEmptyState.kt`, `TodoCheckbox.kt`.

**Step 1: `TodoTopAppBar`** — a thin wrapper over `CenterAlignedTopAppBar` taking
a title string, an optional navigation icon slot, and an optional actions slot.
`@OptIn(ExperimentalMaterial3Api::class)`.

**Step 2: `TodoEmptyState`** — a centered column: an icon, a headline, supporting
text. Parameterized so both features reuse it ("Nothing for today yet" vs "No
expired tasks"). Include an `@Preview` wrapped in `TodoTheme` (Compose rule 7).

**Step 3: `TodoCheckbox`** — a `Checkbox` whose toggle drives a scale spring.
Per Compose rule 4, animate via `Modifier.graphicsLayer`:

```kotlin
package com.eulerity.todo.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun TodoCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (checked) 1.15f else 1f,
        animationSpec = spring(),
        label = "checkbox-scale",
    )
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
    )
}
```

Include an `@Preview` (wrapped in `TodoTheme`) showing checked and unchecked.

**Step 4: Compile, then commit**

```bash
git add core/designsystem
git commit -m "feat(designsystem): add reusable top bar, empty state, checkbox"
```

## Task 2.3: Model-aware UI — `:core:ui`

**Files:** under `core/ui/src/main/kotlin/com/eulerity/todo/core/ui/` —
`TaskUi.kt`, `TaskCard.kt`, `TaskList.kt`. Delete this module's `Placeholder.kt`.

**Step 1: `TaskUi.kt`** — the UI-facing representation and a pure mapper. Per
Compose rule 6, the formatter is a plain function parameter with a default — no
`LocalContext`, and the ViewModel and mapper agree on this one signature:

```kotlin
package com.eulerity.todo.core.ui

import com.eulerity.todo.core.model.Task
import kotlinx.datetime.LocalTime

data class TaskUi(
    val id: String,
    val title: String,
    val isCompleted: Boolean,
    val expiryLabel: String?,
)

/** Default formatter: "HH:mm". Pure; overridable in tests. */
fun defaultExpiryFormatter(time: LocalTime): String =
    "%02d:%02d".format(time.hour, time.minute)

fun Task.asTaskUi(
    formatExpiry: (LocalTime) -> String = ::defaultExpiryFormatter,
): TaskUi = TaskUi(
    id = id,
    title = title,
    isCompleted = isCompleted,
    expiryLabel = expiryTime?.let(formatExpiry),
)
```

**Step 2: `TaskCard.kt`** — a stateless `Card` row: `TodoCheckbox`, the title
(struck through when complete via `TextDecoration.LineThrough`), the optional
`expiryLabel`, and a delete `IconButton`. Parameters: an immutable `TaskUi` plus
`onToggle: (Boolean) -> Unit` and `onDelete: () -> Unit`. No ViewModel reference.
Include an `@Preview` wrapped in `TodoTheme` (Compose rule 7).

**Step 3: `TaskList.kt`** — a `LazyColumn` rendering `TaskCard`s with a stable
`key = { it.id }`. Per Compose rule 5, it takes the typed callbacks
`onToggle: (id: String, checked: Boolean) -> Unit` and `onDelete: (id: String)
-> Unit`; inside the `items` block it builds the per-item lambdas so the screen
above passes one stable reference rather than allocating fresh lambdas per item.
Stateless; takes `List<TaskUi>` plus the two callbacks.

**Step 4: Compile, then commit**

```bash
git add core/ui
git commit -m "feat(ui): add stateless TaskCard and TaskList composables"
```

## Task 2.4: Phase 2 verification gate

Run: `./gradlew :core:designsystem:assembleDebug :core:ui:assembleDebug`
Expected: `BUILD SUCCESSFUL`. Visually confirm previews render in the IDE and
that `@Preview`s are wrapped in `TodoTheme`.

Commit any fixes, then proceed to `06-phase-3-feature-today.md`.
