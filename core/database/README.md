# :core:database

Room database module. Contains the `@Database`, `@Dao`, `@Entity`, `TypeConverters`,
and all schema migrations. Nothing outside this module touches SQL directly.

## Database

`TodoDatabase` is version **2**. Schema exports are committed to
`core/database/schemas/` so CI can detect accidental schema drift.

```kotlin
@Database(
    entities = [TaskEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}
```

## Entity — `TaskEntity`

```
tasks
 ├── id           TEXT PRIMARY KEY
 ├── title        TEXT NOT NULL
 ├── isCompleted  INTEGER NOT NULL  (0 = false, 1 = true)
 ├── createdDate  INTEGER NOT NULL  (epoch days)
 ├── createdAt    INTEGER NOT NULL  (epoch milliseconds)
 ├── expiryTime   INTEGER           (second-of-day, nullable)
 └── category     TEXT NOT NULL DEFAULT 'NONE'
```

`category` was added in **v2** via `MIGRATION_1_2`.

## TypeConverters

Room cannot store `kotlinx-datetime` types. `Converters` maps them to primitives:

| Kotlin type | Column type | Method |
|---|---|---|
| `LocalDate` | `INTEGER` | `toEpochDays()` / `fromEpochDays()` |
| `Instant` | `INTEGER` | `toEpochMilliseconds()` / `fromEpochMilliseconds()` |
| `LocalTime?` | `INTEGER?` | `toSecondOfDay()` / `fromSecondOfDay()` |

## DAO — `TaskDao`

Key query signatures:

```kotlin
@Query("SELECT * FROM tasks WHERE createdDate = :day ORDER BY createdAt ASC")
fun observeTasksForDate(day: Long): Flow<List<TaskEntity>>

@Query("SELECT * FROM tasks WHERE createdDate < :day ORDER BY createdDate DESC, createdAt ASC")
fun observeTasksBeforeDate(day: Long): Flow<List<TaskEntity>>

@Upsert
suspend fun upsert(entity: TaskEntity)

@Query("UPDATE tasks SET isCompleted = :completed WHERE id = :id")
suspend fun updateCompletion(id: String, completed: Boolean)

@Query("DELETE FROM tasks WHERE id = :id")
suspend fun deleteById(id: String)
```

## Migration

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN category TEXT NOT NULL DEFAULT 'NONE'")
    }
}
```

## Hilt module

`DatabaseModule` provides the `@Singleton` `TodoDatabase` instance (built with
`Room.databaseBuilder` + all migrations) and exposes `TaskDao` from it.

## Convention plugin

```kotlin
apply plugin: 'todoapp.android.library'
apply plugin: 'todoapp.android.hilt'
apply plugin: 'todoapp.android.room'   // KSP Room processor + schema export path
```
