# Implementation Plan — Index

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to
> implement each phase file task-by-task.

**Goal:** Build an offline, today-only Android todo app with Now-in-Android-style
architecture — multi-module Gradle build, convention plugins, MVI features, Room,
Hilt, and Compose Material 3.

**App location:** `Eulerity/todo/`  ·  **Package:** `com.eulerity.todo`
**Root project name:** `todo`

**Architecture:** Eleven Gradle modules with strict downward-only dependencies
(`:app` → `:feature:*` → `:core:*`). `build-logic/convention` supplies nine
convention plugins so every module build file is three to six lines. The
day-only constraint is enforced by query-time filtering driven by a reactive
`DateTimeProvider`, never by a deletion job, so expiration is correct by
construction.

**Tech Stack:** Kotlin 2.x, AGP 9, Gradle 9.1, Compose BOM 2026.05, Hilt, Room,
DataStore, androidx.navigation (Compose), kotlinx-datetime, kotlinx-coroutines,
kotlinx-serialization, WorkManager (notifications only), Turbine + JUnit4 +
Robolectric.

---

## How To Use This Plan

The plan is split into eight phase files. Build them in order — each phase ends
in a verification gate that must pass before the next begins. Within a phase,
each task is a sequence of two-to-five-minute steps. Follow TDD where a task
produces logic: write the failing test, run it, implement minimally, run it
green, commit. Use exact file paths and commands as written. Commit after every
task.

Module source roots use `src/main/kotlin` (configured by the convention
plugins). Test roots are `src/test/kotlin` (JVM/Robolectric) and
`src/androidTest/kotlin` (instrumented). All Gradle commands run from
`Eulerity/todo/` (the directory containing `gradlew`).

## Companion Documents

- `00-PRD.md` — product requirements: problem, goals, user stories, requirements
  with acceptance criteria, success metrics.
- `01-architecture-and-design.md` — the technical design and rationale; read it
  before starting Phase 0.

## Phase Files

| Phase | File | Produces |
|---|---|---|
| 0 | `03-phase-0-foundation.md` | Compiling empty multi-module skeleton: version catalog, `build-logic` with nine convention plugins, eleven module build files, `minSdk` corrected 24→28 |
| 1 | `04-phase-1-core.md` | Data and domain layers: `Task` model, testable `DateTimeProvider`, Room, DataStore, repository composing day-scoped queries, use cases. Includes the headline day-rollover test |
| 2 | `05-phase-2-design-system.md` | `:core:designsystem` (Material 3 theme, tokens, components) and `:core:ui` (model-aware composables) |
| 3 | `06-phase-3-feature-today.md` | The main screen: MVI contract, ViewModel, `TodayScreen`, add-task `ModalBottomSheet`, navigation |
| 4 | `07-phase-4-feature-history.md` | The expired-todos screen: MVI contract, ViewModel, `HistoryScreen`, navigation |
| 5 | `08-phase-5-notifications.md` | End-of-day reminder via WorkManager — the only background job |
| 6 | `09-phase-6-app.md` | App module: Hilt `Application`, `MainActivity`, `NavHost`, theme application, reminder scheduling |
| 7 | `10-phase-7-verification.md` | Full test pass, Compose self-audit, README, demo prep |

## Global Conventions

- **Package root:** `com.eulerity.todo`. Module sub-packages follow the
  namespace column in `01-architecture-and-design.md` §2.
- **Commits:** Conventional Commits (`feat(scope):`, `build:`, `test:`,
  `docs:`, `refactor:`, `chore:`). Commit after every task.
- **Tests:** TDD for all logic. Hand-written fakes, never mocking libraries.
- **Compose:** the seven correctness rules in `01-architecture-and-design.md` §4
  are mandatory — each phase file repeats the ones relevant to it.

## Execution Handoff

After a phase file is complete and its verification gate passes, move to the
next. Two overall execution styles:

1. **Subagent-Driven (one session)** — dispatch a fresh subagent per task,
   review between tasks. Use `superpowers:subagent-driven-development`.
2. **Parallel Session (separate)** — open a new session and run phases in
   batches with checkpoints. Use `superpowers:executing-plans`.

## Appendix: Skills & References Used

This plan set was produced using: `superpowers:brainstorming` (requirements and
design), `superpowers:writing-plans` (the phase files),
`product-management:write-spec` and `product-management:feature-spec` (the PRD),
`engineering:architecture` and `engineering:system-design` (architecture
decisions), `android-development` (NIA-style module structure, layer patterns),
`material-3` (theme, tokens, components, motion), `compose-expert` (the Compose
correctness review folded into the phase files),
`kotlin-development-assistant:kotlin-new` (component scaffolding patterns),
`elements-of-style:writing-clearly-and-concisely` (prose). Research drew on
Google's Now in Android `build-logic` README, the Android modularization
patterns guide, and current stable library versions as of May 2026.

**Key external references:**
- Now in Android: `github.com/android/nowinandroid`
- Modularization patterns: `developer.android.com/topic/modularization/patterns`
- Compose architecture: `developer.android.com/develop/ui/compose/architecture`
