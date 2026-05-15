package com.eulerity.todo.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eulerity.todo.core.domain.AddTaskUseCase
import com.eulerity.todo.core.domain.DeleteTaskUseCase
import com.eulerity.todo.core.domain.ObserveTodaysTasksUseCase
import com.eulerity.todo.core.domain.ToggleTaskCompletionUseCase
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
import javax.inject.Inject

/**
 * MVI ViewModel for the Today screen.
 *
 * State is produced by combining the domain use-case Flow with local ephemeral
 * UI state (sheet visibility, draft fields) via [combine] + [stateIn].
 *
 * One-shot events (haptics, errors) travel through a buffered [Channel] so they
 * are never lost while the collector is temporarily inactive.
 *
 * Single entry point [onIntent] keeps the contract unambiguous for tests and
 * the composable layer.
 */
@HiltViewModel
class TodayViewModel @Inject constructor(
    observeTodaysTasks: ObserveTodaysTasksUseCase,
    private val addTask: AddTaskUseCase,
    private val toggleCompletion: ToggleTaskCompletionUseCase,
    private val deleteTask: DeleteTaskUseCase,
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
            TodayUiState(
                tasks = tasks.map { it.asTaskUi() },   // Compose rule 6: pure mapper
                isLoading = false,
                addSheetVisible = local.addSheetVisible,
                draftTitle = local.draftTitle,
                draftExpiryTime = local.draftExpiryTime,
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
                localState.update { it.copy(addSheetVisible = true) }

            TodayIntent.AddTaskClicked ->
                submitDraft()

            TodayIntent.AddSheetDismissed ->
                localState.update { TodayLocalState() }

            is TodayIntent.DraftExpiryTimeChanged ->
                localState.update { it.copy(draftExpiryTime = intent.time) }

            is TodayIntent.TaskCompletionToggled ->
                toggle(intent.id)

            is TodayIntent.DeleteTask ->
                viewModelScope.launch { deleteTask(intent.id) }
        }
    }

    private fun submitDraft() = viewModelScope.launch {
        val local = localState.value
        addTask(local.draftTitle, local.draftExpiryTime)
            .onSuccess { localState.update { TodayLocalState() } }
            .onFailure { e ->
                localState.update { it.copy(validationError = e.message ?: "Invalid task") }
            }
    }

    private fun toggle(id: String) = viewModelScope.launch {
        // Look up the current completion status from the last known state
        // so we can send the correct toggled value to the use case.
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
    val draftTitle: String = "",
    val draftExpiryTime: LocalTime? = null,
    val validationError: String? = null,
)
