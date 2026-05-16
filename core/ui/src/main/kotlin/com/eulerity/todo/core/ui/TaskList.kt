package com.eulerity.todo.core.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eulerity.todo.core.designsystem.theme.TodoTheme

/**
 * Stateless lazy list of [TaskCard]s.
 *
 * Compose rule 5: typed, stable callbacks — the screen above passes a single
 * stable lambda reference per action. Inside the [items] block we build the
 * per-item lambdas by capturing only [id] from the item, so the screen-level
 * callbacks are not re-allocated per frame.
 *
 * [key] is set to [TaskUi.id] so Compose can correctly animate insertions,
 * deletions, and reorders without re-composing the whole list.
 */
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
                // History: read-only cards, no swipe gesture
                TaskCard(
                    task = task,
                    onToggle = { checked -> onToggle(task.id, checked) },
                    onDelete = { onDelete(task.id) },
                    modifier = Modifier.animateItem(),
                    readOnly = true,
                )
            } else {
                // Today: swipe-left to delete
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            onDelete(task.id)
                            true
                        } else false
                    },
                )
                // Reset if the item wasn't removed (e.g. reactive list didn't update)
                LaunchedEffect(task.id) {
                    if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                        dismissState.reset()
                    }
                }
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = MaterialTheme.colorScheme.errorContainer,
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

// Compose rule 7: @Preview wraps in TodoTheme
@Preview(showBackground = true, name = "TaskList — Mixed")
@Composable
private fun TaskListPreview() {
    TodoTheme {
        TaskList(
            tasks = listOf(
                TaskUi(id = "1", title = "Morning standup", isCompleted = true, expiryLabel = "09:30"),
                TaskUi(id = "2", title = "Review PR #42", isCompleted = false, expiryLabel = null),
                TaskUi(id = "3", title = "Write release notes", isCompleted = false, expiryLabel = "17:00"),
            ),
            onToggle = { _, _ -> },
            onDelete = { },
            modifier = Modifier.padding(0.dp),
        )
    }
}
