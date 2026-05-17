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

package com.eulerity.todo.core.database

import androidx.room.TypeConverter
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class Converters {
    @TypeConverter
    fun dateToEpochDay(d: LocalDate?): Long? = d?.toEpochDays()?.toLong()

    @TypeConverter
    fun epochDayToDate(v: Long?): LocalDate? = v?.let { LocalDate.fromEpochDays(it.toInt()) }

    @TypeConverter
    fun instantToMillis(i: Instant?): Long? = i?.toEpochMilliseconds()

    @TypeConverter
    fun millisToInstant(v: Long?): Instant? = v?.let { Instant.fromEpochMilliseconds(it) }

    @TypeConverter
    fun timeToSeconds(t: LocalTime?): Int? = t?.toSecondOfDay()

    @TypeConverter
    fun secondsToTime(v: Int?): LocalTime? = v?.let { LocalTime.fromSecondOfDay(it) }
}
