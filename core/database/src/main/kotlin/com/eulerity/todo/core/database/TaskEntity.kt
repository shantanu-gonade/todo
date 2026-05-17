package com.eulerity.todo.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val isCompleted: Boolean,
    val createdDate: LocalDate,
    val createdAt: Instant,
    val expiryTime: LocalTime?,
    /** Stored as enum name (e.g. "WORK"). Default "NONE" for pre-migration rows. */
    val category: String = "NONE",
)
