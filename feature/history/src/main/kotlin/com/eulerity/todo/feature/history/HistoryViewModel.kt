package com.eulerity.todo.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eulerity.todo.core.domain.ObserveExpiredTasksUseCase
import com.eulerity.todo.core.ui.asTaskUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for the History (expired tasks) screen.
 *
 * History is intentionally read-only — users can view past expired tasks but
 * cannot modify them. There are therefore no intents, no effects, and no local
 * state: the entire UI state is derived from a single upstream Flow.
 *
 * [SharingStarted.WhileSubscribed] with a 5-second timeout keeps the upstream
 * alive during configuration changes without leaking resources when the screen
 * leaves the back stack.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    observeExpiredTasks: ObserveExpiredTasksUseCase,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> =
        observeExpiredTasks()
            .map { tasks ->
                HistoryUiState(
                    tasks = tasks.map { it.asTaskUi() }, // Compose rule 6: pure mapper
                    isLoading = false,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = HistoryUiState(isLoading = true),
            )
}
