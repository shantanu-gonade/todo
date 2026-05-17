/*
 * Copyright 2026 Eulerity, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
