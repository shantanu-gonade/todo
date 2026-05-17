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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.eulerity.todo.core.data.R

/**
 * Receives the AlarmManager broadcast when a task's expiry time arrives.
 * Posts a notification reminding the user that a specific task has expired.
 *
 * Extras used:
 *   [EXTRA_TASK_ID]    — String: used as notification tag for per-task deduplication.
 *   [EXTRA_TASK_TITLE] — String: shown in the notification body.
 */
class TaskExpiryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TASK_EXPIRED) return

        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "A task"

        TodoNotificationChannel.ensureExpiry(context)

        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }

        val pendingIntent = if (launchIntent != null) {
            PendingIntent.getActivity(
                context,
                taskId.hashCode(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        } else null

        val notification = NotificationCompat.Builder(context, EXPIRY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Task expired")
            .setContentText("\"$taskTitle\" has expired")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .apply { if (pendingIntent != null) setContentIntent(pendingIntent) }
            .build()

        // Use taskId.hashCode() as the notification ID so each task gets its own
        // notification slot (avoids replacing previous ones with the same int ID 1001).
        if (androidx.core.app.NotificationManagerCompat.from(context)
                .areNotificationsEnabled()
        ) {
            @Suppress("MissingPermission") // checked via areNotificationsEnabled()
            NotificationManagerCompat.from(context)
                .notify(taskId.hashCode(), notification)
        }
    }

    companion object {
        const val ACTION_TASK_EXPIRED = "com.eulerity.todo.ACTION_TASK_EXPIRED"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TITLE = "task_title"
    }
}
