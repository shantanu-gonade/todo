package com.eulerity.todo.core.designsystem.component

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * App-wide top app bar using M3 [CenterAlignedTopAppBar].
 *
 * Thin wrapper that centralises the default [colors] so every screen
 * gets a consistent surface-colored bar without repeating the token call.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
) {
    CenterAlignedTopAppBar(
        title = { Text(text = title) },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = { actions() },
        colors = colors,
    )
}
