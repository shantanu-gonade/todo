package com.eulerity.todo.feature.today

import kotlinx.datetime.LocalTime

/**
 * All user actions on the Today screen, expressed as a sealed hierarchy.
 *
 * The ViewModel's single [TodayViewModel.onIntent] entry point dispatches on
 * these — the composable layer never calls ViewModel methods directly.
 */
sealed interface TodayIntent {
    /** User typed or cleared text in the "new task" title field. */
    data class DraftTitleChanged(val value: String) : TodayIntent

    /** User tapped the FAB to open the add-task sheet. */
    data object OpenAddSheet : TodayIntent

    /** User tapped the "Add" button inside the add-task sheet. */
    data object AddTaskClicked : TodayIntent

    /** User dismissed the add-task sheet (swipe or outside tap). */
    data object AddSheetDismissed : TodayIntent

    /** User selected (or cleared) an expiry time in the time picker. */
    data class DraftExpiryTimeChanged(val time: LocalTime?) : TodayIntent

    /** User toggled the completion checkbox on a task row. */
    data class TaskCompletionToggled(val id: String) : TodayIntent

    /** User tapped the delete icon on a task row. */
    data class DeleteTask(val id: String) : TodayIntent
}
