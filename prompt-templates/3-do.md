Implement TASK-### only.

1) Restate the acceptance checks you will satisfy.
2) Add tests (unit/instrumented) as diffs that assert the acceptance checks.
3) If any spec gap blocks you, pause and propose a tiny README.md diff to clarify; don’t code until approved.
4) Provide the local commands (e.g., ./gradlew assembleDebug testDebugUnitTest connectedDebugAndroidTest).

After I reply “Approve patch”:
- Output a diff to tick [x] for TASK-### in `tasks.md` (and add one-line outcome).
- Propose a COMMIT_MESSAGE with a Conventional Commit after the task description in `tasks.md`:
  feat({module or scope}): implement REQ-… via TASK-### — short outcome
  Body: rationale, tests added, follow-ups (if any).
