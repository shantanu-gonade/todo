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
