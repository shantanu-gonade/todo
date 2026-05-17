package com.eulerity.todo.core.ui

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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eulerity.todo.core.designsystem.theme.TodoTheme
import com.eulerity.todo.core.model.TaskCategory

/**
 * Stateless lazy list of [TaskCard]s.
 *
 * When tasks span 2 or more distinct categories the list groups them by category
 * with sticky section headers (spec C6). Tasks with [TaskCategory.NONE] appear last
 * under an "Uncategorized" header. If all tasks share one category the list renders
 * flat with no headers.
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
    onEdit: ((id: String) -> Unit)? = null,
) {
    // Spec C6: group by category only when 2+ distinct categories are present.
    val distinctCategories = tasks.map { it.category }.toSet()
    val useGroupedLayout = distinctCategories.size >= 2

    if (useGroupedLayout) {
        GroupedTaskList(
            tasks = tasks,
            onToggle = onToggle,
            onDelete = onDelete,
            modifier = modifier,
            contentPadding = contentPadding,
            readOnly = readOnly,
            onEdit = onEdit,
        )
    } else {
        FlatTaskList(
            tasks = tasks,
            onToggle = onToggle,
            onDelete = onDelete,
            modifier = modifier,
            contentPadding = contentPadding,
            readOnly = readOnly,
            onEdit = onEdit,
        )
    }
}

// ---------------------------------------------------------------------------
// Private helpers
// ---------------------------------------------------------------------------

/**
 * Flat list — all tasks share one category (or the list is empty).
 * Identical to the original implementation.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FlatTaskList(
    tasks: List<TaskUi>,
    onToggle: (id: String, checked: Boolean) -> Unit,
    onDelete: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    readOnly: Boolean = false,
    onEdit: ((id: String) -> Unit)? = null,
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
            TaskRow(
                task = task,
                onToggle = onToggle,
                onDelete = onDelete,
                readOnly = readOnly,
                onEdit = onEdit,
                modifier = Modifier.animateItem(),
            )
        }
    }
}

/**
 * Grouped list with sticky section headers (spec C6).
 *
 * Group order: non-NONE categories in [TaskCategory] declaration order, then
 * NONE tasks collected at the bottom under "Uncategorized".
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun GroupedTaskList(
    tasks: List<TaskUi>,
    onToggle: (id: String, checked: Boolean) -> Unit,
    onDelete: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    readOnly: Boolean = false,
    onEdit: ((id: String) -> Unit)? = null,
) {
    // Stable enum declaration order; NONE goes to the back.
    val categoryOrder = TaskCategory.entries.filter { it != TaskCategory.NONE } + listOf(TaskCategory.NONE)

    // Build sections: only include categories that actually have tasks.
    val grouped: List<Pair<TaskCategory, List<TaskUi>>> = categoryOrder
        .mapNotNull { cat ->
            val group = tasks.filter { it.category == cat }
            if (group.isEmpty()) null else cat to group
        }

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        grouped.forEach { (category, group) ->
            // Sticky header — "Uncategorized" label for NONE tasks.
            val headerLabel = if (category == TaskCategory.NONE) "Uncategorized" else category.label
            stickyHeader(key = "header_${category.name}", contentType = "header") {
                CategorySectionHeader(label = headerLabel)
            }

            items(
                items = group,
                key = { it.id },
                contentType = { "task" },
            ) { task ->
                TaskRow(
                    task = task,
                    onToggle = onToggle,
                    onDelete = onDelete,
                    readOnly = readOnly,
                    onEdit = onEdit,
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

/** Sticky section header composable for a category group. */
@Composable
private fun CategorySectionHeader(label: String) {
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

/** A single row in the list — handles both readOnly and swipe-to-dismiss modes. */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TaskRow(
    task: TaskUi,
    onToggle: (id: String, checked: Boolean) -> Unit,
    onDelete: (id: String) -> Unit,
    readOnly: Boolean,
    onEdit: ((id: String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (readOnly) {
        // History: read-only cards, no swipe gesture
        TaskCard(
            task = task,
            onToggle = { checked -> onToggle(task.id, checked) },
            onDelete = { onDelete(task.id) },
            modifier = modifier,
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
            modifier = modifier,
        ) {
            TaskCard(
                task = task,
                onToggle = { checked -> onToggle(task.id, checked) },
                onDelete = { onDelete(task.id) },
                onEdit = onEdit?.let { cb -> { cb(task.id) } },
            )
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
