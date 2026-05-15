package com.eulerity.todo.feature.today

import com.eulerity.todo.core.ui.TaskUi
import kotlinx.datetime.LocalTime

/**
 * Immutable UI state for the Today screen.
 *
 * All presentation logic produces a new copy of this snapshot — there is no
 * shared mutable state between the ViewModel and the composable tree.
 *
 * @param tasks          Today's tasks, already mapped to [TaskUi] display model.
 * @param isLoading      True only during the initial cold-start before the
 *                       first emission arrives from the combined Flow.
 * @param addSheetVisible Whether the "Add Task" modal bottom sheet is open.
 * @param draftTitle     Text field value for a new task being composed.
 * @param draftExpiryTime Optional expiry time chosen in the bottom sheet.
 * @param validationError Inline error message to show below the title field,
 *                        or null when there is nothing to report.
 */
data class TodayUiState(
    val tasks: List<TaskUi> = emptyList(),
    val isLoading: Boolean = true,
    val addSheetVisible: Boolean = false,
    val draftTitle: String = "",
    val draftExpiryTime: LocalTime? = null,
    val validationError: String? = null,
)
