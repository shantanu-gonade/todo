package com.eulerity.todo.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.eulerity.todo.core.model.ThemeMode
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals

class UserPreferencesDataSourceTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private fun createDataSource() = UserPreferencesDataSource(
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("prefs_test.preferences_pb") },
        ),
    )

    @Test
    fun `default theme mode is SYSTEM`() = testScope.runTest {
        val source = createDataSource()
        source.userData.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem().themeMode)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setThemeMode persists and re-emits theme mode`() = testScope.runTest {
        val source = createDataSource()
        source.setThemeMode(ThemeMode.DARK)
        source.userData.test {
            assertEquals(ThemeMode.DARK, awaitItem().themeMode)
            cancelAndConsumeRemainingEvents()
        }
    }
}
