package com.eulerity.todo.feature.today

import com.eulerity.todo.core.model.TaskCategory
import com.eulerity.todo.core.ui.TaskUi
import kotlinx.datetime.LocalTime

/**
 * Immutable UI state for the Today screen.
 *
 * All presentation logic produces a new copy of this snapshot — there is no
 * shared mutable state between the ViewModel and the composable tree.
 *
 * @param tasks           Today's tasks, already mapped to [TaskUi] display model.
 * @param isLoading       True only during the initial cold-start before the
 *                        first emission arrives from the combined Flow.
 * @param addSheetVisible Whether the Add/Edit Task modal bottom sheet is open.
 * @param editingTaskId   Non-null when the sheet is open in edit mode; null in add mode.
 * @param draftTitle      Text field value for a task being composed or edited.
 * @param draftExpiryTime Optional expiry time chosen in the bottom sheet.
 * @param draftCategory   Category selected in the chip row.
 * @param validationError Inline error message to show below the title field,
 *                        or null when there is nothing to report.
 */
data class TodayUiState(
    val tasks: List<TaskUi> = emptyList(),
    val isLoading: Boolean = true,
    val addSheetVisible: Boolean = false,
    val editingTaskId: String? = null,
    val draftTitle: String = "",
    val draftExpiryTime: LocalTime? = null,
    val draftCategory: TaskCategory = TaskCategory.NONE,
    val validationError: String? = null,
)
