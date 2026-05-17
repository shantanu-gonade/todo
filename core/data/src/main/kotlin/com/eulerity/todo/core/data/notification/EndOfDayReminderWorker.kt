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

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.eulerity.todo.core.common.DateTimeProvider
import com.eulerity.todo.core.database.TaskDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Fires once per day (scheduled by [ReminderScheduler]) and posts a
 * notification if any of today's tasks are still incomplete.
 *
 * Always returns [Result.success] so WorkManager does not retry or report
 * failure — a missed notification is not a critical error.
 */
@HiltWorker
class EndOfDayReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val taskDao: TaskDao,
    private val dateTimeProvider: DateTimeProvider,
) : CoroutineWorker(appContext, params) {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        val today = dateTimeProvider.today()
        val tasks = taskDao.observeTasksForDate(today).first()
        val incompleteCount = tasks.count { !it.isCompleted }

        if (incompleteCount > 0) {
            TodoNotificationChannel.ensure(applicationContext)
            postReminder(incompleteCount)
        }

        return Result.success()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun postReminder(count: Int) {
        val ctx = applicationContext
        val launchIntent = ctx.packageManager
            .getLaunchIntentForPackage(ctx.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }

        val pendingIntent = if (launchIntent != null) {
            PendingIntent.getActivity(
                ctx,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        } else null

        val body = if (count == 1) {
            "You still have 1 task left for today"
        } else {
            "You still have $count tasks left for today"
        }

        val notification = NotificationCompat.Builder(ctx, REMINDER_CHANNEL_ID)
            .setSmallIcon(com.eulerity.todo.core.data.R.drawable.ic_notification)
            .setContentTitle("Don't forget your todos!")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .apply { if (pendingIntent != null) setContentIntent(pendingIntent) }
            .build()

        NotificationManagerCompat.from(ctx).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
