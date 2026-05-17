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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import com.eulerity.todo.core.designsystem.theme.TodoTheme

/**
 * Animated checkbox following M3 interaction patterns.
 *
 * Compose rule 4: the scale animation is applied inside the **draw phase**
 * via [Modifier.graphicsLayer]. This avoids triggering layout on every frame —
 * only the graphics layer is re-drawn, not the full composition subtree.
 *
 * The spring spec produces a natural bounce when the user checks a task,
 * reinforcing the sense of accomplishment.
 */
@Composable
fun TodoCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scale by animateFloatAsState(
        targetValue = if (checked) 1.15f else 1f,
        animationSpec = spring(),
        label = "checkbox-scale",
    )
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
    )
}

// Compose rule 7: @Preview wraps in TodoTheme
@Preview(showBackground = true, name = "Checkbox — Unchecked")
@Composable
private fun TodoCheckboxUncheckedPreview() {
    TodoTheme {
        TodoCheckbox(checked = false, onCheckedChange = {})
    }
}

@Preview(showBackground = true, name = "Checkbox — Checked")
@Composable
private fun TodoCheckboxCheckedPreview() {
    TodoTheme {
        TodoCheckbox(checked = true, onCheckedChange = {})
    }
}
