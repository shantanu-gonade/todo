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

package com.eulerity.todo.core.ui

import com.eulerity.todo.core.model.Task
import com.eulerity.todo.core.model.TaskCategory
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * UI-facing representation of a task.
 *
 * Decoupled from the domain [Task] so the presentation layer never leaks
 * kotlinx-datetime formatting concerns into composables.
 */
data class TaskUi(
    val id: String,
    val title: String,
    val isCompleted: Boolean,
    /** Formatted expiry string (e.g. "2:30 PM"), or null if no expiry is set. */
    val expiryLabel: String?,
    val isExpired: Boolean = false,
    /** Raw expiry time, preserved so the edit sheet can pre-populate without re-parsing the label. */
    val expiryTime: LocalTime? = null,
    /** The category assigned to this task. */
    val category: TaskCategory = TaskCategory.NONE,
    /** The date this task was created; used for grouping in the History screen. */
    val createdDate: LocalDate? = null,
)

private const val HOURS_IN_HALF_DAY = 12
private const val NOON_HOUR = 12

/**
 * Converts a [LocalTime] to a 12-hour AM/PM label (e.g. "2:30 PM").
 */
fun LocalTime.to12hLabel(): String {
    val h = if (hour % HOURS_IN_HALF_DAY == 0) NOON_HOUR else hour % HOURS_IN_HALF_DAY
    val amPm = if (hour < NOON_HOUR) "AM" else "PM"
    return "%d:%02d %s".format(h, minute, amPm)
}

/**
 * Default formatter: 12-hour "h:mm AM/PM". Pure function; overridable in tests.
 *
 * Compose rule 6: this is a plain function — no [androidx.compose.ui.platform.LocalContext],
 * no composable context. The ViewModel calls it when mapping domain → UI model
 * so composables receive already-formatted strings.
 */
fun defaultExpiryFormatter(time: LocalTime): String = time.to12hLabel()

/**
 * Maps a domain [Task] to its UI representation.
 *
 * Compose rule 6: pure function — same input always produces same output.
 * The [formatExpiry] parameter has a default so production code stays concise
 * and tests can override without launching a full Compose tree.
 */
fun Task.asTaskUi(
    formatExpiry: (LocalTime) -> String = ::defaultExpiryFormatter,
): TaskUi = TaskUi(
    id = id,
    title = title,
    isCompleted = isCompleted,
    expiryLabel = expiryTime?.let(formatExpiry),
    expiryTime = expiryTime,
    category = category,
    createdDate = createdDate,
)
