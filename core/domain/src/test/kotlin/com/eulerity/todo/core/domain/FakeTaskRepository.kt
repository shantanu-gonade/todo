package com.eulerity.todo.core.domain

import com.eulerity.todo.core.data.TaskRepository
import com.eulerity.todo.core.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalTime

class FakeTaskRepository : TaskRepository {
    /** Tracks (title, expiryTime) pairs added via [addTask]. */
    val added = mutableListOf<Pair<String, LocalTime?>>()

    private val todaysTasks = MutableStateFlow<List<Task>>(emptyList())
    private val expiredTasks = MutableStateFlow<List<Task>>(emptyList())

    override fun observeTodaysTasks(): Flow<List<Task>> = todaysTasks
    override fun observeExpiredTasks(): Flow<List<Task>> = expiredTasks

    override suspend fun addTask(title: String, expiryTime: LocalTime?) {
        added.add(title to expiryTime)
    }

    override suspend fun setCompleted(id: String, completed: Boolean) {}
    override suspend fun deleteTask(id: String) {}
}
