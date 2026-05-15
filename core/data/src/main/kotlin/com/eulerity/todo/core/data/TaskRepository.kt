package com.eulerity.todo.core.data

import com.eulerity.todo.core.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalTime

interface TaskRepository {
    /** Tasks for the current day. Re-evaluates automatically when the day rolls over. */
    fun observeTodaysTasks(): Flow<List<Task>>

    /** Tasks from days before today — the history view. */
    fun observeExpiredTasks(): Flow<List<Task>>

    suspend fun addTask(title: String, expiryTime: LocalTime?)

    suspend fun setCompleted(id: String, completed: Boolean)

    suspend fun deleteTask(id: String)
}
