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

package com.eulerity.todo.core.data

import com.eulerity.todo.core.database.TaskDao
import com.eulerity.todo.core.database.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/**
 * Hand-written in-memory fake for [TaskDao].
 * Uses [MutableStateFlow] so Flow operators react to mutations.
 */
class FakeTaskDao : TaskDao {
    private val tasks = MutableStateFlow<List<TaskEntity>>(emptyList())

    override fun observeTasksForDate(day: LocalDate): Flow<List<TaskEntity>> =
        tasks.map { list -> list.filter { it.createdDate == day } }

    override fun observeTasksBeforeDate(day: LocalDate): Flow<List<TaskEntity>> =
        tasks.map { list -> list.filter { it.createdDate < day } }

    override suspend fun upsert(task: TaskEntity) {
        tasks.value = tasks.value
            .filterNot { it.id == task.id }
            .plus(task)
    }

    override suspend fun updateCompletion(id: String, completed: Boolean) {
        tasks.value = tasks.value.map { task ->
            if (task.id == id) task.copy(isCompleted = completed) else task
        }
    }

    override suspend fun deleteById(id: String) {
        tasks.value = tasks.value.filterNot { it.id == id }
    }

    override suspend fun getById(id: String): TaskEntity? =
        tasks.value.find { it.id == id }

    override suspend fun updateTask(id: String, title: String, expiryTime: Int?, category: String) {
        tasks.value = tasks.value.map { task ->
            if (task.id == id) task.copy(title = title, expiryTime = null, category = category)
            else task
        }
    }
}
