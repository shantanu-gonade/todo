package com.eulerity.todo.core.data.notification

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.eulerity.todo.core.common.DateTimeProvider
import com.eulerity.todo.core.database.TaskDao
import com.eulerity.todo.core.database.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented smoke test: verifies the worker can be constructed and always
 * returns [ListenableWorker.Result.success], regardless of task count.
 *
 * Uses [TestListenableWorkerBuilder] (no Hilt needed here — we inject fakes
 * manually via the builder's factory override approach is unavailable without
 * Hilt test runner, so we build a minimal manual test that confirms compilation
 * and Result.success contract).
 */
@RunWith(AndroidJUnit4::class)
class EndOfDayReminderWorkerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @Test
    fun workerReturnsSuccess_whenNoTasks() = runBlocking {
        val worker = TestListenableWorkerBuilder<EndOfDayReminderWorker>(context)
            .setWorkerFactory(
                TestWorkerFactory(
                    taskDao = FakeTaskDaoForWorkerTest(emptyList()),
                    dateTimeProvider = FakeDateTimeProviderForWorkerTest(),
                ),
            )
            .build()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun workerReturnsSuccess_whenAllTasksComplete() = runBlocking {
        val today = FakeDateTimeProviderForWorkerTest().today()
        val tasks = listOf(
            makeTask(id = "1", isCompleted = true, date = today),
            makeTask(id = "2", isCompleted = true, date = today),
        )

        val worker = TestListenableWorkerBuilder<EndOfDayReminderWorker>(context)
            .setWorkerFactory(
                TestWorkerFactory(
                    taskDao = FakeTaskDaoForWorkerTest(tasks),
                    dateTimeProvider = FakeDateTimeProviderForWorkerTest(),
                ),
            )
            .build()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun workerReturnsSuccess_whenIncompleteTasksExist() = runBlocking {
        val today = FakeDateTimeProviderForWorkerTest().today()
        val tasks = listOf(
            makeTask(id = "1", isCompleted = false, date = today),
            makeTask(id = "2", isCompleted = true, date = today),
        )

        val worker = TestListenableWorkerBuilder<EndOfDayReminderWorker>(context)
            .setWorkerFactory(
                TestWorkerFactory(
                    taskDao = FakeTaskDaoForWorkerTest(tasks),
                    dateTimeProvider = FakeDateTimeProviderForWorkerTest(),
                ),
            )
            .build()

        val result = worker.doWork()

        // Worker must never fail — notification delivery is best-effort
        assertEquals(ListenableWorker.Result.success(), result)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun makeTask(id: String, isCompleted: Boolean, date: LocalDate) = TaskEntity(
        id = id,
        title = "Task $id",
        isCompleted = isCompleted,
        createdDate = date,
        createdAt = Clock.System.now(),
        expiryTime = null,
    )
}

// ── Fakes ──────────────────────────────────────────────────────────────────

private class FakeTaskDaoForWorkerTest(private val tasks: List<TaskEntity>) : TaskDao {
    override fun observeTasksForDate(day: LocalDate): Flow<List<TaskEntity>> = flowOf(tasks)
    override fun observeTasksBeforeDate(day: LocalDate): Flow<List<TaskEntity>> = flowOf(emptyList())
    override suspend fun upsert(task: TaskEntity) = Unit
    override suspend fun updateCompletion(id: String, completed: Boolean) = Unit
    override suspend fun deleteById(id: String) = Unit
}

private class FakeDateTimeProviderForWorkerTest : DateTimeProvider {
    private val fixedNow = Clock.System.now()
    override fun now(): Instant = fixedNow
    override fun today(): LocalDate = fixedNow.toLocalDateTime(TimeZone.UTC).date
    override val currentDay: Flow<LocalDate> = flowOf(today())
}

// ── TestWorkerFactory ──────────────────────────────────────────────────────

/**
 * Manual WorkerFactory that satisfies HiltWorker's assisted-inject constructor
 * by creating the worker directly without Hilt.
 */
private class TestWorkerFactory(
    private val taskDao: TaskDao,
    private val dateTimeProvider: DateTimeProvider,
) : androidx.work.WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: androidx.work.WorkerParameters,
    ): ListenableWorker? {
        return if (workerClassName == EndOfDayReminderWorker::class.java.name) {
            EndOfDayReminderWorker(
                appContext,
                workerParameters,
                taskDao,
                dateTimeProvider,
            )
        } else null
    }
}
