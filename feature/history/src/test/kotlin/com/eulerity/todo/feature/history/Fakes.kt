package com.eulerity.todo.feature.history

import com.eulerity.todo.core.data.TaskRepository
import com.eulerity.todo.core.domain.ObserveExpiredTasksUseCase
import com.eulerity.todo.core.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

// ---------------------------------------------------------------------------
// Shared test fixtures
// ---------------------------------------------------------------------------

private val YESTERDAY = LocalDate(2026, 5, 14)

private fun expiredTask(id: String) = Task(
    id = id,
    title = "Expired Task $id",
    isCompleted = false,
    createdDate = YESTERDAY,
    createdAt = Instant.fromEpochSeconds(0),
    expiryTime = LocalTime(9, 0),
)

// ---------------------------------------------------------------------------
// FakeObserveExpiredTasks — hand-written fake; no mocking libraries
// ---------------------------------------------------------------------------

/**
 * Minimal [TaskRepository] fake that only supplies the expired-tasks flow.
 * All other operations are unused and throw [UnsupportedOperationException].
 */
class FakeExpiredTaskRepository(tasks: List<Task> = emptyList()) : TaskRepository {
    val tasksFlow = MutableStateFlow(tasks)

    override fun observeExpiredTasks(): Flow<List<Task>> = tasksFlow
    override fun observeTodaysTasks(): Flow<List<Task>> = MutableStateFlow(emptyList())
    override suspend fun addTask(title: String, expiryTime: LocalTime?) = Unit
    override suspend fun setCompleted(id: String, completed: Boolean) = Unit
    override suspend fun deleteTask(id: String) = Unit
}

/**
 * Returns an [ObserveExpiredTasksUseCase] backed by [count] fake expired tasks.
 */
fun FakeObserveExpiredTasks(count: Int = 0): ObserveExpiredTasksUseCase {
    val tasks = (1..count).map { expiredTask(it.toString()) }
    return ObserveExpiredTasksUseCase(FakeExpiredTaskRepository(tasks))
}

/**
 * Returns an [ObserveExpiredTasksUseCase] backed by a controllable repository.
 * Use [repo] to push new emissions in tests.
 */
fun FakeObserveExpiredTasksWithRepo(
    tasks: List<Task> = emptyList(),
): Pair<ObserveExpiredTasksUseCase, FakeExpiredTaskRepository> {
    val repo = FakeExpiredTaskRepository(tasks)
    return ObserveExpiredTasksUseCase(repo) to repo
}
