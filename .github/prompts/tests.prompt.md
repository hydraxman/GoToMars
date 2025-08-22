---
mode: 'agent'
model: 'GPT-5'
description: 'GoToMars — run Android tests after a change, aligned to spec-first workflow.'
---

# Goal
After a change, run the Android build and tests for **GoToMars**, then produce a concise report that links results and verifies the change against the **spec (README.md)** and the **active task in tasks.md**.

# Project assumptions (from repo guides)
- Stack: Kotlin (JDK 11), Android minSdk 31 / compileSdk 36, Jetpack Compose + Material3, OpenGL ES 2.0 (GLSurfaceView).
- Tests: unit (domain, adaptive, route), androidTest (smoke, gestures).
- Windows-friendly commands (use `gradlew`, not `./gradlew`).

# Rules (spec-first workflow)
- Only validate changes that correspond to **one open task (TASK-###)** in `tasks.md`.
- If behavior changed, confirm the **requirement IDs (REQ-###)** referenced in README.md.
- Do **not** modify source or Gradle files; this prompt is read/execute/report only.
- Keep suggestions minimal, atomic, and aligned to the spec. No new dependencies.

# Inputs (ask if missing; then proceed with defaults)
- Base ref to compare against (default: `origin/main`)
- Variant/flavor (default: `debug`)
- Whether to run connected tests (default: auto — only if a device/emulator is attached)
- Optional: class/package filters for fast-path testing (default: infer from diff)

# Plan
1) **Load context**
    - Read `README.md` and `tasks.md`; detect the active **TASK-###** and any **REQ-###** referenced in the diff.
    - Note performance targets (≥55 FPS median idle; input latency <32 ms; route segments ≥64 pre-adaptive) for awareness, but do not attempt perf runs unless a benchmark module exists.

2) **Scope the change**
    - `git fetch --all --quiet || true`
    - `git diff --name-only BASE...HEAD`
    - Map changed files to areas: `domain/`, `data/`, `rendering/`, `ui/`, `telemetry/`.
    - Decide a **test shortlist**:
        - If only `domain/` or `data/` changed → prioritize unit tests.
        - If `ui/` or `rendering/` changed → include connected tests (if a device is present).

3) **Sanity checks**
    - `gradlew --version` (verify JDK 11 via wrapper)
    - If no Android SDK/device → **skip** connected tests and note it.
    - Ensure Gradle daemon is clean: `gradlew --stop`

4) **Build & static checks**
    - `gradlew assembleDebug`
    - If `lintDebug` task exists: `gradlew lintDebug` (optional)

5) **Unit tests (module-aware)**
    - Default full run: `gradlew testDebugUnitTest --continue`
    - If filters were inferred from diff (e.g., `com.ms.gotomars.domain.*`), run:
        - `gradlew testDebugUnitTest --tests "com.ms.gotomars.domain.*" --continue`
    - Collect results: `**/build/test-results/testDebugUnitTest/*` and `**/build/reports/tests/testDebugUnitTest/*`

6) **Connected tests (conditional)**
    - `adb devices`
    - If a device/emulator is listed:
        - `gradlew connectedDebugAndroidTest`
        - Collect results: `**/build/outputs/androidTest-results/connected/*` and `**/build/reports/androidTests/connected/*`

7) **Focused smoke for regressions (optional fast path)**
    - If `androidTest` contains gesture/smoke suites (e.g., `GestureSmokeTest`, `RendererSmokeTest`), run them first with:
        - `gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ms.gotomars.ui.GestureSmokeTest`

8) **Report & next actions**
    - Create `reports/copilot-test-run-<timestamp>.md` containing:
        - **Diff summary** (files changed, mapped area)
        - **Active task** (TASK-###) and any **REQ-###** touched
        - **Commands executed**
        - **Unit test results**: passed/failed/skipped (by package)
        - **Connected test results** (if any)
        - **Lint summary** (if run)
        - **Top failures** with first error line and file:line hints
        - **Repro steps** (copy-paste commands)
        - **Next actions**: which file/class to inspect, suggested minimal fixes
    - Do **not** commit; provide the path and brief summary back to the developer.

# Commands (execute as applicable; skip tasks that don’t exist)
```bash
git fetch --all --quiet || true
: ${BASE:=origin/main}
echo "Comparing against $BASE"
git diff --name-only "$BASE"...HEAD || true

gradlew --version
gradlew --stop

# Build
gradlew assembleDebug || exit 1

# Optional lint if available
gradlew -q tasks | findstr /R /C:"lintDebug" >NUL && gradlew lintDebug || echo "lintDebug not found"

# Unit tests (full)
gradlew testDebugUnitTest --continue || true