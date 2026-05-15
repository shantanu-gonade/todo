package com.eulerity.todo.feature.today

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.eulerity.todo.core.designsystem.theme.TodoTheme
import com.eulerity.todo.core.ui.TaskUi
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for [TodayScreen].
 *
 * Tests use the stateless [TodayScreen] composable directly, so no Hilt
 * or ViewModel wiring is needed. State is injected as plain [TodayUiState].
 */
class TodayScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `empty state is shown when there are no tasks`() {
        composeRule.setContent {
            TodoTheme {
                TodayScreen(uiState = TodayUiState(isLoading = false))
            }
        }

        composeRule
            .onNodeWithText("Nothing for today yet")
            .assertIsDisplayed()
    }

    @Test
    fun `task list is shown when tasks are present`() {
        composeRule.setContent {
            TodoTheme {
                TodayScreen(
                    uiState = TodayUiState(
                        isLoading = false,
                        tasks = listOf(
                            TaskUi(id = "1", title = "Buy milk", isCompleted = false, expiryLabel = null),
                        ),
                    ),
                )
            }
        }

        composeRule
            .onNodeWithText("Buy milk")
            .assertIsDisplayed()
    }

    @Test
    fun `tapping FAB sends OpenAddSheet intent`() {
        val intents = mutableListOf<TodayIntent>()
        composeRule.setContent {
            TodoTheme {
                TodayScreen(
                    uiState = TodayUiState(isLoading = false),
                    onIntent = { intents += it },
                )
            }
        }

        composeRule.onNodeWithText("+").performClick()

        assert(intents.any { it == TodayIntent.OpenAddSheet }) {
            "Expected OpenAddSheet intent but got: $intents"
        }
    }

    @Test
    fun `add task sheet is visible when addSheetVisible is true`() {
        composeRule.setContent {
            TodoTheme {
                TodayScreen(
                    uiState = TodayUiState(
                        isLoading = false,
                        addSheetVisible = true,
                    ),
                )
            }
        }

        composeRule
            .onNodeWithText("New task")
            .assertIsDisplayed()
    }

    @Test
    fun `validation error is shown in the add task sheet`() {
        composeRule.setContent {
            TodoTheme {
                TodayScreen(
                    uiState = TodayUiState(
                        isLoading = false,
                        addSheetVisible = true,
                        validationError = "Title cannot be blank",
                    ),
                )
            }
        }

        composeRule
            .onNodeWithText("Title cannot be blank")
            .assertIsDisplayed()
    }
}
