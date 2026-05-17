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

package com.eulerity.todo.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eulerity.todo.core.domain.ObserveExpiredTasksUseCase
import com.eulerity.todo.core.ui.TaskUi
import com.eulerity.todo.core.ui.asTaskUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate
import javax.inject.Inject

/** Sentinel date used when a task has no [createdDate]; sorts to the bottom of history. */
private val EPOCH_DATE = LocalDate(1970, 1, 1)

/**
 * ViewModel for the History (expired tasks) screen.
 *
 * History is intentionally read-only — users can view past expired tasks but
 * cannot modify them. There are therefore no intents, no effects, and no local
 * state: the entire UI state is derived from a single upstream Flow.
 *
 * Tasks are grouped by [LocalDate] in descending order (most recent day first)
 * so the History screen can render date-keyed sticky headers.
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
                // Map domain → UI, then group by createdDate (descending — newest day first).
                val uiTasks: List<TaskUi> = tasks.map { it.asTaskUi() }
                val grouped: List<Pair<LocalDate, List<TaskUi>>> = uiTasks
                    .groupBy { it.createdDate }
                    .entries
                    .sortedByDescending { it.key }   // most-recent date at top
                    .map { (date, group) -> (date ?: EPOCH_DATE) to group }

                HistoryUiState(
                    tasksByDate = grouped,
                    isLoading = false,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = HistoryUiState(isLoading = true),
            )
}
