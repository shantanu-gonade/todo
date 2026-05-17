/*
 * Copyright 2026 Eulerity, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
