package com.eulerity.todo.core.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eulerity.todo.core.designsystem.theme.TodoTheme
import com.eulerity.todo.core.model.TaskCategory

/**
 * Stateless lazy list of [TaskCard]s.
 *
 * When tasks span 2+ distinct categories the list groups them by category with sticky
 * section headers (spec C6). Within the grouped layout, long-press + drag moves a
 * task to a different category section ([onCategoryDrop]).
 * Tasks with [TaskCategory.NONE] appear last under "Uncategorized".
 * If all tasks share one category the list renders flat with no headers or drag support.
 *
 * @param onCategoryDrop Called when a task is dragged and dropped onto a different
 *   category section. Only fired in the grouped layout and only when the target
 *   category differs from the task's current category. No-op lambda by default so
 *   callers that don't need drag (e.g. History) can ignore it.
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
    onCategoryDrop: ((taskId: String, category: TaskCategory) -> Unit)? = null,
) {
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
            onCategoryDrop = onCategoryDrop,
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
 * Drag state for the grouped list. Tracks the task being dragged and its
 * current drag offset in window coordinates.
 */
private data class DragState(
    val taskId: String,
    val currentOffset: Offset,
)

/**
 * Grouped list with sticky section headers (spec C6) and long-press drag-to-move.
 *
 * Each category section header registers its position so drag-drop can determine
 * which section the user dropped the card into. The dragged card is lifted visually
 * (graphicsLayer scale + alpha) while a ghost placeholder remains in place.
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
    onCategoryDrop: ((taskId: String, category: TaskCategory) -> Unit)? = null,
) {
    val categoryOrder = TaskCategory.entries.filter { it != TaskCategory.NONE } + listOf(TaskCategory.NONE)
    val grouped: List<Pair<TaskCategory, List<TaskUi>>> = categoryOrder
        .mapNotNull { cat ->
            val group = tasks.filter { it.category == cat }
            if (group.isEmpty()) null else cat to group
        }

    // Track where each category section header sits in window coordinates (top-Y).
    val sectionTopMap = remember { mutableMapOf<TaskCategory, Float>() }

    var dragState by remember { mutableStateOf<DragState?>(null) }

    // Compute drop-target status for every category in composable scope (before LazyColumn)
    // so we avoid calling remember/derivedStateOf inside the non-composable LazyListScope.forEach.
    val dropTargets: Map<TaskCategory, Boolean> by remember(dragState, categoryOrder) {
        derivedStateOf {
            if (dragState == null || onCategoryDrop == null) {
                emptyMap()
            } else {
                val dragY = dragState!!.currentOffset.y
                categoryOrder.associateWith { cat ->
                    val thisTop = sectionTopMap[cat] ?: return@associateWith false
                    val nextTop = categoryOrder
                        .dropWhile { it != cat }
                        .drop(1)
                        .firstNotNullOfOrNull { sectionTopMap[it] }
                        ?: Float.MAX_VALUE
                    dragY in thisTop..nextTop
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        state = rememberLazyListState(),
    ) {
        grouped.forEach { (category, group) ->
            val headerLabel = if (category == TaskCategory.NONE) "Uncategorized" else category.label
            val isDropTarget = dropTargets[category] ?: false

            stickyHeader(key = "header_${category.name}", contentType = "header") {
                CategorySectionHeader(
                    label = headerLabel,
                    isDropTarget = isDropTarget,
                    modifier = Modifier.onGloballyPositioned { coords ->
                        sectionTopMap[category] = coords.positionInWindow().y
                    },
                )
            }

            items(
                items = group,
                key = { it.id },
                contentType = { "task" },
            ) { task ->
                val isDragging = dragState?.taskId == task.id

                DraggableTaskRow(
                    task = task,
                    onToggle = onToggle,
                    onDelete = onDelete,
                    readOnly = readOnly,
                    onEdit = onEdit,
                    isDragging = isDragging,
                    onDragStart = { offset ->
                        dragState = DragState(taskId = task.id, currentOffset = offset)
                    },
                    onDragMove = { delta ->
                        dragState = dragState?.copy(
                            currentOffset = dragState!!.currentOffset + delta,
                        )
                    },
                    onDragEnd = {
                        val state = dragState
                        if (state != null && onCategoryDrop != null) {
                            val dragY = state.currentOffset.y
                            val targetCategory = categoryOrder.firstOrNull { cat ->
                                val top = sectionTopMap[cat] ?: return@firstOrNull false
                                val nextTop = categoryOrder
                                    .dropWhile { it != cat }
                                    .drop(1)
                                    .firstNotNullOfOrNull { sectionTopMap[it] }
                                    ?: Float.MAX_VALUE
                                dragY in top..nextTop
                            }
                            if (targetCategory != null && targetCategory != task.category) {
                                onCategoryDrop(task.id, targetCategory)
                            }
                        }
                        dragState = null
                    },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

/** Sticky section header — highlighted when a card is dragged over it. */
@Composable
private fun CategorySectionHeader(
    label: String,
    modifier: Modifier = Modifier,
    isDropTarget: Boolean = false,
) {
    val borderColor = if (isDropTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = if (isDropTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (isDropTarget) Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.small,
                ) else Modifier
            )
            .padding(horizontal = 4.dp, vertical = 6.dp),
    )
}

/**
 * A draggable version of [TaskRow]. Long-press activates drag; release fires [onDragEnd].
 * While dragging, the card is rendered with reduced alpha and a slight scale-up so it
 * appears "lifted". When [isDragging] is true a semi-transparent ghost renders underneath.
 *
 * Coordinate space: [onDragStart] receives a local touch offset (relative to this composable).
 * We combine it with the card's own window position (tracked via [onGloballyPositioned]) to
 * seed [DragState.currentOffset] in window coordinate space — the same space used by
 * [sectionTopMap] — so drag-Y comparisons against section tops work correctly.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun DraggableTaskRow(
    task: TaskUi,
    onToggle: (id: String, checked: Boolean) -> Unit,
    onDelete: (id: String) -> Unit,
    readOnly: Boolean,
    onEdit: ((id: String) -> Unit)?,
    isDragging: Boolean,
    onDragStart: (Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Track this card's top-left corner in window coordinates so we can translate
    // the local touch offset into the window coordinate space expected by sectionTopMap.
    var cardWindowPosition by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                val pos = coords.positionInWindow()
                cardWindowPosition = Offset(pos.x, pos.y)
            }
            .pointerInput(task.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { localOffset ->
                        // Convert local touch offset to window space by adding the card's
                        // window position. This keeps DragState.currentOffset in the same
                        // coordinate space as sectionTopMap entries.
                        onDragStart(cardWindowPosition + localOffset)
                    },
                    onDrag = { _, dragAmount -> onDragMove(dragAmount) },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                )
            }
            .graphicsLayer {
                if (isDragging) {
                    scaleX = 1.04f
                    scaleY = 1.04f
                    alpha = 0.85f
                    shadowElevation = 8.dp.toPx()
                }
            },
    ) {
        TaskRow(
            task = task,
            onToggle = onToggle,
            onDelete = onDelete,
            readOnly = readOnly,
            onEdit = onEdit,
        )
    }
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
        TaskCard(
            task = task,
            onToggle = { checked -> onToggle(task.id, checked) },
            onDelete = { onDelete(task.id) },
            modifier = modifier,
            readOnly = true,
        )
    } else {
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) {
                    onDelete(task.id)
                    true
                } else false
            },
        )
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
