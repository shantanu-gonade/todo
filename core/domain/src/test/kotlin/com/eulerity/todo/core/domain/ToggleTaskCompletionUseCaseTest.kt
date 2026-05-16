package com.eulerity.todo.core.domain

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertTrue

class ToggleTaskCompletionUseCaseTest {

    private val repo = FakeTaskRepository()
    private val useCase = ToggleTaskCompletionUseCase(repo)

    @Test
    fun `invoke marks task as completed`() = runTest {
        useCase("task-1", true)
        assertTrue(repo.toggled.any { it.first == "task-1" && it.second })
    }

    @Test
    fun `invoke marks task as incomplete`() = runTest {
        useCase("task-2", false)
        assertTrue(repo.toggled.any { it.first == "task-2" && !it.second })
    }

    @Test
    fun `invoke delegates to repository setCompleted`() = runTest {
        useCase("task-3", true)
        assertTrue(repo.toggled.isNotEmpty())
    }
}
