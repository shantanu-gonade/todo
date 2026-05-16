package com.eulerity.todo.core.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService

const val REMINDER_CHANNEL_ID = "todo_daily_reminder"

/**
 * Creates the "Daily reminders" notification channel the first time it is called.
 * Safe to call on any API level and multiple times — the OS is idempotent for
 * channel creation.
 */
object TodoNotificationChannel {

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
}
