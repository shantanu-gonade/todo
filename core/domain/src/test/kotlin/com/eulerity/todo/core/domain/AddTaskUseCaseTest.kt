package com.eulerity.todo.core.domain

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.assertTrue

class AddTaskUseCaseTest {

    private val provider = FakeDateTimeProvider(LocalDate(2026, 5, 14))

    @Test
    fun `blank title is rejected`() = runTest {
        val useCase = AddTaskUseCase(FakeTaskRepository(), provider)
        assertTrue(useCase(title = "   ", expiryTime = null).isFailure)
    }

    @Test
    fun `valid title is forwarded to the repository`() = runTest {
        val repo = FakeTaskRepository()
        val useCase = AddTaskUseCase(repo, provider)
        val result = useCase(title = "buy milk", expiryTime = null)
        assertTrue(result.isSuccess)
        assertTrue(repo.added.any { it.first == "buy milk" })
    }

    @Test
    fun `empty-after-trim title is rejected`() = runTest {
        val useCase = AddTaskUseCase(FakeTaskRepository(), provider)
        assertTrue(useCase(title = "  \t  ", expiryTime = null).isFailure)
    }
}
