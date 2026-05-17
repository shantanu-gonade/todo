package com.eulerity.todo.feature.today

/**
 * One-shot side effects emitted by [TodayViewModel].
 *
 * Unlike [TodayUiState], effects are not retained across recompositions.
 * They are delivered via a [kotlinx.coroutines.channels.Channel] and consumed
 * exactly once in [TodayRoute] inside a lifecycle-aware collector
 * (Compose rule 2: collected with repeatOnLifecycle(STARTED)).
 */
sealed interface TodayEffect {
    /** Fire the device haptic engine to celebrate completing a task. */
    data object TaskCompletedHaptic : TodayEffect

    /** Show a transient Snackbar with an error message. */
    data class ShowError(val message: String) : TodayEffect

    /** Show a transient Snackbar with a success/informational message. */
    data class ShowMessage(val message: String) : TodayEffect
}
