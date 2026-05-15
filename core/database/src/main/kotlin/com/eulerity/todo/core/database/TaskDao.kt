package com.eulerity.todo.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE createdDate = :day ORDER BY createdAt ASC")
    fun observeTasksForDate(day: LocalDate): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE createdDate < :day ORDER BY createdDate DESC, createdAt ASC")
    fun observeTasksBeforeDate(day: LocalDate): Flow<List<TaskEntity>>

    @Upsert
    suspend fun upsert(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :completed WHERE id = :id")
    suspend fun updateCompletion(id: String, completed: Boolean)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String)
}
