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

import com.eulerity.todo.core.common.DateTimeProvider
import com.eulerity.todo.core.data.model.asDomain
import com.eulerity.todo.core.data.notification.TaskExpiryScheduler
import com.eulerity.todo.core.database.TaskDao
import com.eulerity.todo.core.database.TaskEntity
import com.eulerity.todo.core.model.Task
import com.eulerity.todo.core.model.TaskCategory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalTime
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineTaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val dateTimeProvider: DateTimeProvider,
    private val expiryScheduler: TaskExpiryScheduler,
) : TaskRepository {

    override fun observeTodaysTasks(): Flow<List<Task>> =
        dateTimeProvider.currentDay.flatMapLatest { day ->
            taskDao.observeTasksForDate(day).map { entities ->
                entities.map(TaskEntity::asDomain)
            }
        }

    override fun observeExpiredTasks(): Flow<List<Task>> =
        dateTimeProvider.currentDay.flatMapLatest { day ->
            taskDao.observeTasksBeforeDate(day).map { entities ->
                entities.map(TaskEntity::asDomain)
            }
        }

    override suspend fun addTask(title: String, expiryTime: LocalTime?, category: TaskCategory) {
        val id = UUID.randomUUID().toString()
        val trimmedTitle = title.trim()
        taskDao.upsert(
            TaskEntity(
                id = id,
                title = trimmedTitle,
                isCompleted = false,
                createdDate = dateTimeProvider.today(),
                createdAt = dateTimeProvider.now(),
                expiryTime = expiryTime,
                category = category.name,
            ),
        )
        // Schedule exact notification if this task has an expiry time today
        if (expiryTime != null) {
            expiryScheduler.schedule(id, trimmedTitle, expiryTime)
        }
    }

    override suspend fun updateTask(id: String, title: String, expiryTime: LocalTime?, category: TaskCategory) {
        val trimmedTitle = title.trim()
        taskDao.updateTask(
            id = id,
            title = trimmedTitle,
            expiryTime = expiryTime?.toSecondOfDay(),
            category = category.name,
        )
        // Always cancel any existing alarm, then re-schedule if there's still an expiry.
        expiryScheduler.cancel(id)
        if (expiryTime != null) {
            expiryScheduler.schedule(id, trimmedTitle, expiryTime)
        }
    }

    override suspend fun getTask(id: String): Task? =
        taskDao.getById(id)?.asDomain()

    override suspend fun setCompleted(id: String, completed: Boolean) {
        taskDao.updateCompletion(id, completed)
        // Cancel the expiry alarm once a task is completed — no longer relevant.
        if (completed) expiryScheduler.cancel(id)
    }

    override suspend fun deleteTask(id: String) {
        expiryScheduler.cancel(id)
        taskDao.deleteById(id)
    }
}
