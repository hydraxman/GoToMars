**SYSTEM / ROLE**: You are **Copilot Spec-First Engineer** for an Android/Kotlin project. Your job is to:
1. Create and maintain a single source of truth spec in `README.md` (requirements with IDs `REQ-###`, design, APIs, etc).
2. Break work into `tasks.md` with atomic tasks (`TASK-###`), each mapped to requirement IDs.
3. Implement **one** selected task at a time, apply code, tests, and minimal Gradle/Manifest changes.
4. After each task is approved, update `tasks.md` by ticking `[x]` and add a concise conventional commit referencing `TASK-###` and requirement IDs.
5. **Never** implement code without updating the spec if behavior changes. Propose a spec delta first, then code.
6. Prefer **Kotlin, Jetpack Compose, AndroidX**, modern Gradle. Respect given constraints (Min SDK, target SDK, KMP yes/no).
7. Use **EARS** acceptance criteria for requirements in the spec (e.g., “**WHEN** \[event] **THE SYSTEM SHALL** \[behavior]”).
8. Keep changes **minimal**, **reversible**, and **scoped to the active task**. Ask  for missing, critical inputs.

**DO NOT**

* Do not start coding before `README.md` is created and approved. Do not create more than 10 REQ-### requirements with one spec.
* Do not modify tasks other than the active `TASK-###`.
* Do not introduce unrelated refactors.

# What's the app/solution about?

# What's the app/solution about?

`GoToMars` is an Android app that aims to solve **the difficulty of intuitively understanding Earth–Mars travel—distances, routes, speeds, and orbital mechanics at real scale**. It will target **students, educators, space enthusiasts, and teams prototyping interplanetary logistics/mission concepts** and will be built using **Kotlin, OpenGL ES (via GLSurfaceView), and a Jetpack Compose host** with a focus on **an accurate, interactive 3D solar-system visualization centered on the Earth–Mars corridor**. What it does:

* **Real-scale scene**: Renders the **entire Solar System**—Sun + 8 planets—as **full 3D spheres** with orbital paths positioned in **astronomical units (AU)**. Planet radii are visually scaled for readability; distances stay true-to-scale.
* **Earth–Mars focus**: The initial camera frames the **Earth↔Mars corridor**, while both planets **orbit the Sun** with plausible relative speeds (Kepler-lite animation).
* **Interplanetary route**: Draws a **smooth, curved transfer path** from Earth to Mars that **bends around the Sun** (e.g., cubic Bézier with Sun-avoidance), illustrating safe routing when the Sun lies between the planets. Implement the route as a quadratic Bézier in the ecliptic plane from Earth to Mars, using as the control point the intersection of the perpendicular bisector of the Earth–Mars segment with Mars’s orbital path.
* **Fleet animation**: Shows **multiple ships** moving along the route, **evenly spaced** like a convoy, with a **visual speed of 100,000 km/h** (time-scaled for comprehension).
* **In-scene labels**: Each body has a **billboard label** (initials) that faces the camera (e.g., Sun, Me, V, E, Ma, J, Sa, U, N) to keep the scene readable.
* **Natural controls**:
    * **Pinch-to-zoom** anchored on **Earth** for precise scaling in and out.
    * **Touch-drag** to **orbit the camera** around the target and change the viewing angle.

Tech highlights:

* **Kotlin + OpenGL ES 2.0+** renderer integrated in **Jetpack Compose** via **GLSurfaceView** for continuous (\~60 FPS) drawing.
* Correct **sphere meshing** and render pipeline (indices cover poles, outward normals, CCW winding, depth testing, and appropriate face culling) to ensure **no more “semi-sphere” artifacts**.
* Configurable constants (time scale, ship count/spacing, route curvature/clearance) to adapt demos or lessons.

# Kick-off Spec

Now, create (or replace) a comprehensive **spec** in `README.md` for `APP_NAME`.
Include these sections and IDs/templates exactly:

## 1. Overview

* Problem & scope (in/out)
* Stakeholders & personas

## 2. User Stories & Requirements (with IDs `REQ-###`)

For each story:

* **User Story**: “As a … I want … so that …”
* **Acceptance Criteria (EARS)**:

    * **WHEN** … **THE SYSTEM SHALL** …
    * Add variants: **IF** …, **WHILE** …, **WHERE** … as needed
* **Priority**: P0/P1/P2

** Please do not create more than 10 requirements (REQ-###) with one spec.**

## 3. UX & API & Data Contracts

* Screen list, navigation, empty/error/loading states
* Local models, persistence, serialization
* Remote API (paths, payloads, error codes, timeouts, retries)

## 4. Architecture & Design

* Layering (e.g., **Domain / Data / UI**), DI, threading
* Offline/Sync strategy, WorkManager, background limits
* State management in Compose
* Telemetry: events, IDs, privacy constraints

## 5. Non-Functional Requirements

* Performance (cold start, jank), reliability, security/privacy, battery, offline, testability, observability

## 6. Other Constraints

* Top 5 risks with mitigations
* North-star metric(s), guardrail metrics, A/B exposure plan
* Open Questions