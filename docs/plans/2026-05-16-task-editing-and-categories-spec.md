# Feature Spec: Task Editing & Categories

**Status:** Draft  
**Author:** Shan  
**Date:** 2026-05-16  
**App context:** Eulerity Today-Only Todo — Android, Kotlin + Compose, offline-only, min API 28  
**Inspiration:** Apple Reminders (lists/groups), Google Tasks (task edit in-place, notes), TickTick (tags + color labels)

---

## 1. Problem Statement

Users currently cannot edit a task once it has been created. If they mistype a title or want to change an expiry time, their only recourse is delete-and-recreate — a friction-heavy workflow that breaks the "stay in flow" goal of a quick-capture todo app. Additionally, all tasks are undifferentiated: a user who juggles multiple contexts (Work, Personal, Errands) has no way to visually separate them in a single-day list.

**Who experiences this:** Any user who adds more than 2–3 tasks per day — which is most users after the first few days of adoption.

**Cost of not solving it:**
- Task editing: moderate daily friction; users lose their task context when forced to retype.
- Categories: the list becomes visually noisy and cognitively harder to scan as task count grows above ~5.

---

## 2. Goals

### User Goals
1. **Edit without disruption** — A user can correct a task title or change its expiry time in under 5 seconds, without losing their position in the list.
2. **Instant category assignment** — A user can assign a category to a task at creation time or retroactively in a single tap — no extra navigation required.
3. **Visual separation by context** — A user scanning their list can distinguish Work tasks from Personal tasks at a glance without reading every title.

### Business Goals
4. **Increase daily active usage** — Users who manage categorized tasks return more often; target 20% increase in sessions-per-user-per-day within 30 days of launch.
5. **Submission differentiation** — The feature demonstrates product thinking and architecture depth beyond the minimum spec, strengthening the intern take-home evaluation.

---

## 3. Non-Goals

| Non-goal | Reason |
|---|---|
| Categories that persist across days | The app's core constraint is today-only. Categories are labels, not persistent entities — they reset with the task list each day. |
| Nested categories / sub-lists | Too complex for v1; adds significant data model changes. |
| Shared or collaborative categories | The app is single-user, offline-only. No accounts. |
| Reordering tasks within a category | Manual drag-and-drop is a full project on its own. Sort-by-category is sufficient for v1. |
| Category-based notifications | Scoping notification logic to categories before the basic notification flow is stable adds risk. |
| Category statistics or analytics | Out of scope for a take-home; no backend to aggregate to. |

---

## 4. User Stories

### Task Editing

**P0 — Core editing**

- As a user who mis-typed a task title, I want to tap on the task and edit the title inline so that I can fix the mistake without deleting and recreating the task.
- As a user who created a task without an expiry time, I want to add an expiry time to an existing task so that I can be reminded of it later in the day.
- As a user who set the wrong expiry time, I want to update a task's expiry time to a later hour so that it no longer shows as expired when I still plan to do it.

**P1 — Edit quality**

- As a user editing a task, I want to see the same bottom sheet I used to create it (pre-populated) so that the interaction feels consistent and familiar.
- As a user who started editing a task, I want to cancel the edit and return to the unchanged task so that accidental taps don't cause unwanted changes.

**Edge cases**

- As a user trying to save an edit with a blank title, I want to see an inline validation error so that I understand why my save didn't work.
- As a user who edits a task's expiry time to a time that has already passed today, I want to see a clear error so that I don't accidentally create an already-expired task.

---

### Categories

**P0 — Core categories**

- As a user with mixed Work and Personal tasks, I want to assign a category to each task so that I can visually distinguish my contexts at a glance.
- As a user adding a new task, I want to choose a category from a small chip-based picker in the bottom sheet so that I can label it without extra navigation.
- As a user looking at my task list, I want tasks grouped or color-coded by category so that I can mentally switch context without re-reading every title.

**P1 — Category management**

- As a user who wants a custom category, I want to create a new category with a name and a color so that my list reflects my personal vocabulary (e.g. "Gym", "Groceries").
- As a user who assigned the wrong category, I want to change a task's category by editing the task so that I don't need to delete it.
- As a user who has no tasks in a category, I want the category to not appear as an empty section so that my view stays clean.

**P2 — Category filtering**

- As a user with many tasks across multiple categories, I want to filter the list to show only one category at a time so that I can focus on a single context.

---

## 5. Requirements

### 5A: Task Editing

#### P0 — Must Have

**E1 — Edit entry point**
- Tapping a task card's title (not the checkbox or delete button) opens the Add/Edit bottom sheet pre-populated with the task's current title and expiry time.
- The sheet title changes from "New task" to "Edit task" when in edit mode.
- Acceptance criteria:
  - [ ] Tapping the title area on any `TaskCard` opens the sheet with the existing title pre-filled
  - [ ] Tapping the checkbox still toggles completion (does not open edit sheet)
  - [ ] Tapping the delete button still deletes (does not open edit sheet)

**E2 — Save edit**
- Tapping "Save" (was "Add task") updates the task in Room and closes the sheet.
- If the title has not changed and no other fields changed, the save is a no-op (no Room write).
- Acceptance criteria:
  - [ ] Tapping "Save" with a valid title persists the change immediately
  - [ ] The task list updates reactively (no manual refresh)
  - [ ] A blank title shows the same validation error as during creation: "Title can't be empty"
  - [ ] An expiry time in the past shows: "Expiry time has already passed"

**E3 — Cancel edit**
- Dismissing the sheet (swipe down or tap outside) discards all changes — the task is unchanged.
- Acceptance criteria:
  - [ ] Swiping down the sheet discards changes
  - [ ] The original task values are intact after dismissal

**E4 — Edit intent / state**
- `TodayIntent` gains `data class EditTaskClicked(val taskId: String)` and `data class SaveEditClicked` entries.
- `TodayLocalState` gains `editingTaskId: String? = null` to distinguish add vs. edit mode.
- The ViewModel wires `EditTaskClicked` → loads the task into draft fields and sets `editingTaskId`.
- Acceptance criteria:
  - [ ] `editingTaskId` is null in add mode and set to the task's ID in edit mode
  - [ ] `AddSheetDismissed` clears `editingTaskId` back to null
  - [ ] All new intent paths have unit tests in `TodayViewModelTest`

#### P1 — Nice to Have

**E5 — Edit feedback**
- After a successful save, a brief Snackbar confirms "Task updated" (reuses the existing `TodayEffect.ShowError` channel — add a new `ShowMessage` variant).

---

### 5B: Categories

#### P0 — Must Have

**C1 — Built-in default categories**
Six pre-defined categories ship with v1. Users cannot delete them but can ignore them.

| Name | Color token | Icon |
|---|---|---|
| Personal | `tertiary` (pink) | `Icons.Outlined.Person` |
| Work | `primary` (purple) | `Icons.Outlined.Work` |
| Errands | `secondary` (slate) | `Icons.Outlined.ShoppingCart` |
| Health | `tertiaryContainer` (rose) | `Icons.Outlined.FitnessCenter` |
| Home | `secondaryContainer` (lavender) | `Icons.Outlined.Home` |
| None (default) | `surfaceVariant` | — |

**C2 — Assign category at creation**
- The Add Task bottom sheet gains a horizontal scrollable row of category chips below the expiry-time row.
- Tapping a chip selects it (selected = filled chip, M3 `FilterChip`); tapping again deselects (reverts to "None").
- Default selection is "None".
- Acceptance criteria:
  - [ ] Category chip row is visible in the Add Task sheet
  - [ ] Exactly one category can be selected at a time (or none)
  - [ ] Selected chip uses `FilterChip` selected state (filled background)
  - [ ] The chosen category is saved with the task to Room

**C3 — Visual category indicator on TaskCard**
- Each `TaskCard` shows a colored left border (2dp) matching the task's category color.
- No category (None) → no border / `surfaceVariant` border.
- Acceptance criteria:
  - [ ] Work tasks show a purple left border
  - [ ] Personal tasks show a pink left border
  - [ ] Tasks with no category show no prominent border
  - [ ] Border color comes from the M3 color scheme token (adapts to dark mode)

**C4 — Data model changes**
- `Task` domain model gains `category: TaskCategory` (default `TaskCategory.NONE`).
- `TaskEntity` gains `category: String` (stored as enum name, default `"NONE"`).
- Room migration: `ALTER TABLE tasks ADD COLUMN category TEXT NOT NULL DEFAULT 'NONE'`.
- `TaskCategory` is a sealed class / enum defined in `core:model`.
- Acceptance criteria:
  - [ ] Existing tasks (pre-migration) get `NONE` category automatically
  - [ ] The Room schema version is bumped and a migration is provided
  - [ ] `TaskMappers` maps `TaskEntity.category` → `TaskCategory`

#### P1 — Nice to Have

**C5 — Custom category creation**
- A "+ New category" chip at the end of the chip row opens a small dialog: name field + color picker (6 preset swatches).
- Custom categories are persisted in DataStore (not Room — they survive across days).
- A user can have up to 5 custom categories (enforced with an inline error if exceeded).
- Acceptance criteria:
  - [ ] "+ New category" chip is always the last item in the chip row
  - [ ] Name field: max 20 characters, blank name is rejected
  - [ ] Color picker shows 6 swatches (M3 tertiary, error, secondary, primary, tertiary, surface)
  - [ ] Created category persists and appears in the chip row on next app launch
  - [ ] More than 5 custom categories shows: "You've reached the category limit"

**C6 — Group tasks by category in Today list**
- When at least 2 different categories are in use, the task list groups tasks by category with a sticky section header (category name + icon).
- Within each section, tasks are sorted by creation time.
- "None" category tasks appear at the bottom under "Uncategorized".
- Acceptance criteria:
  - [ ] Section headers are sticky (`LazyColumn` sticky header)
  - [ ] Tasks within a section are ordered by `createdAt`
  - [ ] Sections with zero tasks do not appear
  - [ ] If all tasks share one category (or all are None), no grouping headers are shown (flat list)

#### P2 — Future Considerations

**C7 — Category filter tab row**
- A horizontally scrollable tab row at the top of the Today screen lets the user filter to a single category.
- "All" tab is always first and is the default.

**C8 — Category-level completion**
- A "Complete all" action per category section header that marks all tasks in that section done at once.

---

## 6. Success Metrics

| Metric | Target | Measurement window |
|---|---|---|
| Edit feature adoption (% of users who edit ≥1 task) | ≥ 30% of DAU within 14 days of launch | 14 days post-launch |
| Category assignment rate (% of tasks created with a non-None category) | ≥ 25% of all created tasks | 30 days post-launch |
| Delete-and-recreate events (proxy for "needed to edit") | Decrease ≥ 40% vs. pre-feature baseline | 30-day comparison |
| Crash-free sessions | ≥ 99.5% | Ongoing |
| P0 task completion rate in HistoryViewModelTest + new tests | 100% passing | CI gate |

*Note: Since this is an offline-only app with no analytics backend, "metrics" for the take-home will be demonstrated via demo video and the README tradeoffs section. The targets above document intent for a real production launch.*

---

## 7. Open Questions

| # | Question | Owner | Blocking? |
|---|---|---|---|
| OQ1 | Should the edit sheet pre-validate the expiry time on open (i.e., show "already expired" immediately if the saved expiry has passed)? Or only on save? | Engineering + UX | No — default to validate on save; revisit if user testing shows confusion |
| OQ2 | When a task's category is changed, should the task move to a new section immediately (with animation) or only on next load? | Engineering | No — default to immediate reactive update via Flow |
| OQ3 | Should custom categories created today be available tomorrow? Or reset with the task list? | Product (Shan) | Yes — categories should persist across days (they're a user preference, not a task) |
| OQ4 | Is Room migration backward-compatible if the user downgrades the app? | Engineering | No — document in proguard-rules and release notes |
| OQ5 | Do we need a confirmation dialog before destructive category deletion (P1 future)? | UX | No — not in scope for v1 |

---

## 8. Architecture Guidance (Android-specific)

This section is non-standard for a PRD but useful given this is an Android take-home.

### Data model changes
```
core:model
  TaskCategory.kt          ← new sealed class / enum
  Task.kt                  ← add category: TaskCategory field

core:database
  TaskEntity.kt            ← add category: String field
  Converters.kt            ← add CategoryConverter if needed
  TodoDatabase.kt          ← bump version (e.g. 2), add migration

core:data
  OfflineTaskRepository.kt ← update addTask/updateTask signatures
  TaskRepository.kt        ← update interface
```

### New use case
```
core:domain
  UpdateTaskUseCase.kt     ← new; validates title, expiry, delegates to repo
```

### Feature: today
```
feature:today
  TodayIntent.kt           ← add EditTaskClicked(id), SaveEditClicked
  TodayLocalState.kt       ← add editingTaskId: String?
  TodayViewModel.kt        ← wire new intents
  AddTaskSheet.kt          ← rename to AddEditTaskSheet; accept editMode: Boolean
  CategoryChipRow.kt       ← new composable; horizontal chip scroll
```

### Timeline estimate (rough)
- Data model + migration: 2h
- UpdateTaskUseCase + tests: 1h
- TodayViewModel edits + tests: 2h
- AddEditTaskSheet (edit mode): 2h
- CategoryChipRow composable: 1.5h
- TaskCard category border: 1h
- Grouping (P1): 3h
- Custom categories (P1): 3h
- **P0 total: ~10h** | **P0+P1 total: ~16h**
