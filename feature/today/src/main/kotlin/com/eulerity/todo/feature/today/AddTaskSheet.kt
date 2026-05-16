package com.eulerity.todo.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

    // Initialise the time-picker clock to the current draft value if set,
    // otherwise default to 12:00. The ViewModel owns the canonical value;
    // this state is only used to drive the picker UI.
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

            // Title field — validation error shown as supportingText
            OutlinedTextField(
                value = draftTitle,
                onValueChange = { onIntent(TodayIntent.DraftTitleChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Task title") },
                placeholder = { Text("e.g. Buy groceries") },
                isError = validationError != null,
                supportingText = if (validationError != null) {
                    { Text(text = validationError, color = MaterialTheme.colorScheme.error) }
                } else null,
                singleLine = true,
            )

            // Expiry-time row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { showTimePicker = !showTimePicker }) {
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

            // Inline time picker — only visible when toggled
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

            // Primary action
            Button(
                onClick = { onIntent(TodayIntent.AddTaskClicked) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add task")
            }
        }
    }
}

// Compose rule 7: @Preview wraps in TodoTheme
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
