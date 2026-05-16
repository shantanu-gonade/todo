package com.eulerity.todo.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.eulerity.todo.core.model.ThemeMode
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals

class UserPreferencesDataSourceTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    /**
     * Creates a DataSource whose DataStore scope is tied to [TestScope.backgroundScope].
     *
     * DataStore internally launches a long-lived [StandaloneCoroutine] to collect its state.
     * Passing `this` (TestScope) as the scope makes that coroutine a direct child of the test
     * job, causing [runTest] to wait up to 1 minute for it to finish — which it never does.
     *
     * [backgroundScope] is the correct scope for such infrastructure: it is cancelled
     * automatically when the test body completes, without triggering UncompletedCoroutinesError.
     *
     * A unique filename per call avoids cross-test state sharing.
     */
    private fun kotlinx.coroutines.test.TestScope.createDataSource(name: String = "prefs_test") =
        UserPreferencesDataSource(
            dataStore = PreferenceDataStoreFactory.create(
                scope = backgroundScope,
                produceFile = { tmpFolder.newFile("${name}.preferences_pb") },
            ),
        )

    @Test
    fun `default theme mode is SYSTEM`() = runTest {
        val source = createDataSource("default_test")
        source.userData.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem().themeMode)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setThemeMode persists and re-emits theme mode`() = runTest {
        val source = createDataSource("theme_test")
        source.setThemeMode(ThemeMode.DARK)
        source.userData.test {
            assertEquals(ThemeMode.DARK, awaitItem().themeMode)
            cancelAndConsumeRemainingEvents()
        }
    }
}
