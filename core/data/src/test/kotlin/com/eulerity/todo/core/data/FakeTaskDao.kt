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
}
