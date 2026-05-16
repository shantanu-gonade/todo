package com.eulerity.todo.core.domain

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertTrue

class DeleteTaskUseCaseTest {

    private val repo = FakeTaskRepository()
    private val useCase = DeleteTaskUseCase(repo)

    @Test
    fun `invoke delegates delete to repository`() = runTest {
        useCase("task-99")
        assertTrue(repo.deleted.contains("task-99"))
    }

    @Test
    fun `invoke removes the correct task id`() = runTest {
        useCase("abc-123")
        assertTrue(repo.deleted.contains("abc-123"))
        assertTrue(!repo.deleted.contains("other-id"))
    }
}
