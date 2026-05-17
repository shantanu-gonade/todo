/*
 * Copyright 2026 Eulerity, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.eulerity.todo.feature.today

import com.eulerity.todo.core.model.TaskCategory
import kotlinx.datetime.LocalTime

/**
 * All user actions on the Today screen, expressed as a sealed hierarchy.
 *
 * The ViewModel's single [TodayViewModel.onIntent] entry point dispatches on
 * these — the composable layer never calls ViewModel methods directly.
 */
sealed interface TodayIntent {
    /** User typed or cleared text in the "new/edit task" title field. */
    data class DraftTitleChanged(val value: String) : TodayIntent

    /** User tapped the FAB to open the add-task sheet. */
    data object OpenAddSheet : TodayIntent

    /** User tapped the "Add task" button inside the add-task sheet. */
    data object AddTaskClicked : TodayIntent

    /** User tapped the task title to open the edit sheet pre-populated. */
    data class EditTaskClicked(val taskId: String) : TodayIntent

    /** User tapped "Save" inside the edit sheet. */
    data object SaveEditClicked : TodayIntent

    /** User dismissed the add/edit sheet (swipe or outside tap). */
    data object AddSheetDismissed : TodayIntent

    /** User tapped a category chip in the chip row. */
    data class DraftCategoryChanged(val category: TaskCategory) : TodayIntent

    /** User selected (or cleared) an expiry time in the time picker. */
    data class DraftExpiryTimeChanged(val time: LocalTime?) : TodayIntent

    /** User toggled the completion checkbox on a task row. */
    data class TaskCompletionToggled(val id: String) : TodayIntent

    /** User tapped the delete icon on a task row. */
    data class DeleteTask(val id: String) : TodayIntent

    /**
     * User dragged a task card and dropped it onto a different category section.
     * The ViewModel will call [UpdateTaskUseCase] to persist the new category.
     */
    data class TaskDroppedToCategory(val taskId: String, val newCategory: TaskCategory) : TodayIntent
}
