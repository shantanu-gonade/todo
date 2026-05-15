package com.eulerity.todo.core.domain

import com.eulerity.todo.core.data.TaskRepository
import com.eulerity.todo.core.model.Task
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveExpiredTasksUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    operator fun invoke(): Flow<List<Task>> = repository.observeExpiredTasks()
}
