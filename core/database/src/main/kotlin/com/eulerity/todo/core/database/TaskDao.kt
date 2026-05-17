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

package com.eulerity.todo.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE createdDate = :day ORDER BY createdAt ASC")
    fun observeTasksForDate(day: LocalDate): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE createdDate < :day ORDER BY createdDate DESC, createdAt ASC")
    fun observeTasksBeforeDate(day: LocalDate): Flow<List<TaskEntity>>

    @Upsert
    suspend fun upsert(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :completed WHERE id = :id")
    suspend fun updateCompletion(id: String, completed: Boolean)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TaskEntity?

    @Query("UPDATE tasks SET title = :title, expiryTime = :expiryTime, category = :category WHERE id = :id")
    suspend fun updateTask(id: String, title: String, expiryTime: Int?, category: String)
}
