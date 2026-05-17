package com.eulerity.todo.core.domain

import com.eulerity.todo.core.common.DateTimeProvider
import com.eulerity.todo.core.data.TaskRepository
import com.eulerity.todo.core.model.TaskCategory
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

/**
 * Updates an existing task's title, expiry time, and category.
 *
 * Applies the same validation rules as [AddTaskUseCase]:
 * - title must not be blank
 * - expiry time, if provided, must not already be in the past
 *
 * Returns [Result.failure] with a human-readable message on validation error.
 */
class UpdateTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val dateTimeProvider: DateTimeProvider,
) {
    suspend operator fun invoke(
        id: String,
        title: String,
        expiryTime: LocalTime?,
        category: TaskCategory,
    ): Result<Unit> {
        if (title.isBlank()) {
            return Result.failure(IllegalArgumentException("Title can't be empty"))
        }
        if (expiryTime != null) {
            val nowLocalTime = dateTimeProvider.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .time
            if (expiryTime < nowLocalTime) {
                return Result.failure(
                    IllegalArgumentException("Expiry time has already passed"),
                )
            }
        }
        return runCatching { repository.updateTask(id, title.trim(), expiryTime, category) }
    }
}
