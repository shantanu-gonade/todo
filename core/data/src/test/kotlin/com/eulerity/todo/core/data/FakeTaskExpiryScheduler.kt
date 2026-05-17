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
