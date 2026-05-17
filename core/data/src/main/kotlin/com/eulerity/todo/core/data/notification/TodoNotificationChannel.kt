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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService

const val REMINDER_CHANNEL_ID = "todo_daily_reminder"
const val EXPIRY_CHANNEL_ID = "todo_task_expiry"

/**
 * Creates notification channels for the app.
 * Safe to call on any API level and multiple times — the OS is idempotent for
 * channel creation.
 */
object TodoNotificationChannel {

    /** Ensures the end-of-day daily reminder channel exists. */
    fun ensure(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        if (manager.getNotificationChannel(REMINDER_CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            "Daily reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "End-of-day reminder when you still have tasks left for today"
        }
        manager.createNotificationChannel(channel)
    }

    /** Ensures the per-task expiry alert channel exists. */
    fun ensureExpiry(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        if (manager.getNotificationChannel(EXPIRY_CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            EXPIRY_CHANNEL_ID,
            "Task expiry alerts",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notifies you when a specific task has reached its expiry time"
        }
        manager.createNotificationChannel(channel)
    }
}
