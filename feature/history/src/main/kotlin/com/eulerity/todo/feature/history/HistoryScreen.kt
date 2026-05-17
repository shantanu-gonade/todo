package com.eulerity.todo.feature.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eulerity.todo.core.designsystem.component.TodoEmptyState
import com.eulerity.todo.core.designsystem.component.TodoTopAppBar
import com.eulerity.todo.core.designsystem.theme.TodoTheme
import com.eulerity.todo.core.ui.TaskCard
import com.eulerity.todo.core.ui.TaskUi
import kotlinx.datetime.LocalDate

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
 * Tasks are grouped by date with sticky section headers (most recent date first).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TodoTopAppBar(
                title = "History",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
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
            contentAlignment = Alignment.Center,
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator()
                uiState.isEmpty -> TodoEmptyState(
                    headline = "No expired tasks",
                    supportingText = "Tasks from previous days will appear here",
                )
                else -> HistoryDateGroupedList(
                    tasksByDate = uiState.tasksByDate,
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.TopStart),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Date-grouped lazy list
// ---------------------------------------------------------------------------

/**
 * Renders expired tasks grouped by [LocalDate] with sticky section headers.
 * Most-recent date appears first (the VM already sorts descending).
 * Tasks within a date section are read-only — checkboxes and delete icons
 * are disabled/hidden.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryDateGroupedList(
    tasksByDate: List<Pair<LocalDate, List<TaskUi>>>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tasksByDate.forEach { (date, tasks) ->
            stickyHeader(key = "date_${date}", contentType = "date_header") {
                DateSectionHeader(date = date)
            }
            items(
                items = tasks,
                key = { it.id },
                contentType = { "task" },
            ) { task ->
                TaskCard(
                    task = task,
                    onToggle = {},
                    onDelete = {},
                    readOnly = true,
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

/**
 * Sticky header showing the date label for a group of expired tasks.
 * Format: "Mon, May 16" (or equivalent locale-friendly string).
 */
@Composable
private fun DateSectionHeader(date: LocalDate) {
    val label = formatHistoryDate(date)
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 4.dp, vertical = 6.dp),
    )
}

/**
 * Pure function: formats a [LocalDate] as a readable history group label.
 * Example: 2026-05-16 → "Sat, May 16"
 */
internal fun formatHistoryDate(date: LocalDate): String {
    val dayName = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    val monthName = date.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    return "$dayName, $monthName ${date.dayOfMonth}"
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

@Preview(showBackground = true, name = "History — With Expired Tasks Grouped")
@Composable
private fun HistoryScreenTasksPreview() {
    TodoTheme {
        HistoryScreen(
            uiState = HistoryUiState(
                isLoading = false,
                tasksByDate = listOf(
                    LocalDate(2026, 5, 16) to listOf(
                        TaskUi(id = "1", title = "Morning standup", isCompleted = true, expiryLabel = "09:30", createdDate = LocalDate(2026, 5, 16)),
                        TaskUi(id = "2", title = "Submit report", isCompleted = false, expiryLabel = "17:00", createdDate = LocalDate(2026, 5, 16)),
                    ),
                    LocalDate(2026, 5, 15) to listOf(
                        TaskUi(id = "3", title = "Team lunch booking", isCompleted = false, expiryLabel = null, createdDate = LocalDate(2026, 5, 15)),
                    ),
                ),
            ),
            onBack = {},
        )
    }
}
