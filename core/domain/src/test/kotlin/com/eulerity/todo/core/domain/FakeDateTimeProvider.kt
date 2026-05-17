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

import com.eulerity.todo.core.common.DateTimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

/** Minimal test double for [DateTimeProvider] — fixed clock at midnight on [date]. */
class FakeDateTimeProvider(private val date: LocalDate) : DateTimeProvider {
    private val zone = TimeZone.currentSystemDefault()

    override fun now(): Instant = date.atStartOfDayIn(zone)
    override fun today(): LocalDate = date
    override val currentDay: Flow<LocalDate> = MutableStateFlow(date)
}
