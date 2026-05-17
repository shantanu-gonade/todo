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

package com.eulerity.todo.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eulerity.todo.core.designsystem.theme.TodoTheme

/**
 * Generic empty-state placeholder reused by both the Today and History screens.
 *
 * Parameterized so each screen can supply its own messaging:
 * - Today screen: "Nothing for today yet"
 * - History screen: "No expired tasks"
 *
 * @param headline Primary message, e.g. "Nothing for today yet"
 * @param supportingText Secondary description shown below the headline
 * @param icon Optional override for the icon; defaults to a check-circle
 */
@Composable
fun TodoEmptyState(
    headline: String,
    supportingText: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        )
    },
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = headline,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = supportingText,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Compose rule 7: @Preview wraps in TodoTheme
@Preview(showBackground = true, name = "Empty State — Today")
@Composable
private fun TodoEmptyStateTodayPreview() {
    TodoTheme {
        TodoEmptyState(
            headline = "Nothing for today yet",
            supportingText = "Add a task to get started",
        )
    }
}

@Preview(showBackground = true, name = "Empty State — History")
@Composable
private fun TodoEmptyStateHistoryPreview() {
    TodoTheme {
        TodoEmptyState(
            headline = "No expired tasks",
            supportingText = "Tasks from previous days will appear here",
        )
    }
}
