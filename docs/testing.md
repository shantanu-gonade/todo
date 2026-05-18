# Testing Strategy

The project follows a **fake-over-mock** philosophy. Every test boundary is crossed
using real fake implementations, not mocked objects. This means tests break on
real interface contract violations — not on method signature changes alone.

No Mockito, MockK, or any mocking framework is used.

---

## Test infrastructure

| Tool | Role |
|---|---|
| JUnit 5 | Test runner for all unit tests |
| [Turbine](https://github.com/cashapp/turbine) | Flow testing — `flow.test {}` DSL |
| Robolectric | JVM-based Android unit tests (no emulator required) |
| `kotlinx-coroutines-test` | `runTest`, `TestCoroutineScheduler`, `UnconfinedTestDispatcher` |
| `androidx.test.ext:junit` | AndroidX JUnit integration |

---

## Fakes

All fakes live in `core/testing/src/main/kotlin/`. They are published as a test
fixture so every module that needs them can add:

```kotlin
testImplementation(projects.core.testing)
```

### `FakeTaskDao`

An in-memory `TaskDao` backed by a `MutableStateFlow<List<TaskEntity>>`. Implements
the full DAO interface. Downstream queries (`observeTasksForDate`, `observeTasksBeforeDate`)
are derived flows with `filter` and `sortedBy` applied on the in-memory list.

```kotlin
class FakeTaskDao : TaskDao {
    private val tasks = MutableStateFlow<List<TaskEntity>>(emptyList())

    override fun observeTasksForDate(day: Long): Flow<List<TaskEntity>> =
        tasks.map { list -> list.filter { it.createdDate == day } }

    override suspend fun upsert(entity: TaskEntity) {
        tasks.update { current ->
            current.filterNot { it.id == entity.id } + entity
        }
    }
    // …
}
```

### `FakeDateTimeProvider`

```kotlin
class FakeDateTimeProvider(
    initialDate: LocalDate = LocalDate(2024, 1, 15),
) : DateTimeProvider {
    private val _currentDay = MutableStateFlow(initialDate)
    override val currentDay: Flow<LocalDate> = _currentDay.asStateFlow()
    override fun today(): LocalDate = _currentDay.value
    override fun now(): Instant = _currentDay.value.atTime(12, 0).toInstant(TimeZone.UTC)

    fun advanceDay() { _currentDay.update { it.plus(1, DateTimeUnit.DAY) } }
}
```

`advanceDay()` lets tests simulate midnight without any system clock manipulation.

### `FakeTaskExpiryScheduler`

Records calls to `schedule()` and `cancel()` in lists for assertion:

```kotlin
class FakeTaskExpiryScheduler : TaskExpiryScheduler {
    val scheduled = mutableListOf<Triple<String, String, LocalTime>>()
    val cancelled = mutableListOf<String>()

    override fun schedule(taskId: String, taskTitle: String, expiryTime: LocalTime) {
        scheduled += Triple(taskId, taskTitle, expiryTime)
    }
    override fun cancel(taskId: String) { cancelled += taskId }
}
```

### `FakeTaskRepository`

Implements `TaskRepository` using an in-memory `MutableStateFlow<List<Task>>`.
Mirrors the behaviour of `OfflineTaskRepository` without any Room or Hilt dependency.

---

## Layer-by-layer patterns

### Data layer — `OfflineTaskRepository`

Tests confirm the reactive day-reset behaviour:

```kotlin
@Test
fun `observeTodaysTasks switches query when day advances`() = runTest {
    val fakeDao = FakeTaskDao()
    val fakeClock = FakeDateTimeProvider(LocalDate(2024, 1, 15))
    val repo = OfflineTaskRepository(fakeDao, fakeClock, FakeTaskExpiryScheduler())

    repo.observeTodaysTasks().test {
        // seed a task on day 1
        fakeDao.upsert(taskEntity(createdDate = LocalDate(2024, 1, 15)))
        assertThat(awaitItem()).hasSize(1)

        // advance the clock → query switches, no tasks for new day
        fakeClock.advanceDay()
        assertThat(awaitItem()).isEmpty()

        cancelAndIgnoreRemainingEvents()
    }
}
```

### Domain layer — use cases

Use cases are tested with `FakeTaskRepository` and `FakeDateTimeProvider`. Because
they are pure Kotlin, no Robolectric is needed:

```kotlin
@Test
fun `addTask returns failure when title is blank`() = runTest {
    val useCase = AddTaskUseCase(FakeTaskRepository(), FakeDateTimeProvider(), FakeTaskExpiryScheduler())
    val result = useCase("", null, TaskCategory.NONE)
    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull()?.message).contains("empty")
}

@Test
fun `addTask returns failure when expiry time is in the past`() = runTest {
    val fakeClock = FakeDateTimeProvider()
    val useCase = AddTaskUseCase(FakeTaskRepository(), fakeClock, FakeTaskExpiryScheduler())
    val past = fakeClock.today().atTime(8, 0).time   // fakeClock.now() is noon
    val result = useCase("Buy milk", past, TaskCategory.NONE)
    assertThat(result.isFailure).isTrue()
}
```

### UI layer — ViewModels

ViewModel tests use `FakeTaskRepository` (or individual fake use cases), a
`TestCoroutineScheduler`, and Turbine for state assertions:

```kotlin
@Test
fun `opening add sheet sets addSheetVisible true`() = runTest {
    val viewModel = TodayViewModel(
        observeTodaysTasks = FakeObserveTodaysTasksUseCase(),
        addTaskUseCase = FakeAddTaskUseCase(),
        // …
        dateTimeProvider = FakeDateTimeProvider(),
    )

    viewModel.uiState.test {
        assertThat(awaitItem().addSheetVisible).isFalse()
        viewModel.onIntent(TodayIntent.OpenAddSheet)
        assertThat(awaitItem().addSheetVisible).isTrue()
        cancelAndIgnoreRemainingEvents()
    }
}
```

Effects are tested by collecting `viewModel.effects` in a parallel coroutine:

```kotlin
@Test
fun `completing a task sends haptic effect`() = runTest {
    val viewModel = buildTestViewModel()
    val effects = mutableListOf<TodayEffect>()

    backgroundScope.launch {
        viewModel.effects.collect { effects += it }
    }

    viewModel.onIntent(TodayIntent.TaskCompletionToggled("id-1", true))
    advanceUntilIdle()

    assertThat(effects).contains(TodayEffect.TaskCompletedHaptic)
}
```

### Database layer — DAO tests

DAO tests use an **in-memory Room database** with Robolectric, exercising real SQL:

```kotlin
@RunWith(RobolectricTestRunner::class)
class TaskDaoTest {
    private lateinit var db: TodoDatabase
    private lateinit var dao: TaskDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TodoDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.taskDao()
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun `observeTasksForDate returns only tasks matching that day`() = runTest {
        dao.upsert(taskEntity(id = "1", createdDate = LocalDate(2024, 1, 15).toEpochDays()))
        dao.upsert(taskEntity(id = "2", createdDate = LocalDate(2024, 1, 16).toEpochDays()))

        dao.observeTasksForDate(LocalDate(2024, 1, 15).toEpochDays()).test {
            assertThat(awaitItem().map { it.id }).containsExactly("1")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

---

## Running tests

```bash
# All unit tests across all modules
./gradlew test

# Single module
./gradlew :core:domain:test

# With coverage report
./gradlew testDebugUnitTestCoverage

# Static analysis
./gradlew detekt
./gradlew spotlessCheck
```

---

## What is not tested (and why)

| Area | Reason |
|---|---|
| `TodoTheme` rendering | Covered by screenshot tests if added; logic-free |
| `AlarmManagerTaskExpiryScheduler` | Android system service; covered by integration test |
| `EndOfDayReminderWorker` | WorkManager integration test; not unit testable without a real `WorkManager` instance |
| Compose UI layout | No UI tests in scope; ViewModel + use-case coverage is the priority |

UI tests with Compose Test Rule (`createComposeRule`) and screenshot tests with
Paparazzi are the natural next additions when the app moves to production.

---

## Guidelines for new tests

1. **Always use fakes**, never mocks. If a fake does not exist, create it in
   `:core:testing`.
2. **Test the contract, not the implementation.** Assert on `UiState` properties and
   emitted `Effect` values, not on internal ViewModel fields.
3. **Use Turbine for all Flow assertions.** Avoid `first()` or `take(1)` — they hide
   missing emissions.
4. **Name tests descriptively** using backtick syntax:
   `fun \`addTask returns failure when title is blank\`()`.
5. **One assertion per logical concept.** Split into multiple `@Test` functions rather
   than asserting five unrelated things in one test.
