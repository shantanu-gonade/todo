package com.eulerity.todo.core.data

import com.eulerity.todo.core.datastore.UserPreferencesDataSource
import com.eulerity.todo.core.model.ThemeMode
import com.eulerity.todo.core.model.UserData
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class OfflineFirstUserDataRepository @Inject constructor(
    private val dataSource: UserPreferencesDataSource,
) : UserDataRepository {
    override val userData: Flow<UserData> = dataSource.userData
    override suspend fun setThemeMode(mode: ThemeMode) = dataSource.setThemeMode(mode)
}
