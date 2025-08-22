# GoToMars

Plan your trip to Mars.

<img width="578" height="1239" alt="image" src="https://github.com/user-attachments/assets/4f5931a0-753c-470f-b2f6-73086463d513" />


## Project Structure

High-level layout (omitting build artifacts):

```
app/                     Android application module (Compose + OpenGL ES code lives here)
  src/main/...           Runtime source (to be organized into domain, data, rendering, ui, telemetry packages)
  src/test/...           JVM unit tests
  src/androidTest/...    Instrumented Android tests

prompt-templates/        Spec-first authoring & iteration prompts
  1-kick-off-spec.md     Prompt to create / refresh initial product & technical spec (spec.md)
  2-plan.md              Prompt to derive ordered task list (tasks.md) from spec deltas
  3-do.md                Prompt to implement a single task (TASK-###) atomically
  convert-vibe-prompts-to-spec.md  Helper to translate exploratory "vibe" prompts into concrete spec requirements
  practice/              Sandboxed examples / experiments
    kick-off-spec-gotomars.md  Sample generated spec for practice

.github/
  copilot-instructions.md  Compact on-repo guidance for AI assistant: workflow rules, stack, conventions
  prompts/
    implement.prompt.md    Reusable prompt for implementing a task (TASK-###)
    tests.prompt.md        Reusable prompt to test a change

spec.md                  Living specification (requirements + architecture/NFRs). Must change before code.
tasks.md                 Ordered, traceable implementation tasks (TASK-###) derived from spec.
```

### How the Prompt Templates Work
The `prompt-templates` directory enables a repeatable spec-driven loop:
1. Author / refine requirements with `1-kick-off-spec.md` producing or updating `spec.md` (never code first).
2. Use `2-plan.md` to (re)generate an ordered set of atomic tasks into `tasks.md` (each task maps to future commits/PRs).
3. For an active task (exactly one at a time), drive focused implementation using `3-do.md`.
4. If early exploratory / qualitative ideas exist, run them through `convert-vibe-prompts-to-spec.md` to formalize before planning.
5. `practice/` holds examples that can be referenced or discarded; it is non-normative.

### `.github` Automation & Guidance
These artifacts shape contributor + AI assistant behavior:
- `copilot-instructions.md` enforces: spec-first edits, commit message format (`type(scope): summary (TASK-###, REQ-###)`), constraints (no silent deps), performance + architectural guidelines.
- `prompts/*.prompt.md` are modular prompt snippets. They are intentionally small so tooling (or a human) can inject the right guardrails during implementation or test authoring.

### Working Agreement (Summary)
- Always update `spec.md` first for any behavioral change; regenerate or adjust `tasks.md`; only then implement.
- Exactly one open task in progress; do not batch.
- New dependencies require an accompanying spec & README justification (Architecture / NFR sections).

### Building & Testing (Windows PowerShell 5.1)
```
gradlew app:assembleDebug
gradlew app:testDebugUnitTest
gradlew app:connectedDebugAndroidTest
```
(If chaining conditionally in PS 5.1: `gradlew app:assembleDebug; if ($?) { gradlew app:testDebugUnitTest }`)

### Traceability
Maintain links: each commit references the implementing TASK plus affected REQ IDs (from `spec.md`). When a requirement changes: update spec -> re-plan tasks -> implement.

### Next Steps (Roadmap Snapshot)
See `tasks.md` for the authoritative ordered backlog. Do not infer work from code gaps alone.

---
Feel free to use this to play with spec-driven coding and VS code custom instructions.

<img width="1518" height="842" alt="image" src="https://github.com/user-attachments/assets/f069e05f-fcfd-4d61-9035-0034f394fd0c" />
