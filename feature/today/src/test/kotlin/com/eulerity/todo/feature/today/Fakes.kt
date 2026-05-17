package com.eulerity.todo.feature.today

import com.eulerity.todo.core.common.DateTimeProvider
import com.eulerity.todo.core.data.TaskRepository
import com.eulerity.todo.core.domain.AddTaskUseCase
import com.eulerity.todo.core.domain.DeleteTaskUseCase
import com.eulerity.todo.core.domain.UpdateTaskUseCase
import com.eulerity.todo.core.domain.ObserveTodaysTasksUseCase
import com.eulerity.todo.core.domain.ToggleTaskCompletionUseCase
import com.eulerity.todo.core.model.Task
import com.eulerity.todo.core.model.TaskCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

// ---------------------------------------------------------------------------
// FakeDateTimeProvider
// ---------------------------------------------------------------------------

private val TEST_DATE = LocalDate(2026, 5, 15)

class FakeDateTimeProvider(
    private val date: LocalDate = TEST_DATE,
) : DateTimeProvider {
    private val zone = TimeZone.currentSystemDefault()
    override fun now(): Instant = date.atStartOfDayIn(zone)
    override fun today(): LocalDate = date
    override val currentDay: Flow<LocalDate> = MutableStateFlow(date)
}

// ---------------------------------------------------------------------------
// FakeTaskRepository — backs all fake use-cases below
// ---------------------------------------------------------------------------

private fun fakeTask(id: String) = Task(
    id = id,
    title = "Task $id",
    isCompleted = false,
    createdDate = TEST_DATE,
    createdAt = Instant.fromEpochSeconds(0),
    expiryTime = null,
)

/** Minimal in-memory [TaskRepository] for unit tests. */
class FakeTaskRepository(tasks: List<Task> = emptyList()) : TaskRepository {
    val tasksFlow = MutableStateFlow(tasks)
    val added = mutableListOf<Pair<String, LocalTime?>>()
    val toggled = mutableListOf<Pair<String, Boolean>>()
    val deleted = mutableListOf<String>()

    override fun observeTodaysTasks(): Flow<List<Task>> = tasksFlow
    override fun observeExpiredTasks(): Flow<List<Task>> = MutableStateFlow(emptyList())
    override suspend fun addTask(title: String, expiryTime: LocalTime?, category: TaskCategory) {
        added += title to expiryTime
    }
    override suspend fun updateTask(id: String, title: String, expiryTime: LocalTime?, category: TaskCategory) {}
    override suspend fun getTask(id: String): Task? = tasksFlow.value.find { it.id == id }
    override suspend fun setCompleted(id: String, completed: Boolean) {
        toggled += id to completed
    }
    override suspend fun deleteTask(id: String) {
        deleted += id
    }
}

// ---------------------------------------------------------------------------
// Convenience factory functions — match the signatures expected by the tests.
// Each function wires a real use-case to a FakeTaskRepository so no subclassing
// is needed (all use-case classes are final).
// ---------------------------------------------------------------------------

/**
 * Returns an [ObserveTodaysTasksUseCase] backed by [initialCount] fake tasks.
 * If [firstId] is provided the first task gets that specific id.
 */
fun FakeObserveTodaysTasks(
    initialCount: Int = 0,
    firstId: String = "1",
): ObserveTodaysTasksUseCase {
    val tasks = when {
        initialCount == 0 -> emptyList()
        else -> listOf(fakeTask(firstId)) + (2..initialCount).map { fakeTask(it.toString()) }
    }
    return ObserveTodaysTasksUseCase(FakeTaskRepository(tasks))
}

/**
 * Returns a [FakeTaskRepository] pre-populated with [initialCount] fake tasks,
 * plus an [ObserveTodaysTasksUseCase] backed by the same repo.
 * Use when a test needs multiple use-cases to share state.
 */
fun FakeRepoWithTasks(
    initialCount: Int = 0,
    firstId: String = "1",
): Pair<FakeTaskRepository, ObserveTodaysTasksUseCase> {
    val tasks = when {
        initialCount == 0 -> emptyList()
        else -> listOf(fakeTask(firstId)) + (2..initialCount).map { fakeTask(it.toString()) }
    }
    val repo = FakeTaskRepository(tasks)
    return repo to ObserveTodaysTasksUseCase(repo)
}

/**
 * Returns an [AddTaskUseCase] backed by a fake repository.
 * The [shouldFail] flag causes the real use-case to receive a null title that
 * triggers its own blank-title guard — use [FakeAddTask] without that flag for
 * happy-path tests.
 *
 * For blank-title validation the test just passes "" — the real use-case logic
 * handles it correctly without any special fake behaviour.
 */
fun FakeAddTask(
    dateTimeProvider: DateTimeProvider = FakeDateTimeProvider(),
): AddTaskUseCase = AddTaskUseCase(FakeTaskRepository(), dateTimeProvider)

fun FakeToggleCompletion(): ToggleTaskCompletionUseCase =
    ToggleTaskCompletionUseCase(FakeTaskRepository())

fun FakeDeleteTask(): DeleteTaskUseCase =
    DeleteTaskUseCase(FakeTaskRepository())

fun FakeUpdateTask(
    dateTimeProvider: DateTimeProvider = FakeDateTimeProvider(),
    repo: TaskRepository = FakeTaskRepository(),
): UpdateTaskUseCase = UpdateTaskUseCase(repo, dateTimeProvider)
