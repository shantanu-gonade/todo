# PRD — Today-Only Todo App

**Date:** 2026-05-14
**Status:** Approved
**Owner:** Shan
**Context:** Eulerity Android internship take-home exercise
**App location:** `Eulerity/todo/`  ·  **Package:** `com.eulerity.todo`

---

## Problem Statement

Conventional todo apps accumulate backlog: unfinished tasks pile up, overdue
items create guilt, and the list stops reflecting what matters now. A person who
just wants to track "what am I doing today" is served a growing graveyard of
yesterday's intentions. This app removes that weight entirely — it cares only
about today. Tasks belong to the current day and expire when the day ends, so
each morning starts clean.

## Goals

1. A user can capture a task for today in under three seconds from app launch.
2. The list always shows exactly today's tasks — never yesterday's, never a
   future date — with zero manual cleanup.
3. The day boundary is correct without the app being opened: if the user skips a
   day, the next launch shows an empty list, not stale tasks.
4. The app works fully offline with no network permission and persists across
   process death and device restart.
5. The codebase demonstrates production Android architecture an interviewer can
   probe — modular, tested, and idiomatic.

## Non-Goals

1. **User accounts or sync** — out of scope per the exercise; the app is
   single-device, local-only.
2. **Future scheduling or recurring tasks** — the today-only constraint is the
   product; adding future dates would contradict it.
3. **A settings screen** — the exercise explicitly excludes complex settings.
   Theme follows the system (with a stored override as the only preference).
4. **Editing a task's title after creation** — keeps the interaction model
   minimal; a user deletes and re-adds instead.
5. **Subtasks, tags, priorities, or sorting controls** — scope creep against a
   deliberately small product.

## User Stories

### Persona: the daily planner (primary)

- As a daily planner, I want to add a task for today so that I can track what I
  intend to get done.
- As a daily planner, I want to mark a task complete so that I can see my
  progress through the day.
- As a daily planner, I want to delete a task so that I can drop something I no
  longer plan to do.
- As a daily planner, I want yesterday's tasks gone when I open the app so that
  my list always reflects today.
- As a daily planner, I want an optional reminder time on a task so that I am
  nudged before the day ends.

### Persona: the returning user (edge cases)

- As a returning user who skipped a day, I want a clean empty list so that I am
  not greeted by stale tasks.
- As a returning user who travelled across timezones, I want "today" to mean my
  current local day so that the list stays correct.

### Persona: the reflective user (optional)

- As a reflective user, I want to view tasks from previous days so that I can
  see what expired.

## Requirements

### Must-Have (P0)

**P0-1 — Add a task for today.**
Acceptance: From the main screen, a user can open an input, type a title, and
save. The task appears immediately in today's list. A blank or whitespace-only
title is rejected with inline feedback and no task is created.

**P0-2 — Mark a task complete / incomplete.**
Acceptance: Tapping a task's checkbox toggles its completed state. The change
persists immediately and survives app restart. Completed tasks remain in today's
list (struck through) until the day ends.

**P0-3 — Delete a task.**
Acceptance: A user can delete a task from today's list. It is removed
immediately and permanently.

**P0-4 — Today-only list.**
Acceptance: The main list shows only tasks whose creation date equals the
current local day. Tasks from any prior day never appear in it.

**P0-5 — Automatic day reset.**
Acceptance: When the local day changes — whether the app is open or was closed —
the main list reflects the new day. If the app is open at midnight, the list
clears without user action. If the app was closed for one or more days, the next
launch shows only the current day's tasks (empty if none were added today). No
background job is required for this to be correct.

**P0-6 — Local persistence, fully offline.**
Acceptance: Tasks persist across process death and device restart. The app
declares no `INTERNET` permission and makes no network calls. `minSdk` is 28.

### Nice-to-Have (P1) — all in scope for this build

**P1-1 — Material 3 theming with light/dark.**
Acceptance: The app uses a Material 3 theme, dynamic color on Android 12+, a
static light/dark scheme below that, and follows the system dark-mode setting.

**P1-2 — Optional per-task expiry time.**
Acceptance: When adding a task, a user may optionally set a same-day expiry
time. The time is displayed on the task row. Setting a time already in the past
for today is rejected.

**P1-3 — Completion animation and haptics.**
Acceptance: Toggling completion plays a brief animation and a haptic tick.

**P1-4 — Thoughtful empty states.**
Acceptance: An empty today list and an empty history list each show a clear,
friendly empty state rather than a blank screen.

**P1-5 — View expired (past) tasks.**
Acceptance: A separate screen lists tasks from days before today, read-only,
grouped or ordered by date. Reachable from the main screen.

**P1-6 — End-of-day reminder notification.**
Acceptance: If incomplete tasks remain, a single local notification fires in the
evening before midnight. No notification fires if the day's tasks are all
complete or none exist.

### Future Considerations (P2) — explicitly not built

- Schema migration history with versioned `Migration` objects.
- `api`/`impl` feature module splits if the app grew more screens.
- Baseline profile for startup performance.
- Screenshot tests.
- Adaptive tablet and foldable layouts.

## Success Metrics

Because this is a take-home exercise, "success" is measured against the exercise
rubric and demonstrable product behavior rather than production analytics.

**Leading indicators (verifiable at build time):**
- Time-to-add a task from cold launch: under three seconds in the demo.
- Headline test green: a task added today is absent from "today" and present in
  "history" after the clock advances one day.
- Zero `INTERNET` permission; `minSdk` 28; project builds with `./gradlew test`
  all green.

**Lagging indicators (the interview itself):**
- Every architectural decision is explainable and defensible in discussion.
- The day-reset behavior is correct across the edge cases in the returning-user
  stories (skipped day, timezone change, process death).

## Open Questions

- *(Engineering)* Does KSP on a pure-JVM `:core:domain` module cooperate with
  the Hilt plugin, or should that module use the Android library convention
  plugin instead? Resolved during Phase 0 — see `01-phase-0-foundation.md`.
- *(Design)* Exact seed color for the Material 3 palette — to be chosen with the
  Material Theme Builder during Phase 2; does not block the plan.

## Timeline Considerations

This is a single deliverable with no hard external deadline beyond the exercise
submission. The implementation is phased (Phases 0–7, see the plan index) so it
can be built and verified incrementally. Each phase ends in a verification gate;
the build is shippable from the end of Phase 6, with Phase 7 covering final
verification, the README, and the demo recording the exercise requires.

## Related Documents

- `01-architecture-and-design.md` — the technical design and rationale.
- `02-plan-index.md` — the phased implementation plan index.
- Exercise brief: `Android Intern Take-Home Exercise.md` (repository root area).
