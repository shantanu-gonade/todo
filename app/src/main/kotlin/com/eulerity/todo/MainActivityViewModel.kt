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

package com.eulerity.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eulerity.todo.core.data.UserDataRepository
import com.eulerity.todo.core.model.UserData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for [MainActivity].
 *
 * Reads [UserDataRepository.userData] and exposes it as a [StateFlow] so the
 * Activity can observe the persisted [ThemeMode] and apply the correct
 * dark/light theme on every recomposition.
 *
 * [SharingStarted.WhileSubscribed] with a 5-second timeout keeps the upstream
 * Flow alive through configuration changes while stopping it promptly when the
 * app truly goes to the background.
 */
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    userDataRepository: UserDataRepository,
) : ViewModel() {

    val userData: StateFlow<UserData> = userDataRepository.userData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserData(),
        )
}
