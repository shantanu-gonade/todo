package com.eulerity.todo.core.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** A todo item that belongs to exactly one calendar day. */
data class Task(
    val id: String,
    val title: String,
    val isCompleted: Boolean,
    val createdDate: LocalDate,
    val createdAt: Instant,
    val expiryTime: LocalTime?,
)
