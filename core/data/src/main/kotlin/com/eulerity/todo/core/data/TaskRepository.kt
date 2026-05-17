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
