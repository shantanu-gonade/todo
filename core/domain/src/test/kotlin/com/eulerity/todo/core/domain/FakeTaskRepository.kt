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

import com.eulerity.todo.core.data.TaskRepository
import com.eulerity.todo.core.model.Task
import com.eulerity.todo.core.model.TaskCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalTime

class FakeTaskRepository : TaskRepository {
    /** Tracks (title, expiryTime) pairs added via [addTask]. */
    val added = mutableListOf<Pair<String, LocalTime?>>()

    /** Tracks (id, completed) pairs set via [setCompleted]. */
    val toggled = mutableListOf<Pair<String, Boolean>>()

    /** Tracks ids deleted via [deleteTask]. */
    val deleted = mutableListOf<String>()

    private val todaysTasks = MutableStateFlow<List<Task>>(emptyList())
    private val expiredTasks = MutableStateFlow<List<Task>>(emptyList())

    override fun observeTodaysTasks(): Flow<List<Task>> = todaysTasks
    override fun observeExpiredTasks(): Flow<List<Task>> = expiredTasks

    override suspend fun addTask(title: String, expiryTime: LocalTime?, category: TaskCategory) {
        added.add(title to expiryTime)
    }

    override suspend fun updateTask(id: String, title: String, expiryTime: LocalTime?, category: TaskCategory) {}

    override suspend fun getTask(id: String): Task? = null

    override suspend fun setCompleted(id: String, completed: Boolean) {
        toggled.add(id to completed)
    }

    override suspend fun deleteTask(id: String) {
        deleted.add(id)
    }
}
