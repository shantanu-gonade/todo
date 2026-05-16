package com.eulerity.todo.feature.history

import com.eulerity.todo.core.ui.TaskUi

// History is read-only: there are no user intents beyond navigation, so this
// feature intentionally has no Intent/Effect files. The asymmetry with
// :feature:today is deliberate — do not add an Intent type "for consistency".
data class HistoryUiState(
    val tasks: List<TaskUi> = emptyList(),
    val isLoading: Boolean = true,
)
