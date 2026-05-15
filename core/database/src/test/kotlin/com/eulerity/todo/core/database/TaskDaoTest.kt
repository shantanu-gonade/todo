package com.eulerity.todo.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class TaskDaoTest {
    private lateinit var db: TodoDatabase
    private lateinit var dao: TaskDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TodoDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.taskDao()
    }

    @After
    fun teardown() = db.close()

    @Test
    fun `observeTasksForDate returns only that day's tasks`() = runTest {
        val today = LocalDate(2026, 5, 14)
        dao.upsert(task("a", today))
        dao.upsert(task("b", LocalDate(2026, 5, 13)))
        dao.observeTasksForDate(today).test {
            val rows = awaitItem()
            assertEquals(1, rows.size)
            assertEquals("a", rows.first().id)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `observeTasksBeforeDate returns only prior days`() = runTest {
        val today = LocalDate(2026, 5, 14)
        dao.upsert(task("a", today))
        dao.upsert(task("b", LocalDate(2026, 5, 13)))
        dao.observeTasksBeforeDate(today).test {
            val rows = awaitItem()
            assertEquals(1, rows.size)
            assertEquals("b", rows.first().id)
            cancelAndConsumeRemainingEvents()
        }
    }

    private fun task(id: String, date: LocalDate) = TaskEntity(
        id = id,
        title = "t-$id",
        isCompleted = false,
        createdDate = date,
        createdAt = Instant.fromEpochMilliseconds(0),
        expiryTime = null,
    )
}
