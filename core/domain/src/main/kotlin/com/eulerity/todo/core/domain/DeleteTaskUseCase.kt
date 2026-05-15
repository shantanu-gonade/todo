package com.eulerity.todo.core.domain

import com.eulerity.todo.core.data.TaskRepository
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(id: String) = repository.deleteTask(id)
}
