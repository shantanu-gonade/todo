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

/**
 * Main theme composable for the Todo app.
 *
 * Compose rule 1: Status-bar icon appearance is synced with [darkTheme] via
 * [DisposableEffect] — this is a side effect that bridges the imperative
 * [WindowCompat] API into the declarative Compose world.
 *
 * Dynamic color is applied on API 31+ (Android 12+); static [LightColors] /
 * [DarkColors] are the fallback for older devices.
 */
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
        // Compose rule 1: sync status-bar icon tint with theme inside DisposableEffect.
        // DisposableEffect is correct here: we need to run the imperative Window API
        // call whenever darkTheme changes, and clean up is a no-op (Window state is
        // managed by the Activity lifecycle).
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
