package com.eulerity.todo.core.data

import com.eulerity.todo.core.model.ThemeMode
import com.eulerity.todo.core.model.UserData
import kotlinx.coroutines.flow.Flow

interface UserDataRepository {
    val userData: Flow<UserData>
    suspend fun setThemeMode(mode: ThemeMode)
}
