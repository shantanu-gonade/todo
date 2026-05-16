package com.eulerity.todo.core.domain

import com.eulerity.todo.core.common.DateTimeProvider
import com.eulerity.todo.core.data.TaskRepository
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

class AddTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val dateTimeProvider: DateTimeProvider,
) {
    suspend operator fun invoke(title: String, expiryTime: LocalTime?): Result<Unit> {
        if (title.isBlank()) {
            return Result.failure(IllegalArgumentException("Title can't be empty"))
        }
        // Reject an expiry time already in the past for today.
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
        return runCatching { repository.addTask(title.trim(), expiryTime) }
    }
}
