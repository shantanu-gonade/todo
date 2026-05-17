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

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eulerity.todo.core.designsystem.theme.TodoTheme
import com.eulerity.todo.core.model.TaskCategory

/**
 * Horizontal scrollable row of [FilterChip]s — one per [TaskCategory].
 *
 * Exactly one chip can be selected at a time (or none — [TaskCategory.NONE] acts
 * as the "no category" selection). Tapping the currently-selected chip de-selects
 * it back to [TaskCategory.NONE].
 *
 * Colors are derived from [MaterialTheme.colorScheme] via [TaskCategory.categoryColor],
 * so they automatically adapt to light/dark mode.
 *
 * Compose rule 5: [onCategorySelected] is a stable reference passed from the parent.
 * Compose rule 7: Previews are wrapped in [TodoTheme].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryChipRow(
    selected: TaskCategory,
    onCategorySelected: (TaskCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Skip NONE — it is the implicit "no category" state, not shown as a chip.
        TaskCategory.entries
            .filter { it != TaskCategory.NONE }
            .forEach { category ->
                val isSelected = selected == category
                val chipColor = category.categoryColor()
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        // Tapping the already-selected chip de-selects back to NONE.
                        onCategorySelected(if (isSelected) TaskCategory.NONE else category)
                    },
                    label = { Text(category.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipColor.copy(alpha = 0.85f),
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
    }
}

/**
 * Maps a [TaskCategory]'s [TaskCategory.colorRole] to the active M3 [Color].
 * Must be called from a composable scope (reads [MaterialTheme.colorScheme]).
 */
@Composable
fun TaskCategory.categoryColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return when (colorRole) {
        "primary"            -> scheme.primary
        "secondary"          -> scheme.secondary
        "tertiary"           -> scheme.tertiary
        "tertiaryContainer"  -> scheme.tertiaryContainer
        "secondaryContainer" -> scheme.secondaryContainer
        else                 -> scheme.surfaceVariant
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "CategoryChipRow — None selected")
@Composable
private fun CategoryChipRowNonePreview() {
    TodoTheme {
        CategoryChipRow(selected = TaskCategory.NONE, onCategorySelected = {})
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "CategoryChipRow — Work selected")
@Composable
private fun CategoryChipRowWorkPreview() {
    TodoTheme {
        CategoryChipRow(selected = TaskCategory.WORK, onCategorySelected = {})
    }
}
