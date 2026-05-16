package com.eulerity.todo.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.animation.animateColorAsState
import com.eulerity.todo.core.designsystem.component.TodoCheckbox
import com.eulerity.todo.core.designsystem.theme.TodoTheme

/**
 * Stateless card row for a single task.
 *
 * Receives an immutable [TaskUi] snapshot and typed callbacks — no ViewModel
 * reference, no side effects. Correct by construction.
 *
 * Layout: [TodoCheckbox] | title + optional expiry label | delete [IconButton]
 */
@Composable
fun TaskCard(
    task: TaskUi,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
) {
    val isExpiredActive = task.isExpired && !task.isCompleted
    val titleColor by animateColorAsState(
        targetValue = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                      else MaterialTheme.colorScheme.onSurface,
        label = "titleColor",
    )
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = if (isExpiredActive) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            )
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TodoCheckbox(
                checked = task.isCompleted,
                onCheckedChange = if (readOnly) ({}) else onToggle,
                enabled = !readOnly,
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = titleColor,
                    textDecoration = if (task.isCompleted) {
                        TextDecoration.LineThrough
                    } else {
                        TextDecoration.None
                    },
                )
                if (task.expiryLabel != null) {
                    Text(
                        text = "Expires ${task.expiryLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isExpiredActive) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            if (!readOnly) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete task",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// Compose rule 7: @Preview wraps in TodoTheme
@Preview(showBackground = true, name = "TaskCard — Active")
@Composable
private fun TaskCardActivePreview() {
    TodoTheme {
        TaskCard(
            task = TaskUi(id = "1", title = "Buy groceries", isCompleted = false, expiryLabel = "18:00"),
            onToggle = {},
            onDelete = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "TaskCard — Completed")
@Composable
private fun TaskCardCompletedPreview() {
    TodoTheme {
        TaskCard(
            task = TaskUi(id = "2", title = "Call dentist", isCompleted = true, expiryLabel = null),
            onToggle = {},
            onDelete = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
