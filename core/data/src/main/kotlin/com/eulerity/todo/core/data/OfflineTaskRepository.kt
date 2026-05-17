package com.eulerity.todo.core.data

import com.eulerity.todo.core.common.DateTimeProvider
import com.eulerity.todo.core.data.model.asDomain
import com.eulerity.todo.core.database.TaskDao
import com.eulerity.todo.core.database.TaskEntity
import com.eulerity.todo.core.model.Task
import com.eulerity.todo.core.model.TaskCategory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalTime
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineTaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val dateTimeProvider: DateTimeProvider,
) : TaskRepository {

    override fun observeTodaysTasks(): Flow<List<Task>> =
        dateTimeProvider.currentDay.flatMapLatest { day ->
            taskDao.observeTasksForDate(day).map { entities ->
                entities.map(TaskEntity::asDomain)
            }
        }

    override fun observeExpiredTasks(): Flow<List<Task>> =
        dateTimeProvider.currentDay.flatMapLatest { day ->
            taskDao.observeTasksBeforeDate(day).map { entities ->
                entities.map(TaskEntity::asDomain)
            }
        }

    override suspend fun addTask(title: String, expiryTime: LocalTime?, category: TaskCategory) {
        taskDao.upsert(
            TaskEntity(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                isCompleted = false,
                createdDate = dateTimeProvider.today(),
                createdAt = dateTimeProvider.now(),
                expiryTime = expiryTime,
                category = category.name,
            ),
        )
    }

    override suspend fun updateTask(id: String, title: String, expiryTime: LocalTime?, category: TaskCategory) {
        taskDao.updateTask(
            id = id,
            title = title.trim(),
            expiryTime = expiryTime?.toSecondOfDay(),
            category = category.name,
        )
    }

    override suspend fun getTask(id: String): Task? =
        taskDao.getById(id)?.asDomain()

    override suspend fun setCompleted(id: String, completed: Boolean) =
        taskDao.updateCompletion(id, completed)

    override suspend fun deleteTask(id: String) = taskDao.deleteById(id)
}
