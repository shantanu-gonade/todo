package com.eulerity.todo.feature.history

import com.eulerity.todo.core.ui.TaskUi
import kotlinx.datetime.LocalDate

// History is read-only: there are no user intents beyond navigation, so this
// feature intentionally has no Intent/Effect files. The asymmetry with
// :feature:today is deliberate — do not add an Intent type "for consistency".
data class HistoryUiState(
    /**
     * Tasks grouped by [LocalDate] in descending order (most recent day first).
     * Each entry is a date → tasks pair where tasks within the day are ordered
     * by [createdAt] ascending.
     */
    val tasksByDate: List<Pair<LocalDate, List<TaskUi>>> = emptyList(),
    val isLoading: Boolean = true,
) {
    /** Convenience: true when there are no expired tasks to show. */
    val isEmpty: Boolean get() = tasksByDate.isEmpty()
}
