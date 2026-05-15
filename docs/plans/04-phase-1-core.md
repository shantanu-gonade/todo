# Phase 1 — Core Modules

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans.
> Prerequisite: Phase 0 verification gate passed.

**Produces:** the data and domain layers. The most test-heavy phase; it proves
the today-only constraint works.

**Package root:** `com.eulerity.todo`. Gradle commands run from `Eulerity/todo/`.

---

## Task 1.1: Domain model — `:core:model`

**Files:**
- Create: `core/model/src/main/kotlin/com/eulerity/todo/core/model/Task.kt`
- Create: `core/model/src/main/kotlin/com/eulerity/todo/core/model/UserData.kt`
- Delete: this module's `Placeholder.kt`

**Step 1: Write `Task.kt`**

```kotlin
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
```

**Step 2: Write `UserData.kt`**

```kotlin
package com.eulerity.todo.core.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserData(val themeMode: ThemeMode = ThemeMode.SYSTEM)
```

**Step 3: Verify it compiles**

Run: `./gradlew :core:model:compileKotlin`
Expected: `BUILD SUCCESSFUL`.

**Step 4: Commit**

```bash
git add core/model
git commit -m "feat(model): add Task and ThemeMode domain models"
```

## Task 1.2: Time abstraction — `:core:common`

This is the lever for the entire day-only constraint. Build it test-first.

**Files:**
- Create: `core/common/src/main/kotlin/com/eulerity/todo/core/common/DateTimeProvider.kt`
- Create: `core/common/src/main/kotlin/com/eulerity/todo/core/common/DefaultDateTimeProvider.kt`
- Create: `core/common/src/main/kotlin/com/eulerity/todo/core/common/Dispatchers.kt`
- Create: `core/common/src/main/kotlin/com/eulerity/todo/core/common/di/CommonModule.kt`
- Test: `core/common/src/test/kotlin/com/eulerity/todo/core/common/DefaultDateTimeProviderTest.kt`
- Test helper: `core/common/src/test/kotlin/com/eulerity/todo/core/common/FakeClock.kt`

**Step 1: Define `DateTimeProvider.kt`**

```kotlin
package com.eulerity.todo.core.common

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Single source of truth for "now" and "today". Injected everywhere time is
 * read so tests can substitute a deterministic fake.
 */
interface DateTimeProvider {
    fun now(): Instant
    fun today(): LocalDate
    /** Emits the current day, then re-emits whenever the local day changes. */
    val currentDay: Flow<LocalDate>
}
```

**Step 2: Write `FakeClock.kt`** (test source set)

```kotlin
package com.eulerity.todo.core.common

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

class FakeClock(private var day: LocalDate) : Clock {
    fun advanceToNextDay() { day = LocalDate.fromEpochDays(day.toEpochDays() + 1) }
    override fun now(): Instant = day.atStartOfDayIn(TimeZone.currentSystemDefault())
}
```

**Step 3: Write the failing test `DefaultDateTimeProviderTest.kt`**

```kotlin
package com.eulerity.todo.core.common

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.assertEquals

class DefaultDateTimeProviderTest {

    @Test
    fun `currentDay emits today's date on collection`() = runTest {
        val provider = DefaultDateTimeProvider(clock = FakeClock(LocalDate(2026, 5, 14)))
        provider.currentDay.test {
            assertEquals(LocalDate(2026, 5, 14), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `today reflects the injected clock`() {
        val provider = DefaultDateTimeProvider(clock = FakeClock(LocalDate(2026, 1, 1)))
        assertEquals(LocalDate(2026, 1, 1), provider.today())
    }
}
```

**Step 4: Run to verify it fails**

Run: `./gradlew :core:common:testDebugUnitTest --tests "*DefaultDateTimeProviderTest*"`
Expected: FAIL — `DefaultDateTimeProvider` does not exist.

**Step 5: Implement `DefaultDateTimeProvider.kt`**

```kotlin
package com.eulerity.todo.core.common

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

class DefaultDateTimeProvider @Inject constructor(
    private val clock: Clock = Clock.System,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) : DateTimeProvider {

    override fun now(): Instant = clock.now()

    override fun today(): LocalDate = clock.now().toLocalDateTime(zone).date

    override val currentDay: Flow<LocalDate> = callbackFlow {
        trySend(today())
        // The midnight re-emit and broadcast-receiver path are wired in Task 1.3
        // via an injected DateChangeBroadcaster, keeping this seam unit-testable.
        awaitClose { }
    }
}
```

**Step 6: Run to verify it passes**

Run: `./gradlew :core:common:testDebugUnitTest --tests "*DefaultDateTimeProviderTest*"`
Expected: PASS.

**Step 7: Add `Dispatchers.kt` + `di/CommonModule.kt`**

`Dispatchers.kt`:
```kotlin
package com.eulerity.todo.core.common

import javax.inject.Qualifier

@Qualifier @Retention(AnnotationRetention.RUNTIME)
annotation class IoDispatcher

@Qualifier @Retention(AnnotationRetention.RUNTIME)
annotation class DefaultDispatcher
```

`di/CommonModule.kt`:
```kotlin
package com.eulerity.todo.core.common.di

import com.eulerity.todo.core.common.DateTimeProvider
import com.eulerity.todo.core.common.DefaultDateTimeProvider
import com.eulerity.todo.core.common.DefaultDispatcher
import com.eulerity.todo.core.common.IoDispatcher
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CommonBindModule {
    @Binds @Singleton
    abstract fun bindDateTimeProvider(impl: DefaultDateTimeProvider): DateTimeProvider
}

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {
    @Provides @IoDispatcher
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides @DefaultDispatcher
    fun defaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
```

**Step 8: Commit**

```bash
git add core/common
git commit -m "feat(common): add testable DateTimeProvider and dispatchers"
```

## Task 1.3: Day-change broadcaster — `:core:common`

**Files:**
- Create: `core/common/src/main/kotlin/com/eulerity/todo/core/common/DateChangeBroadcaster.kt`
- Modify: `DefaultDateTimeProvider.kt` to consume it

**Step 1: Implement `DateChangeBroadcaster.kt`**

```kotlin
package com.eulerity.todo.core.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/** Emits Unit whenever the system date, time, or timezone changes. */
class DateChangeBroadcaster @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val changes: Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { trySend(Unit) }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        awaitClose { context.unregisterReceiver(receiver) }
    }
}
```

**Step 2: Wire it into `DefaultDateTimeProvider.currentDay`**

Inject `DateChangeBroadcaster`. Build `currentDay` as a `merge` of: an emission
of `today()` on collection, a ticker Flow that delays until the next computed
local midnight then re-emits and repeats, and `dateChangeBroadcaster.changes`
mapped to `today()`. Apply `distinctUntilChanged()`. Keep the no-arg constructor
default for the unit test by giving the broadcaster a nullable default of `null`
and skipping the broadcast branch when it is null.

**Step 3: Verify the module still compiles and tests pass**

Run: `./gradlew :core:common:testDebugUnitTest`
Expected: PASS — the unit test only checks the first emission, unchanged.

**Step 4: Commit**

```bash
git add core/common
git commit -m "feat(common): re-emit current day on midnight and clock changes"
```

## Task 1.4: Room persistence — `:core:database`

**Files:** under `core/database/src/main/kotlin/com/eulerity/todo/core/database/`
— `TaskEntity.kt`, `TaskDao.kt`, `TodoDatabase.kt`, `Converters.kt`,
`di/DatabaseModule.kt`. Test: `TaskDaoTest.kt`.

**Step 1: Write `Converters.kt`**

```kotlin
package com.eulerity.todo.core.database

import androidx.room.TypeConverter
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class Converters {
    @TypeConverter fun dateToEpochDay(d: LocalDate?): Long? = d?.toEpochDays()?.toLong()
    @TypeConverter fun epochDayToDate(v: Long?): LocalDate? = v?.let { LocalDate.fromEpochDays(it.toInt()) }
    @TypeConverter fun instantToMillis(i: Instant?): Long? = i?.toEpochMilliseconds()
    @TypeConverter fun millisToInstant(v: Long?): Instant? = v?.let { Instant.fromEpochMilliseconds(it) }
    @TypeConverter fun timeToSeconds(t: LocalTime?): Int? = t?.toSecondOfDay()
    @TypeConverter fun secondsToTime(v: Int?): LocalTime? = v?.let { LocalTime.fromSecondOfDay(it) }
}
```

**Step 2: Write `TaskEntity.kt`**

```kotlin
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
)
```

**Step 3: Write the failing DAO test `TaskDaoTest.kt`**

```kotlin
package com.eulerity.todo.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class TaskDaoTest {
    private lateinit var db: TodoDatabase
    private lateinit var dao: TaskDao

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), TodoDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.taskDao()
    }
    @After fun teardown() = db.close()

    @Test fun `observeTasksForDate returns only that day's tasks`() = runTest {
        val today = LocalDate(2026, 5, 14)
        dao.upsert(task("a", today))
        dao.upsert(task("b", LocalDate(2026, 5, 13)))
        dao.observeTasksForDate(today).test {
            val rows = awaitItem()
            assertEquals(1, rows.size)
            assertEquals("a", rows.first().id)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test fun `observeTasksBeforeDate returns only prior days`() = runTest {
        val today = LocalDate(2026, 5, 14)
        dao.upsert(task("a", today))
        dao.upsert(task("b", LocalDate(2026, 5, 13)))
        dao.observeTasksBeforeDate(today).test {
            val rows = awaitItem()
            assertEquals(1, rows.size)
            assertEquals("b", rows.first().id)
            cancelAndConsumeRemainingEvents()
        }
    }

    private fun task(id: String, date: LocalDate) = TaskEntity(
        id = id, title = "t-$id", isCompleted = false,
        createdDate = date, createdAt = Instant.fromEpochMilliseconds(0), expiryTime = null,
    )
}
```

**Step 4: Run to verify it fails**

Run: `./gradlew :core:database:testDebugUnitTest --tests "*TaskDaoTest*"`
Expected: FAIL — `TaskDao` and `TodoDatabase` do not exist.

**Step 5: Write `TaskDao.kt`**

```kotlin
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
```

**Step 6: Write `TodoDatabase.kt`**

```kotlin
package com.eulerity.todo.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [TaskEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}
```

**Step 7: Run to verify it passes**

Run: `./gradlew :core:database:testDebugUnitTest --tests "*TaskDaoTest*"`
Expected: PASS.

**Step 8: Write `di/DatabaseModule.kt`**

```kotlin
package com.eulerity.todo.core.database.di

import android.content.Context
import androidx.room.Room
import com.eulerity.todo.core.database.TaskDao
import com.eulerity.todo.core.database.TodoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TodoDatabase =
        Room.databaseBuilder(context, TodoDatabase::class.java, "todo.db").build()

    @Provides
    fun provideTaskDao(db: TodoDatabase): TaskDao = db.taskDao()
}
```

**Step 9: Commit**

```bash
git add core/database
git commit -m "feat(database): add Room entity, DAO, and date-scoped queries"
```

## Task 1.5: Preferences — `:core:datastore`

**Files:** under `core/datastore/src/main/kotlin/com/eulerity/todo/core/datastore/`
— `UserPreferencesDataSource.kt`, `di/DataStoreModule.kt`. Test:
`UserPreferencesDataSourceTest.kt`.

**Step 1: Write the failing test** — write a `ThemeMode`, assert the
`Flow<UserData>` re-emits it. Use a temp-folder-backed
`PreferenceDataStoreFactory` per the DataStore testing guide.

**Step 2: Implement `UserPreferencesDataSource`** — wraps a
`DataStore<Preferences>`, exposes `userData: Flow<UserData>` mapping a string
preference key to `ThemeMode`, and `suspend fun setThemeMode(mode: ThemeMode)`.

**Step 3: Run the test green, add `di/DataStoreModule.kt`, commit**

```bash
git add core/datastore
git commit -m "feat(datastore): persist theme mode via DataStore Preferences"
```

## Task 1.6: Repository — `:core:data`

Where the day-only constraint becomes a composed Flow.

**Files:** under `core/data/src/main/kotlin/com/eulerity/todo/core/data/` —
`model/TaskMappers.kt`, `TaskRepository.kt`, `OfflineTaskRepository.kt`,
`UserDataRepository.kt`, `OfflineFirstUserDataRepository.kt`, `di/DataModule.kt`.
Test: `OfflineTaskRepositoryTest.kt` plus hand-written fakes.

**Step 1: Write `model/TaskMappers.kt`**

```kotlin
package com.eulerity.todo.core.data.model

import com.eulerity.todo.core.database.TaskEntity
import com.eulerity.todo.core.model.Task

fun TaskEntity.asDomain() = Task(id, title, isCompleted, createdDate, createdAt, expiryTime)
fun Task.asEntity() = TaskEntity(id, title, isCompleted, createdDate, createdAt, expiryTime)
```

**Step 2: Write `TaskRepository.kt`**

```kotlin
package com.eulerity.todo.core.data

import com.eulerity.todo.core.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalTime

interface TaskRepository {
    /** Tasks for the current day. Re-evaluates automatically when the day rolls over. */
    fun observeTodaysTasks(): Flow<List<Task>>
    /** Tasks from days before today — the history view. */
    fun observeExpiredTasks(): Flow<List<Task>>
    suspend fun addTask(title: String, expiryTime: LocalTime?)
    suspend fun setCompleted(id: String, completed: Boolean)
    suspend fun deleteTask(id: String)
}
```

**Step 3: Write the failing test `OfflineTaskRepositoryTest.kt` — the headline test**

```kotlin
package com.eulerity.todo.core.data

import app.cash.turbine.test
import com.eulerity.todo.core.common.FakeClock
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OfflineTaskRepositoryTest {

    @Test
    fun `a task added today disappears from today and appears in history tomorrow`() = runTest {
        val dao = FakeTaskDao()
        val dateTimeProvider = FakeDateTimeProvider(FakeClock(LocalDate(2026, 5, 14)))
        val repo = OfflineTaskRepository(dao, dateTimeProvider)

        repo.addTask("ship the build", expiryTime = null)

        repo.observeTodaysTasks().test {
            assertEquals(1, awaitItem().size)   // present today
            dateTimeProvider.rollToNextDay()    // simulate midnight
            assertTrue(awaitItem().isEmpty())   // gone from "today"
            cancelAndConsumeRemainingEvents()
        }
        repo.observeExpiredTasks().test {
            assertEquals(1, awaitItem().size)   // now in history
            cancelAndConsumeRemainingEvents()
        }
    }
}
```

`FakeTaskDao` and `FakeDateTimeProvider` are hand-written fakes in the test
source set. `FakeDateTimeProvider` exposes `rollToNextDay()` which advances the
clock and pushes a new value into its `currentDay` `MutableStateFlow`. Place
`FakeDateTimeProvider` in `core/data/src/test/kotlin/.../` (it implements the
`:core:common` `DateTimeProvider` interface).

**Step 4: Run to verify it fails**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*OfflineTaskRepositoryTest*"`
Expected: FAIL — `OfflineTaskRepository` does not exist.

**Step 5: Implement `OfflineTaskRepository.kt`**

```kotlin
package com.eulerity.todo.core.data

import com.eulerity.todo.core.common.DateTimeProvider
import com.eulerity.todo.core.data.model.asDomain
import com.eulerity.todo.core.database.TaskDao
import com.eulerity.todo.core.database.TaskEntity
import com.eulerity.todo.core.model.Task
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
            taskDao.observeTasksForDate(day).map { it.map(TaskEntity::asDomain) }
        }

    override fun observeExpiredTasks(): Flow<List<Task>> =
        dateTimeProvider.currentDay.flatMapLatest { day ->
            taskDao.observeTasksBeforeDate(day).map { it.map(TaskEntity::asDomain) }
        }

    override suspend fun addTask(title: String, expiryTime: LocalTime?) {
        taskDao.upsert(
            TaskEntity(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                isCompleted = false,
                createdDate = dateTimeProvider.today(),
                createdAt = dateTimeProvider.now(),
                expiryTime = expiryTime,
            ),
        )
    }

    override suspend fun setCompleted(id: String, completed: Boolean) =
        taskDao.updateCompletion(id, completed)

    override suspend fun deleteTask(id: String) = taskDao.deleteById(id)
}
```

**Step 6: Run to verify it passes**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*OfflineTaskRepositoryTest*"`
Expected: PASS. This is the proof that the central constraint works.

**Step 7: Add `UserDataRepository` + `di/DataModule.kt`**

`UserDataRepository` (interface) and `OfflineFirstUserDataRepository` delegate to
`UserPreferencesDataSource`. `di/DataModule.kt` `@Binds` both repository
interfaces to their implementations.

**Step 8: Commit**

```bash
git add core/data
git commit -m "feat(data): compose day-scoped task queries with reactive day provider"
```

## Task 1.7: Use cases — `:core:domain`

**Files:** under `core/domain/src/main/kotlin/com/eulerity/todo/core/domain/` —
`ObserveTodaysTasksUseCase.kt`, `ObserveExpiredTasksUseCase.kt`,
`AddTaskUseCase.kt`, `ToggleTaskCompletionUseCase.kt`, `DeleteTaskUseCase.kt`.
Test: `AddTaskUseCaseTest.kt` plus a `FakeTaskRepository`.

**Step 1: Write the failing test `AddTaskUseCaseTest.kt`**

```kotlin
package com.eulerity.todo.core.domain

import com.eulerity.todo.core.common.FakeClock
import com.eulerity.todo.core.common.FakeDateTimeProvider
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.assertTrue

class AddTaskUseCaseTest {
    private val provider = FakeDateTimeProvider(FakeClock(LocalDate(2026, 5, 14)))

    @Test fun `blank title is rejected`() = runTest {
        val useCase = AddTaskUseCase(FakeTaskRepository(), provider)
        assertTrue(useCase(title = "   ", expiryTime = null).isFailure)
    }

    @Test fun `valid title is forwarded to the repository`() = runTest {
        val repo = FakeTaskRepository()
        val useCase = AddTaskUseCase(repo, provider)
        val result = useCase(title = "buy milk", expiryTime = null)
        assertTrue(result.isSuccess)
        assertTrue(repo.added.any { it.first == "buy milk" })
    }
}
```

**Step 2: Run to verify it fails, then implement `AddTaskUseCase.kt`**

```kotlin
package com.eulerity.todo.core.domain

import com.eulerity.todo.core.common.DateTimeProvider
import com.eulerity.todo.core.data.TaskRepository
import kotlinx.datetime.LocalTime
import javax.inject.Inject

class AddTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val dateTimeProvider: DateTimeProvider,
) {
    suspend operator fun invoke(title: String, expiryTime: LocalTime?): Result<Unit> {
        if (title.isBlank()) {
            return Result.failure(IllegalArgumentException("Title can't be empty"))
        }
        // Reject an expiry time already in the past for today.
        if (expiryTime != null) {
            val nowTime = dateTimeProvider.now()
            // Compare against current local time; if past, fail.
            // (Concrete comparison left to implementation — use the injected zone.)
        }
        return runCatching { repository.addTask(title, expiryTime) }
    }
}
```

**Step 3: Run the test green; implement the four remaining use cases**

`ObserveTodaysTasksUseCase` and `ObserveExpiredTasksUseCase` are thin Flow
pass-throughs (`operator fun invoke(): Flow<List<Task>>`).
`ToggleTaskCompletionUseCase` calls `repository.setCompleted`.
`DeleteTaskUseCase` calls `repository.deleteTask`.

**Step 4: Run the full domain test suite**

Run: `./gradlew :core:domain:testDebugUnitTest`
Expected: PASS.

**Step 5: Commit**

```bash
git add core/domain
git commit -m "feat(domain): add task use cases with input validation"
```

## Task 1.8: Phase 1 verification gate

**Step 1: Run every core module's tests**

Run: `./gradlew :core:model:test :core:common:testDebugUnitTest :core:database:testDebugUnitTest :core:datastore:testDebugUnitTest :core:data:testDebugUnitTest :core:domain:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, all green.

**Step 2: Confirm the headline test passes**

The test `a task added today disappears from today and appears in history
tomorrow` must be green. This is the single most important test in the project.

Commit any fixes, then proceed to `05-phase-2-design-system.md`.
