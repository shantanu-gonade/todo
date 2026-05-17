package com.eulerity.todo.core.data.model

import com.eulerity.todo.core.database.TaskEntity
import com.eulerity.todo.core.model.Task
import com.eulerity.todo.core.model.TaskCategory

fun TaskEntity.asDomain() = Task(
    id = id,
    title = title,
    isCompleted = isCompleted,
    createdDate = createdDate,
    createdAt = createdAt,
    expiryTime = expiryTime,
    category = runCatching { TaskCategory.valueOf(category) }.getOrDefault(TaskCategory.NONE),
)

fun Task.asEntity() = TaskEntity(
    id = id,
    title = title,
    isCompleted = isCompleted,
    createdDate = createdDate,
    createdAt = createdAt,
    expiryTime = expiryTime,
    category = category.name,
)
