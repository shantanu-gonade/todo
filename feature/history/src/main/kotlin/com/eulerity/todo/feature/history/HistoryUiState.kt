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
