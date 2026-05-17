package com.eulerity.todo.feature.today

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import com.eulerity.todo.core.designsystem.component.TodoEmptyState
import com.eulerity.todo.core.designsystem.component.TodoTopAppBar
import com.eulerity.todo.core.designsystem.theme.TodoTheme
import com.eulerity.todo.core.ui.TaskList
import com.eulerity.todo.core.ui.TaskUi

// ---------------------------------------------------------------------------
// Stateful entry point — wired to Hilt + lifecycle
// ---------------------------------------------------------------------------

/**
 * Stateful route composable. Owns the ViewModel, collects state
 * lifecycle-aware, and routes one-shot [TodayEffect]s to side-effects
 * (haptics, Snackbar) without leaking collectors.
 *
 * Compose rule 2: effects are consumed inside [LaunchedEffect] +
 * `flowWithLifecycle` so collection is tied to STARTED lifecycle state.
 */
@Composable
fun TodayRoute(
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val lifecycleAwareEffects = remember(viewModel.effects, lifecycle) {
        viewModel.effects.flowWithLifecycle(lifecycle)
    }
    LaunchedEffect(lifecycleAwareEffects) {
        lifecycleAwareEffects.collect { effect ->
            when (effect) {
                TodayEffect.TaskCompletedHaptic ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                is TodayEffect.ShowError ->
                    snackbarHostState.showSnackbar(effect.message)

                is TodayEffect.ShowMessage ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    TodayScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
        onNavigateToHistory = onNavigateToHistory,
        modifier = modifier,
    )
}

// ---------------------------------------------------------------------------
// Stateless screen — receives all state and callbacks as parameters
// ---------------------------------------------------------------------------

/**
 * Stateless Today screen. All state is in [uiState]; all mutations go
 * through [onIntent]. The sheet is conditionally rendered inside this
 * composable (Compose rule 3 — visibility driven by state, not local var).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    uiState: TodayUiState,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onIntent: (TodayIntent) -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
) {
    // Compose rule 5: callbacks are remembered so their identity is stable across
    // recompositions. TaskList passes them into items{} without re-allocating per item.
    val onToggle: (id: String, checked: Boolean) -> Unit = remember(onIntent) {
        { id, _ -> onIntent(TodayIntent.TaskCompletionToggled(id)) }
    }
    val onDelete: (id: String) -> Unit = remember(onIntent) {
        { id -> onIntent(TodayIntent.DeleteTask(id)) }
    }
    // E1: tapping the task title opens the edit sheet
    val onEdit: (id: String) -> Unit = remember(onIntent) {
        { id -> onIntent(TodayIntent.EditTaskClicked(id)) }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TodoTopAppBar(
                title = "Today",
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = "View history",
                        )
                    }
                },
            )
        },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    onEdit = onEdit,
                )
            }
        }

        // Compose rule 3: sheet visibility driven entirely by state.
        if (uiState.addSheetVisible) {
            AddEditTaskSheet(
                draftTitle = uiState.draftTitle,
                draftExpiryTime = uiState.draftExpiryTime,
                validationError = uiState.validationError,
                onIntent = onIntent,
                editingTaskId = uiState.editingTaskId,
                draftCategory = uiState.draftCategory,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — Compose rule 7: always wrapped in TodoTheme
// ---------------------------------------------------------------------------

@Preview(showBackground = true, name = "Today — Empty")
@Composable
private fun TodayScreenEmptyPreview() {
    TodoTheme {
        TodayScreen(uiState = TodayUiState(isLoading = false))
    }
}

@Preview(showBackground = true, name = "Today — With Tasks")
@Composable
private fun TodayScreenTasksPreview() {
    TodoTheme {
        TodayScreen(
            uiState = TodayUiState(
                isLoading = false,
                tasks = listOf(
                    TaskUi(id = "1", title = "Morning standup", isCompleted = false, expiryLabel = "09:30"),
                    TaskUi(id = "2", title = "Review PR #42", isCompleted = true, expiryLabel = null),
                ),
            ),
        )
    }
}

@Preview(showBackground = true, name = "Today — Loading")
@Composable
private fun TodayScreenLoadingPreview() {
    TodoTheme {
        TodayScreen(uiState = TodayUiState(isLoading = true))
    }
}
