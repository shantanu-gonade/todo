package com.eulerity.todo.core.data.model

import com.eulerity.todo.core.database.TaskEntity
import com.eulerity.todo.core.model.Task

fun TaskEntity.asDomain() = Task(
    id = id,
    title = title,
    isCompleted = isCompleted,
    createdDate = createdDate,
    createdAt = createdAt,
    expiryTime = expiryTime,
)

fun Task.asEntity() = TaskEntity(
    id = id,
    title = title,
    isCompleted = isCompleted,
    createdDate = createdDate,
    createdAt = createdAt,
    expiryTime = expiryTime,
)
