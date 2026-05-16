package com.eulerity.todo

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eulerity.todo.core.data.notification.ReminderScheduler
import com.eulerity.todo.core.designsystem.theme.TodoTheme
import com.eulerity.todo.core.model.ThemeMode
import com.eulerity.todo.navigation.TodoNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single-activity host for the Todo app.
 *
 * Responsibilities:
 *  - Enable edge-to-edge display so Compose content extends under system bars.
 *  - Schedule the end-of-day reminder on every launch (idempotent via WorkManager
 *    [ExistingWorkPolicy.REPLACE]).
 *  - Observe the persisted [ThemeMode] from [MainActivityViewModel] and apply
 *    [TodoTheme] accordingly before composing the [TodoNavHost].
 *
 * All navigation and UI logic lives in [TodoNavHost] and the feature screens —
 * this Activity is intentionally thin.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    private val viewModel: MainActivityViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied — we don't force it */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        reminderScheduler.schedule()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val userData by viewModel.userData.collectAsStateWithLifecycle()

            val darkTheme = when (userData.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            TodoTheme(darkTheme = darkTheme) {
                TodoNavHost()
            }
        }
    }
}
