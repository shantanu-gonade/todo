package com.eulerity.todo.core.data.notification

import android.Manifest
//noinspection SuspiciousImport
import android.R
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
            .setSmallIcon(R.drawable.ic_popup_reminder)
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
