package com.eulerity.todo.core.ui

import com.eulerity.todo.core.model.Task
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
    /** Formatted expiry string (e.g. "14:30"), or null if no expiry is set. */
    val expiryLabel: String?,
)

/**
 * Default formatter: 24-hour "HH:mm". Pure function; overridable in tests.
 *
 * Compose rule 6: this is a plain function — no [androidx.compose.ui.platform.LocalContext],
 * no composable context. The ViewModel calls it when mapping domain → UI model
 * so composables receive already-formatted strings.
 */
fun defaultExpiryFormatter(time: LocalTime): String =
    "%02d:%02d".format(time.hour, time.minute)

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
)
