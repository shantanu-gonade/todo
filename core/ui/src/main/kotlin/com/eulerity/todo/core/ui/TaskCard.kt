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

package com.eulerity.todo.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.eulerity.todo.core.model.TaskCategory
import com.eulerity.todo.core.designsystem.component.TodoCheckbox
import com.eulerity.todo.core.designsystem.theme.TodoTheme


/**
 * Maps [TaskCategory.colorRole] to the current M3 color token.
 * Called from composable scope only.
 */
@Composable
private fun TaskCategory.resolveColor(): Color {
    val s = androidx.compose.material3.MaterialTheme.colorScheme
    return when (colorRole) {
        "primary"            -> s.primary
        "secondary"          -> s.secondary
        "tertiary"           -> s.tertiary
        "tertiaryContainer"  -> s.tertiaryContainer
        "secondaryContainer" -> s.secondaryContainer
        else                 -> Color.Transparent
    }
}

/**
 * Stateless card row for a single task.
 *
 * Receives an immutable [TaskUi] snapshot and typed callbacks — no ViewModel
 * reference, no side effects. Correct by construction.
 *
 * Layout: [TodoCheckbox] | title + optional expiry label
 *
 * Deletion is handled exclusively by swipe-to-dismiss in [TaskList] — there is
 * no in-card delete button. This keeps the card surface clean and avoids
 * duplicate affordances.
 */
@Suppress("CognitiveComplexMethod")
@Composable
fun TaskCard(
    task: TaskUi,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    onEdit: (() -> Unit)? = null,
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
            // CenterVertically: strip, checkbox, and text column are centred on the
            // row's cross-axis. The text Column uses weight(1f) and wraps its own height,
            // so the Row naturally grows to fit two lines (title + expiry label) while
            // every other element centres within that height — correct M3 two-line pattern.
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Spec C3: 4 dp wide coloured left indicator strip.
            // fillMaxHeight + fraction keeps the strip proportional to the Row height
            // whether the card is 1-line (title only) or 2-line (title + expiry label).
            if (task.category != TaskCategory.NONE) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(40.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(task.category.resolveColor()),
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            TodoCheckbox(
                checked = task.isCompleted,
                onCheckedChange = if (readOnly) ({}) else onToggle,
                enabled = !readOnly,
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (onEdit != null && !readOnly)
                            Modifier.clickable(onClick = onEdit)
                        else Modifier
                    ),
                verticalArrangement = Arrangement.Center,
            ) {
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
            modifier = Modifier.padding(16.dp),
        )
    }
}
