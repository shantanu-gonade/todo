package com.eulerity.todo.feature.today

import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.eulerity.todo.core.ui.to12hLabel
import kotlinx.datetime.LocalTime

/**
 * Add-task bottom sheet.
 *
 * Compose rule 3: visibility is entirely controlled by the caller (TodayScreen
 * only renders this composable when `uiState.addSheetVisible == true`).
 * `onDismissRequest` routes back to the ViewModel via [TodayIntent.AddSheetDismissed].
 *
 * The time picker is shown/hidden via a local `showTimePicker` flag that lives
 * inside this composable — it is purely presentation state, not domain state.
 *
 * ## Keyboard dismissal strategy
 * ModalBottomSheet runs in its own dialog window. The correct way to dismiss
 * the IME is to call InputMethodManager.hideSoftInputFromWindow() using the
 * window token from LocalView.current *inside* the sheet's content lambda —
 * that view belongs to the dialog window, giving us the right token.
 * focusManager.clearFocus() is called first so the TextField stops being the
 * IME target; then hideSoftInputFromWindow forcibly dismisses the IME on that
 * dialog window.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheet(
    draftTitle: String,
    draftExpiryTime: LocalTime?,
    validationError: String?,
    onIntent: (TodayIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState()
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

        // Single function that reliably hides the IME on the dialog window.
        // Step 1: clear Compose focus (makes TextField stop being the IME target)
        // Step 2: call IMM.hideSoftInputFromWindow with the DIALOG window token
        fun hideKeyboard() {
            focusManager.clearFocus()
            val imm = ContextCompat.getSystemService(context, InputMethodManager::class.java)
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "New task",
                style = MaterialTheme.typography.titleLarge,
            )

            OutlinedTextField(
                value = draftTitle,
                onValueChange = { onIntent(TodayIntent.DraftTitleChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        // When the title field gains focus while the time picker is
                        // open, close the picker. The keyboard will appear naturally
                        // because the field is now focused — the picker would collide.
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
                keyboardActions = KeyboardActions(
                    onDone = { hideKeyboard() }
                ),
            )

            // Expiry-time row: tapping the clock icon hides the keyboard first,
            // then toggles the time picker.
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

            Button(
                onClick = { onIntent(TodayIntent.AddTaskClicked) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add task")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "AddTaskSheet — Empty")
@Composable
private fun AddTaskSheetEmptyPreview() {
    TodoTheme {
        AddTaskSheet(
            draftTitle = "",
            draftExpiryTime = null,
            validationError = null,
            onIntent = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "AddTaskSheet — Validation error")
@Composable
private fun AddTaskSheetErrorPreview() {
    TodoTheme {
        AddTaskSheet(
            draftTitle = "",
            draftExpiryTime = null,
            validationError = "Title cannot be blank",
            onIntent = {},
        )
    }
}
