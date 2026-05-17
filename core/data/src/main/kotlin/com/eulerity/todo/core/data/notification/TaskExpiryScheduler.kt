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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) return

        val tz = TimeZone.currentSystemDefault()
        val today = dateTimeProvider.today()
        val triggerMs = LocalDateTime(
            date = today,
            time = expiryTime,
        ).toInstant(tz).toEpochMilliseconds()

        // Don't schedule if already in the past
        if (triggerMs <= dateTimeProvider.now().toEpochMilliseconds()) return

        val pendingIntent = buildPendingIntent(taskId, taskTitle) ?: return
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
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
