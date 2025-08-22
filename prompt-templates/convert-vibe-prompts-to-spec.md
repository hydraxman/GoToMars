You are a senior engineer practicing Specification-Driven Development (SpecDD).
Your job is to CONVERT the following “vibe coding” prompt flow into a clear, reviewable SPEC and EXECUTION PLAN.

INPUT (vibe flow):
<<<PASTE_VIBE_PROMPTS_HERE>>>

OUTPUT (produce exactly these sections, no code yet):
1) README.md — Product + Technical Spec
    - Problem / Goal
    - In Scope / Out of Scope
    - User Stories (EARS/BDD style, concise)
    - Functional Requirements (numbered, testable; consolidate all asks from the vibe flow)
    - UX & Interaction (inputs, gestures, error states)
    - System & Architecture notes (platforms, frameworks, rendering/GL if relevant, data models)
    - Data & Constants (tables for any scales, speeds, thresholds, units)
    - Non-Goals
    - Assumptions
    - Risks & Mitigations
    - Open Questions (call out missing decisions)
    - Acceptance Criteria (checkbox list; each criterion maps to at least one requirement)

2) tasks.md — Plan & Checklist
    - Group tasks by phases (Setup, Core Features, Interactions, Data/Models, Integration, Testing/Perf, Polish).
    - 12–20 atomic tasks; each has:
      • [ ] checkbox
      • Definition of Done (1–2 lines, measurable)
      • Suggested branch name (kebab-case)
    - Include a small tracking table at top: Task | Status | PR link (empty)

3) Implementation Prompts (C1, C2, …)
    - For each task group, provide one concise prompt that a code assistant can execute.
    - Each prompt must:
      • implement ONLY that group,
      • update tasks.md (tick items) after completion,
      • avoid touching areas outside the group,
      • mention any guardrails (tests, error handling, perf toggles).

4) Verification
    - “Acceptance Pass” checklist (step-by-step runbook to verify Acceptance Criteria).
    - “Common Pitfalls” (3–7 bullets derived from the vibe flow; e.g., renderer interfaces, culling/normals, gesture anchors).

CONVERSION RULES:
- Parse the vibe flow, deduplicate overlaps, resolve conflicts, and normalize terminology.
- If the vibe flow includes specific numbers (e.g., speeds, scales), capture them in a “Constants” table with units.
- If details are missing, propose sensible defaults and list them under Assumptions + Open Questions.
- Prefer bullet points over prose; keep everything testable and unambiguous.
- Do NOT generate source code; output only the four sections above, ready for copy into files/PR description.

Now produce the four sections in order with clear headings:
README.md
tasks.md
Implementation Prompts
Verification
