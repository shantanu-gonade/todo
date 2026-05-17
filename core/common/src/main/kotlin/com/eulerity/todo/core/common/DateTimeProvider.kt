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

package com.eulerity.todo.core.common

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Single source of truth for "now" and "today". Injected everywhere time is
 * read so tests can substitute a deterministic fake.
 */
interface DateTimeProvider {
    fun now(): Instant
    fun today(): LocalDate
    /** Emits the current day, then re-emits whenever the local day changes. */
    val currentDay: Flow<LocalDate>
}
