# :core:designsystem

Material 3 design system for the app. Defines the app's `MaterialTheme` wrapper,
color palette, typography, and shared atomic components (TopAppBar, EmptyState).
All Compose screens import from this module rather than calling `MaterialTheme`
directly.

## `TodoTheme`

The root composable applied once in `MainActivity`. It selects the correct
`ColorScheme` based on Android version and user preference:

```kotlin
@Composable
fun TodoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
        dynamicColor             -> dynamicLightColorScheme(LocalContext.current)
        darkTheme                -> DarkColors
        else                     -> LightColors
    }
    DisposableEffect(darkTheme) {
        // sync status bar icon tint (light icons on dark, dark icons on light)
        …
        onDispose { … }
    }
    MaterialTheme(colorScheme = colorScheme, typography = TodoTypography, content = content)
}
```

**Dynamic color** (Android 12+, API 31) extracts palette seeds from the user's
wallpaper. On older devices the static `LightColors`/`DarkColors` palettes based on
the M3 purple seed `#6750A4` are used.

## Color palette (`Color.kt`)

The static palette is generated from the M3 baseline purple seed. `LightColors` and
`DarkColors` are `ColorScheme` instances covering all 30 M3 color roles (primary,
onPrimary, primaryContainer, …, surface, onSurface, etc.).

## Typography (`Type.kt`)

`TodoTypography` uses the Material 3 default type scale (`displayLarge` through
`labelSmall`) with the system's default font unless overridden.

## Shared components

### `TodoTopAppBar`

```kotlin
@Composable
fun TodoTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
)
```

Wraps `CenterAlignedTopAppBar` with slot-based API. Screens pass their own title
string, navigation icon, and action items without boilerplate.

### `TodoEmptyState`

```kotlin
@Composable
fun TodoEmptyState(
    headline: String,
    modifier: Modifier = Modifier,
    supportingText: String = "",
    icon: ImageVector = Icons.Rounded.CheckCircle,
)
```

Consistent empty state treatment across screens: centered icon + headline +
optional supporting text.

## TaskCategory color extension

`TaskCategory.categoryColor()` is a `@Composable` extension function defined here
that maps a category's `colorRole` string token to the current `MaterialTheme.colorScheme`
property. Keeps color resolution in the design system rather than scattered across
feature screens.

## Dependencies

```
:core:designsystem
 └── :core:model   (TaskCategory.colorRole)
```

Plus Compose BOM, Material 3, and Material Icons Extended.

## Convention plugin

```kotlin
apply plugin: 'todoapp.android.library'
apply plugin: 'todoapp.android.library.compose'
```
