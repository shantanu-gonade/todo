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

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.eulerity.todo.core.common.DateTimeProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abstraction for scheduling and cancelling per-task expiry alarms.
 * Exists as an interface so tests can substitute a no-op fake without
 * needing a real [android.app.AlarmManager] or [android.content.Context].
 */
interface TaskExpiryScheduler {
    fun schedule(taskId: String, taskTitle: String, expiryTime: LocalTime)
    fun cancel(taskId: String)
}

/**
 * AlarmManager-backed implementation of [TaskExpiryScheduler].
 *
 * Uses [AlarmManager.setExactAndAllowWhileIdle] for accurate delivery even in Doze mode.
 * On API 31+ we check [AlarmManager.canScheduleExactAlarms]; if the permission is missing
 * the alarm is skipped silently — the task is still usable without the notification.
 * Alarm request codes are derived from [taskId.hashCode()] for stable cancellation.
 */
@Singleton
class AlarmManagerTaskExpiryScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dateTimeProvider: DateTimeProvider,
) : TaskExpiryScheduler {

    private val alarmManager: AlarmManager?
        get() = context.getSystemService()

    override fun schedule(taskId: String, taskTitle: String, expiryTime: LocalTime) {
        val am = alarmManager ?: return
        if (!canScheduleExactAlarms(am)) return

        val triggerMs = computeTriggerMs(expiryTime)
        if (triggerMs != null) {
            val pendingIntent = buildPendingIntent(taskId, taskTitle)
            if (pendingIntent != null) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
            }
        }
    }

    private fun canScheduleExactAlarms(am: AlarmManager): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()

    private fun computeTriggerMs(expiryTime: LocalTime): Long? {
        val tz = TimeZone.currentSystemDefault()
        val triggerMs = LocalDateTime(
            date = dateTimeProvider.today(),
            time = expiryTime,
        ).toInstant(tz).toEpochMilliseconds()
        // Don't schedule if already in the past
        return triggerMs.takeIf { it > dateTimeProvider.now().toEpochMilliseconds() }
    }

    override fun cancel(taskId: String) {
        val am = alarmManager ?: return
        val pendingIntent = buildPendingIntent(taskId, "") ?: return
        am.cancel(pendingIntent)
    }

    private fun buildPendingIntent(taskId: String, taskTitle: String): PendingIntent? {
        val intent = Intent(context, TaskExpiryReceiver::class.java).apply {
            action = TaskExpiryReceiver.ACTION_TASK_EXPIRED
            putExtra(TaskExpiryReceiver.EXTRA_TASK_ID, taskId)
            putExtra(TaskExpiryReceiver.EXTRA_TASK_TITLE, taskTitle)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
