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

import com.eulerity.todo.core.model.Task
import com.eulerity.todo.core.model.TaskCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalTime

interface TaskRepository {
    /** Tasks for the current day. Re-evaluates automatically when the day rolls over. */
    fun observeTodaysTasks(): Flow<List<Task>>

    /** Tasks from days before today — the history view. */
    fun observeExpiredTasks(): Flow<List<Task>>

    suspend fun addTask(title: String, expiryTime: LocalTime?, category: TaskCategory = TaskCategory.NONE)

    suspend fun updateTask(id: String, title: String, expiryTime: LocalTime?, category: TaskCategory)

    suspend fun getTask(id: String): Task?

    suspend fun setCompleted(id: String, completed: Boolean)

    suspend fun deleteTask(id: String)
}
