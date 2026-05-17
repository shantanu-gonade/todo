package com.eulerity.todo.core.ui

import com.eulerity.todo.core.model.Task
import com.eulerity.todo.core.model.TaskCategory
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
)

/**
 * Converts a [LocalTime] to a 12-hour AM/PM label (e.g. "2:30 PM").
 */
fun LocalTime.to12hLabel(): String {
    val h = if (hour % 12 == 0) 12 else hour % 12
    val amPm = if (hour < 12) "AM" else "PM"
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
)
