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
