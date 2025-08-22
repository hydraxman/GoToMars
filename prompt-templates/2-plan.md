Read the current `README.md`, and create `tasks.md` with an ordered backlog.

**Format exactly:**

# Tasks

* [ ] `TASK-001` — Short title (P0)

    * **Description**: …
    * **Maps to**: REQ-###, REQ-###
    * **Acceptance**: bullet list mirroring REQ acceptance in test-speak
    * **Artifacts**: files/classes to add or change
    * **Deps**: `TASK-…`
    * **Estimate**: S / M / L
    * **Test Plan**: unit/instrumented, what to verify
* [ ] `TASK-002` — …

**Rules:**

* Keep tasks atomic (merge/split as needed).
* Try to keep tasks count under 15.
* Put **tests** as first-class tasks where appropriate.
* Include an the build command in the **Test Plan**. (For Android, use `./gradlew ....`.)
