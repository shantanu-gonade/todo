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

import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.eulerity.todo.core.designsystem.theme.TodoTheme
import com.eulerity.todo.core.model.TaskCategory
import com.eulerity.todo.core.ui.to12hLabel
import kotlinx.datetime.LocalTime

/**
 * Add/Edit task bottom sheet.
 *
 * When [editingTaskId] is null the sheet is in **add mode**: title reads "New task"
 * and the primary button fires [TodayIntent.AddTaskClicked].
 *
 * When [editingTaskId] is non-null the sheet is in **edit mode**: title reads
 * "Edit task" and the primary button fires [TodayIntent.SaveEditClicked].
 * The sheet is pre-populated with [draftTitle] and [draftExpiryTime] from the
 * ViewModel (which loaded them when [TodayIntent.EditTaskClicked] was received).
 *
 * Compose rule 3: visibility is entirely controlled by the caller (TodayScreen
 * only renders this composable when `uiState.addSheetVisible == true`).
 * `onDismissRequest` routes back to the ViewModel via [TodayIntent.AddSheetDismissed].
 */
@Suppress("CyclomaticComplexMethod", "CognitiveComplexMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskSheet(
    draftTitle: String,
    draftExpiryTime: LocalTime?,
    validationError: String?,
    onIntent: (TodayIntent) -> Unit,
    modifier: Modifier = Modifier,
    editingTaskId: String? = null,
    draftCategory: TaskCategory = TaskCategory.NONE,
) {
    val isEditMode = editingTaskId != null
    // skipPartiallyExpanded = true — prevents the sheet from stopping at the
    // half-expanded state on first open. Without this flag the default 3-state
    // sheet (Hidden → HalfExpanded → Expanded) means the sheet only opens to
    // ~50 % height until the user drags it up, which also causes the keyboard
    // and TimePicker to appear to "cut off" the sheet instead of expanding it.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    val timePickerState: TimePickerState = rememberTimePickerState(
        initialHour = draftExpiryTime?.hour ?: 12,
        initialMinute = draftExpiryTime?.minute ?: 0,
        is24Hour = false,
    )

    ModalBottomSheet(
        onDismissRequest = { onIntent(TodayIntent.AddSheetDismissed) },
        sheetState = sheetState,
        modifier = modifier,
    ) {
        // LocalView.current INSIDE the sheet content resolves to the dialog's
        // own ComposeView — its windowToken belongs to the dialog window, not
        // the Activity. This is the token we must pass to hideSoftInputFromWindow.
        val view = LocalView.current
        val context = LocalContext.current
        val focusManager = LocalFocusManager.current
        val focusRequester = remember { FocusRequester() }

        fun hideKeyboard() {
            focusManager.clearFocus()
            val imm = ContextCompat.getSystemService(context, InputMethodManager::class.java)
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                // imePadding() pushes the Column's bottom up when the soft keyboard
                // appears, keeping "Add task" / "Save" visible above the keyboard.
                // Must come BEFORE navigationBarsPadding so both insets stack correctly.
                .imePadding()
                // Ensure content clears the navigation bar when no keyboard is showing.
                .navigationBarsPadding()
                // Allow the column to scroll when TimePicker adds ~300dp of height —
                // this prevents the "OK/Cancel" buttons being cut off at the bottom.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = if (isEditMode) "Edit task" else "New task",
                style = MaterialTheme.typography.titleLarge,
            )

            OutlinedTextField(
                value = draftTitle,
                onValueChange = { onIntent(TodayIntent.DraftTitleChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused && showTimePicker) {
                            showTimePicker = false
                        }
                    },
                label = { Text("Task title") },
                placeholder = { Text("e.g. Buy groceries") },
                isError = validationError != null,
                supportingText = if (validationError != null) {
                    { Text(text = validationError, color = MaterialTheme.colorScheme.error) }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { hideKeyboard() }),
            )

            // Expiry-time row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {
                    if (!showTimePicker) hideKeyboard()
                    showTimePicker = !showTimePicker
                }) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = if (showTimePicker) "Hide time picker" else "Set expiry time",
                        tint = if (draftExpiryTime != null)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (draftExpiryTime != null) {
                    Text(
                        text = "Expires at ${draftExpiryTime.to12hLabel()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { onIntent(TodayIntent.DraftExpiryTimeChanged(null)) }) {
                        Text("Clear")
                    }
                } else {
                    Text(
                        text = "No expiry time",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (showTimePicker) {
                TimePicker(state = timePickerState)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onIntent(
                                TodayIntent.DraftExpiryTimeChanged(
                                    LocalTime(timePickerState.hour, timePickerState.minute)
                                )
                            )
                            showTimePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Category chip row (spec C2)
            CategoryChipRow(
                selected = draftCategory,
                onCategorySelected = { onIntent(TodayIntent.DraftCategoryChanged(it)) },
            )

            Button(
                onClick = {
                    if (isEditMode) onIntent(TodayIntent.SaveEditClicked)
                    else onIntent(TodayIntent.AddTaskClicked)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isEditMode) "Save" else "Add task")
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "AddEditTaskSheet — Add mode")
@Composable
private fun AddTaskSheetEmptyPreview() {
    TodoTheme {
        AddEditTaskSheet(
            draftTitle = "",
            draftExpiryTime = null,
            validationError = null,
            onIntent = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "AddEditTaskSheet — Edit mode")
@Suppress("MagicNumber")
@Composable
private fun EditTaskSheetPreview() {
    TodoTheme {
        AddEditTaskSheet(
            draftTitle = "Buy groceries",
            draftExpiryTime = LocalTime(14, 30),
            validationError = null,
            onIntent = {},
            editingTaskId = "task-1",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "AddEditTaskSheet — Validation error")
@Composable
private fun AddTaskSheetErrorPreview() {
    TodoTheme {
        AddEditTaskSheet(
            draftTitle = "",
            draftExpiryTime = null,
            validationError = "Title can't be empty",
            onIntent = {},
        )
    }
}
