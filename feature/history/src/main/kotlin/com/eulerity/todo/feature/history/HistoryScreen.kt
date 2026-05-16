package com.eulerity.todo.feature.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eulerity.todo.core.designsystem.component.TodoEmptyState
import com.eulerity.todo.core.designsystem.component.TodoTopAppBar
import com.eulerity.todo.core.designsystem.theme.TodoTheme
import com.eulerity.todo.core.ui.TaskList
import com.eulerity.todo.core.ui.TaskUi

// ---------------------------------------------------------------------------
// Stateful entry point — wired to Hilt + lifecycle
// ---------------------------------------------------------------------------

/**
 * Stateful route composable. Owns the ViewModel and collects state
 * lifecycle-aware. History has no effects — the only navigation event is
 * back, forwarded to the caller via [onBack].
 */
@Composable
fun HistoryRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HistoryScreen(
        uiState = uiState,
        onBack = onBack,
        modifier = modifier,
    )
}

// ---------------------------------------------------------------------------
// Stateless screen — receives all state and callbacks as parameters
// ---------------------------------------------------------------------------

/**
 * Stateless History screen. History is read-only — there are no task mutations.
 *
 * [TaskList] receives no-op callbacks (Compose rule 5: callbacks are defined
 * once as stable `remember`ed references so the list never re-composes due to
 * callback identity changes).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Compose rule 5: define stable no-op callbacks once via remember so TaskList
    // never re-composes due to lambda identity changes across recompositions.
    val noOpToggle: (id: String, checked: Boolean) -> Unit = remember { { _, _ -> } }
    val noOpDelete: (id: String) -> Unit = remember { { } }

    Scaffold(
        modifier = modifier,
        topBar = {
            TodoTopAppBar(
                title = "Expired",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "Navigate back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (uiState.tasks.isEmpty()) {
                TodoEmptyState(
                    headline = "No expired tasks",
                    supportingText = "Tasks from previous days will appear here",
                )
            } else {
                // Read-only: onToggle and onDelete are no-ops. The checkbox/delete
                // affordances still render (reusing the shared TaskList + TaskCard
                // components) but user interactions produce no state change — a
                // deliberate product decision. Alternatively render a read-only variant;
                // the no-op approach is simpler and avoids component duplication.
                TaskList(
                    tasks = uiState.tasks,
                    onToggle = noOpToggle,
                    onDelete = noOpDelete,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — Compose rule 7: always wrapped in TodoTheme
// ---------------------------------------------------------------------------

@Preview(showBackground = true, name = "History — Empty")
@Composable
private fun HistoryScreenEmptyPreview() {
    TodoTheme {
        HistoryScreen(
            uiState = HistoryUiState(isLoading = false),
            onBack = {},
        )
    }
}

@Preview(showBackground = true, name = "History — With Expired Tasks")
@Composable
private fun HistoryScreenTasksPreview() {
    TodoTheme {
        HistoryScreen(
            uiState = HistoryUiState(
                isLoading = false,
                tasks = listOf(
                    TaskUi(id = "1", title = "Morning standup", isCompleted = true, expiryLabel = "09:30"),
                    TaskUi(id = "2", title = "Submit report", isCompleted = false, expiryLabel = "17:00"),
                    TaskUi(id = "3", title = "Team lunch booking", isCompleted = false, expiryLabel = null),
                ),
            ),
            onBack = {},
        )
    }
}
