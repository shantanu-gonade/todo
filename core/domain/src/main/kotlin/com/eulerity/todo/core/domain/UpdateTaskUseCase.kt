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
        val validationError = validate(title, expiryTime)
        if (validationError != null) return validationError
        return runCatching { repository.updateTask(id, title.trim(), expiryTime, category) }
    }

    private fun validate(title: String, expiryTime: LocalTime?): Result<Unit>? {
        if (title.isBlank()) return Result.failure(IllegalArgumentException("Title can't be empty"))
        if (expiryTime != null) {
            val nowLocalTime = dateTimeProvider.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .time
            if (expiryTime < nowLocalTime) {
                return Result.failure(IllegalArgumentException("Expiry time has already passed"))
            }
        }
        return null
    }
}
