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
