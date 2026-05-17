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

package com.eulerity.todo.core.data.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules (or cancels) the end-of-day reminder worker.
 *
 * Call [schedule] on each app launch so the reminder always fires at ~21:00
 * local time. If 21:00 has already passed today the delay targets tomorrow's
 * 21:00. Uses [ExistingWorkPolicy.REPLACE] so repeated scheduling is safe.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun schedule() {
        val delayMs = computeDelayMs()
        val request = OneTimeWorkRequestBuilder<EndOfDayReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /**
     * Returns milliseconds until the next 21:00 in the device's local timezone.
     * Exposed as `internal` so it can be unit-tested without a device.
     * Minimum 1 minute to guard against immediate-fire edge cases.
     */
    internal fun computeDelayMs(
        nowMs: Long = Clock.System.now().toEpochMilliseconds(),
    ): Long {
        val tz = TimeZone.currentSystemDefault()
        val now = Instant.fromEpochMilliseconds(nowMs)
        val localNow = now.toLocalDateTime(tz)

        val todayAt21 = LocalDateTime(
            year = localNow.year,
            monthNumber = localNow.monthNumber,
            dayOfMonth = localNow.dayOfMonth,
            hour = REMINDER_HOUR,
            minute = 0,
            second = 0,
            nanosecond = 0,
        ).toInstant(tz).toEpochMilliseconds()

        val rawDelay = if (todayAt21 > nowMs) {
            todayAt21 - nowMs
        } else {
            // Already past 21:00 — aim for tomorrow's 21:00
            todayAt21 - nowMs + MILLIS_PER_DAY
        }

        return maxOf(rawDelay, MIN_DELAY_MS)
    }

    companion object {
        private const val WORK_NAME = "todo_end_of_day_reminder"
        private const val REMINDER_HOUR = 21
        private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1_000
        private const val MIN_DELAY_MS = 60_000L // 1-minute floor
    }
}
