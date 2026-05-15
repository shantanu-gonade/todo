package com.eulerity.todo.core.domain

import com.eulerity.todo.core.data.TaskRepository
import javax.inject.Inject

class ToggleTaskCompletionUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(id: String, completed: Boolean) =
        repository.setCompleted(id, completed)
}
