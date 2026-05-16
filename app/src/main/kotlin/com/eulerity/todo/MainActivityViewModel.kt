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
