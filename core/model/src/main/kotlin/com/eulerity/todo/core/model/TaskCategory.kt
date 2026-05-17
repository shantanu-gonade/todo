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
