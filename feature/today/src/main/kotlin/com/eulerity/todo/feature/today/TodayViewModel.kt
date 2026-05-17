package com.eulerity.todo.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eulerity.todo.core.common.DateTimeProvider
import com.eulerity.todo.core.domain.AddTaskUseCase
import com.eulerity.todo.core.domain.DeleteTaskUseCase
import com.eulerity.todo.core.domain.ObserveTodaysTasksUseCase
import com.eulerity.todo.core.domain.ToggleTaskCompletionUseCase
import com.eulerity.todo.core.domain.UpdateTaskUseCase
import com.eulerity.todo.core.model.TaskCategory
import com.eulerity.todo.core.ui.asTaskUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

/**
 * MVI ViewModel for the Today screen.
 *
 * State is produced by combining the domain use-case Flow with local ephemeral
 * UI state (sheet visibility, draft fields) via [combine] + [stateIn].
 *
 * One-shot events (haptics, errors, messages) travel through a buffered [Channel]
 * so they are never lost while the collector is temporarily inactive.
 *
 * Single entry point [onIntent] keeps the contract unambiguous for tests and
 * the composable layer.
 */
@HiltViewModel
class TodayViewModel @Inject constructor(
    observeTodaysTasks: ObserveTodaysTasksUseCase,
    private val addTask: AddTaskUseCase,
    private val updateTask: UpdateTaskUseCase,
    private val toggleCompletion: ToggleTaskCompletionUseCase,
    private val deleteTask: DeleteTaskUseCase,
    private val dateTimeProvider: DateTimeProvider,
) : ViewModel() {

    /** Ephemeral UI-only state that does not live in the domain layer. */
    private val localState = MutableStateFlow(TodayLocalState())

    /** Channel-backed one-shot event stream; collected lifecycle-aware in TodayRoute. */
    private val effectChannel = Channel<TodayEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    /**
     * Authoritative screen state.
     *
     * Combines the reactive task list from the repository with local UI state.
     * [SharingStarted.WhileSubscribed] with a 5-second timeout keeps the upstream
     * Flow alive during configuration changes without leaking resources when the
     * screen leaves the back stack.
     */
    val uiState: StateFlow<TodayUiState> =
        combine(observeTodaysTasks(), localState) { tasks, local ->
            val nowLocalTime = dateTimeProvider.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).time
            TodayUiState(
                tasks = tasks.map { task ->
                    val expiry = task.expiryTime
                    task.asTaskUi().copy(
                        isExpired = expiry != null && !task.isCompleted && expiry < nowLocalTime
                    )
                },
                isLoading = false,
                addSheetVisible = local.addSheetVisible,
                editingTaskId = local.editingTaskId,
                draftTitle = local.draftTitle,
                draftExpiryTime = local.draftExpiryTime,
                draftCategory = local.draftCategory,
                validationError = local.validationError,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TodayUiState(isLoading = true),
        )

    fun onIntent(intent: TodayIntent) {
        when (intent) {
            is TodayIntent.DraftTitleChanged ->
                localState.update { it.copy(draftTitle = intent.value, validationError = null) }

            TodayIntent.OpenAddSheet ->
                localState.update { it.copy(addSheetVisible = true, editingTaskId = null) }

            TodayIntent.AddTaskClicked ->
                submitDraft()

            is TodayIntent.EditTaskClicked ->
                openEditSheet(intent.taskId)

            TodayIntent.SaveEditClicked ->
                saveEdit()

            TodayIntent.AddSheetDismissed ->
                localState.update { TodayLocalState() }

            is TodayIntent.DraftCategoryChanged ->
                localState.update { it.copy(draftCategory = intent.category) }

            is TodayIntent.DraftExpiryTimeChanged ->
                localState.update { it.copy(draftExpiryTime = intent.time, validationError = null) }

            is TodayIntent.TaskCompletionToggled ->
                toggle(intent.id)

            is TodayIntent.DeleteTask ->
                viewModelScope.launch { deleteTask(intent.id) }

            is TodayIntent.TaskDroppedToCategory ->
                moveTaskToCategory(intent.taskId, intent.newCategory)
        }
    }

    private fun openEditSheet(taskId: String) = viewModelScope.launch {
        // TaskUi carries the raw expiryTime (not just the formatted label) so the
        // edit sheet can pre-populate the time picker without any lossy re-parsing.
        val task = uiState.value.tasks.find { it.id == taskId } ?: return@launch
        localState.update { _ ->
            TodayLocalState(
                addSheetVisible = true,
                editingTaskId = taskId,
                draftTitle = task.title,
                draftExpiryTime = task.expiryTime,
                draftCategory = task.category,
            )
        }
    }

    private fun submitDraft() = viewModelScope.launch {
        val local = localState.value
        addTask(local.draftTitle, local.draftExpiryTime, local.draftCategory)
            .onSuccess { localState.update { TodayLocalState() } }
            .onFailure { e ->
                localState.update { it.copy(validationError = e.message ?: "Invalid task") }
            }
    }

    private fun saveEdit() = viewModelScope.launch {
        val local = localState.value
        val editId = local.editingTaskId ?: return@launch
        updateTask(editId, local.draftTitle, local.draftExpiryTime, local.draftCategory)
            .onSuccess {
                localState.update { TodayLocalState() }
                effectChannel.send(TodayEffect.ShowMessage("Task updated"))
            }
            .onFailure { e ->
                localState.update { it.copy(validationError = e.message ?: "Invalid task") }
            }
    }

    private fun moveTaskToCategory(taskId: String, newCategory: TaskCategory) =
        viewModelScope.launch {
            val task = uiState.value.tasks.find { it.id == taskId } ?: return@launch
            updateTask(taskId, task.title, task.expiryTime, newCategory)
                .onFailure { e ->
                    effectChannel.send(TodayEffect.ShowError(e.message ?: "Failed to move task"))
                }
        }

    private fun toggle(id: String) = viewModelScope.launch {
        val current = uiState.value.tasks.find { it.id == id }?.isCompleted ?: false
        toggleCompletion(id, !current)
        effectChannel.send(TodayEffect.TaskCompletedHaptic)
    }
}

/**
 * Ephemeral UI-only state that does not need to survive process death.
 * Held separately from the domain-driven task list so [combine] stays clean.
 */
internal data class TodayLocalState(
    val addSheetVisible: Boolean = false,
    val editingTaskId: String? = null,
    val draftTitle: String = "",
    val draftExpiryTime: LocalTime? = null,
    val draftCategory: TaskCategory = TaskCategory.NONE,
    val validationError: String? = null,
)
