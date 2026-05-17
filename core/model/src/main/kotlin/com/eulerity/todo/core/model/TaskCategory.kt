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

package com.eulerity.todo.core.model

/**
 * Built-in task categories. [NONE] is the default — tasks without a category.
 *
 * Colors are expressed as M3 color role names so the UI layer maps them to the
 * current [MaterialTheme.colorScheme] token (adapts to light/dark automatically).
 *
 * Categories intentionally DO NOT persist across days — they are labels on tasks,
 * not independent entities. (Custom categories via DataStore are a P1 feature.)
 */
enum class TaskCategory(
    /** Human-readable label shown in UI. */
    val label: String,
    /** M3 color role name — the UI layer maps this to colorScheme.* */
    val colorRole: String,
) {
    NONE(label = "None", colorRole = "surfaceVariant"),
    PERSONAL(label = "Personal", colorRole = "tertiary"),
    WORK(label = "Work", colorRole = "primary"),
    ERRANDS(label = "Errands", colorRole = "secondary"),
    HEALTH(label = "Health", colorRole = "tertiaryContainer"),
    HOME(label = "Home", colorRole = "secondaryContainer"),
}
