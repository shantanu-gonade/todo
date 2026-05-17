package com.eulerity.todo.core.data

import com.eulerity.todo.core.data.notification.TaskExpiryScheduler
import kotlinx.datetime.LocalTime

/**
 * No-op [TaskExpiryScheduler] for unit tests. Records all calls so
 * tests can assert on scheduling/cancellation behaviour if needed.
 */
class FakeTaskExpiryScheduler : TaskExpiryScheduler {
    val scheduled = mutableListOf<Pair<String, LocalTime>>()
    val cancelled = mutableListOf<String>()

    override fun schedule(taskId: String, taskTitle: String, expiryTime: LocalTime) {
        scheduled += taskId to expiryTime
    }

    override fun cancel(taskId: String) {
        cancelled += taskId
    }
}
