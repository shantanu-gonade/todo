# Phase 7 — Verification & Documentation

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans and
> superpowers:verification-before-completion.
> Prerequisite: Phase 6 verification gate passed.

**Produces:** a verified, documented, submission-ready app.

**Package root:** `com.eulerity.todo`. Gradle commands run from `Eulerity/todo/`.

---

## Task 7.1: Full test + lint pass

**Step 1: Run every unit test in the project**

Run: `./gradlew test`
Expected: `BUILD SUCCESSFUL` — every module's unit tests green, including the
headline day-rollover test in `:core:data`.

**Step 2: Run lint across all modules**

Run: `./gradlew lintDebug`
Expected: no errors. Address warnings or document why they are acceptable.

**Step 3: Run the instrumented tests** (emulator running)

Run: `./gradlew connectedDebugAndroidTest`
Expected: `BUILD SUCCESSFUL` — `TodayScreenTest` and the WorkManager smoke test
pass.

## Task 7.2: Compose audit (self-review gate)

Review every composable against these criteria — the Compose correctness rules
from `01-architecture-and-design.md` §4 plus general hygiene:

- **Rule 1** — `TodoTheme` syncs `isAppearanceLightStatusBars` with `darkTheme`.
- **Rule 2** — effect Flows are collected inside `repeatOnLifecycle(STARTED)`,
  never a bare `LaunchedEffect`.
- **Rule 3** — the `ModalBottomSheet`'s visibility derives from
  `uiState.addSheetVisible`; it holds no independent visibility state.
- **Rule 4** — `TodoCheckbox` scale animates via `Modifier.graphicsLayer`, not by
  recomposing per frame.
- **Rule 5** — `TaskList` item lambdas are stable: callbacks defined once in the
  screen, not allocated per item.
- **Rule 6** — `Task.asTaskUi()` is a pure function with a default formatter; the
  ViewModel call site and the mapper signature agree.
- **Rule 7** — every `@Preview` wraps content in `TodoTheme`.
- State is hoisted; stateless composables take immutable params plus lambdas.
- `collectAsStateWithLifecycle` is used, never bare `collectAsState`.
- `LazyColumn` items have stable `key`s.
- No hardcoded `Color(...)` or magic `Dp` — tokens via `MaterialTheme` only.
- No business logic in composables; formatting happens in the `TaskUi` mapper.

Fix anything that fails, then commit:

```bash
git add -A
git commit -m "refactor: address Compose audit findings"
```

## Task 7.3: Write the README

**Files:**
- Create: `README.md` at the repository root (`Eulerity/todo/README.md`)

The exercise requires a README covering: overall approach, key decisions and
trade-offs, what you would improve with more time, and what you got stuck on.
Write it as prose, drawing on `00-PRD.md` and `01-architecture-and-design.md`:

- **Approach** — the today-only constraint enforced by query-time filtering;
  NIA-style modular architecture; offline-first with Room; `package
  com.eulerity.todo`.
- **Key decisions & trade-offs** — query-time filtering vs a deletion job (why
  the former is correct by construction and non-destructive); full
  modularization on a small app (deliberate, to demonstrate production
  architecture, with add-task kept as a sheet inside `:feature:today` rather than
  its own module to avoid over-engineering); convention plugins for build
  consistency; MVI with a separate effect channel for one-shot events.
- **What I would improve with more time** — schema migrations with version
  history, `api`/`impl` feature splits if the app grew, a baseline profile,
  screenshot tests, adaptive tablet/foldable layouts.
- **What I got stuck on** — a short, honest note; e.g. the KSP-on-JVM-module
  wrinkle for `:core:domain` flagged in `03-phase-0-foundation.md` Task 0.4, and
  how it was resolved.
- **AI tool usage** — link or attach the conversation transcript, per the
  exercise's required "Use of AI Tools" section.

Commit:

```bash
git add README.md
git commit -m "docs: add README with approach, trade-offs, and AI usage"
```

## Task 7.4: Demo preparation

**Step 1:** Record the 30–60 second demo video: add a task with an expiry time,
mark one complete (show the animation and haptic), open History, show the empty
state, and briefly show the app relaunching with state intact.

**Step 2:** Verify the repository is clean: `git status` shows nothing
uncommitted, the `core/database/schemas/` directory is committed, and
`local.properties` is gitignored.

**Step 3: Final commit if needed**

```bash
git add -A
git commit -m "chore: final cleanup before submission"
```

## Task 7.5: Final verification gate

Use `superpowers:verification-before-completion` — run each command and confirm
the output before claiming done:

- `./gradlew test` — all green, headline day-rollover test included.
- `./gradlew assembleDebug` — builds.
- `./gradlew lintDebug` — no errors.
- App runs on an API 28 emulator; full add → complete → delete → history flow
  works; state survives process death.
- `grep -rn "INTERNET" app/` returns nothing; `minSdk` is 28 in
  `build-logic/`.
- `README.md` covers all four required sections plus AI usage.
- Demo video recorded.

When all seven pass, the submission is complete.

---

## Appendix: Definition of Done (per the PRD)

Cross-check against `00-PRD.md` requirements:

| Requirement | Verified by |
|---|---|
| P0-1 Add a task | `TodayViewModelTest` + manual demo |
| P0-2 Mark complete/incomplete | manual demo + persistence check |
| P0-3 Delete a task | manual demo |
| P0-4 Today-only list | `TaskDaoTest`, `OfflineTaskRepositoryTest` |
| P0-5 Automatic day reset | headline test in `OfflineTaskRepositoryTest` |
| P0-6 Local persistence, offline | `grep INTERNET`, process-death check, `minSdk` 28 |
| P1-1 Material 3 + light/dark | Compose audit, manual demo |
| P1-2 Optional expiry time | `AddTaskUseCase` validation test + demo |
| P1-3 Completion animation + haptics | manual demo |
| P1-4 Empty states | `TodayScreenTest` + manual demo |
| P1-5 View expired tasks | `HistoryViewModelTest` + manual demo |
| P1-6 End-of-day reminder | WorkManager smoke test + manual demo |
