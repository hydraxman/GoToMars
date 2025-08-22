# Tasks

* [X] `TASK-001` — Core domain & data models (P0)

    * **Description**: Define planet constants, data classes (Planet, Ship, SimulationConfig, Route), math utilities (Vec3), simulation clock.
    * **Maps to**: REQ-001, REQ-003, REQ-004, REQ-005, REQ-008
    * **Acceptance**:
        * Data classes exist with required fields per README models section.
        * Planet catalog provides Sun + 8 planets with semi-major axis AU, radius km, orbital period days, label.
        * Simulation clock returns monotonic seconds and scales by timeScale.
        * Route placeholder struct present (control + segments list).
    * **Artifacts**: domain/{Planet.kt, Ship.kt, SimulationConfig.kt, Route.kt, Math.kt, PlanetCatalog.kt, SimulationClock.kt}
    * **Deps**: —
    * **Estimate**: S
    * **Test Plan**: Unit tests for planet count & invariants, clock scaling, vector math ops.

* [X] `TASK-002` — Renderer scaffold & Compose host (P0)

    * **Description**: Create GLSurfaceView composable, renderer loop skeleton, shader compile/link utilities, frame timing capture (EMA), basic clear.
    * **Maps to**: REQ-009, REQ-010, REQ-001
    * **Acceptance**:
        * GLSurfaceView visible in main activity via Compose.
        * Trivial shaders compile & link; failures logged with info log.
        * Per-frame delta + EMA frame time stored.
    * **Artifacts**: rendering/{Renderer.kt, GlProgram.kt, SurfaceComposable.kt}
    * **Deps**: TASK-001
    * **Estimate**: M
    * **Test Plan**: Instrumented smoke launch; unit test induced shader failure logs error tag.

* [X] `TASK-003` — Sphere mesh & planet rendering (P0)

    * **Description**: Implement UV sphere mesh generator (indices/normals/CCW, pole closure) and render all planets with AU distance scale + visual radius scale.
    * **Maps to**: REQ-001, REQ-009
    * **Acceptance**:
        * Mesh vertex/index counts formula correct; poles closed.
        * Normals unit length within 1e-4.
        * 9 spheres drawn at proportional distances; uniform radius scale applied.
        * Depth testing & back-face culling enabled.
    * **Artifacts**: rendering/{SphereMesh.kt, PlanetRenderer.kt}
    * **Deps**: TASK-002
    * **Estimate**: M
    * **Test Plan**: Unit tests counts & normals; instrumented screenshot vs blank.

* [X] `TASK-004` — Default camera framing (P0)

    * **Description**: Camera struct + initial framing to include Earth & Mars (and Sun) adjusting azimuth if Sun occludes corridor.
    * **Maps to**: REQ-002
    * **Acceptance**:
        * First frame frustum includes Earth & Mars bounding spheres.
        * Occlusion scenario triggers non-zero azimuth offset.
    * **Artifacts**: rendering/Camera.kt
    * **Deps**: TASK-003
    * **Estimate**: S
    * **Test Plan**: Unit test synthetic occlusion; projection bounds check.

* [X] `TASK-005` — Orbital motion update (P0)

    * **Description**: Compute planet angular positions from absolute simulation time & orbital periods; frame-rate independent; timeScale reactive.
    * **Maps to**: REQ-003
    * **Acceptance**:
        * Angle at time T matches analytic value (error < epsilon) regardless of step size.
        * Changing timeScale adjusts angular velocity next frame.
    * **Artifacts**: domain/OrbitCalculator.kt + renderer integration.
    * **Deps**: TASK-004
    * **Estimate**: S
    * **Test Plan**: Unit tests angle progression & timeScale change.

* [X] `TASK-006` — Transfer route computation & rendering (P0)

    * **Description**: Quadratic Bézier control point (perpendicular bisector ∩ Mars orbit + safety offset) & tessellated polyline (>=64 seg) rendering.
    * **Maps to**: REQ-004
    * **Acceptance**:
        * Control point on Mars orbit & perpendicular bisector within tolerance.
        * Min Sun distance ≥ safety threshold.
        * Segments count ≥ 64 (pre-adaptive).
        * Route updates within 1–2 frames of planetary movement.
    * **Artifacts**: domain/RouteBuilder.kt, rendering/RouteRenderer.kt
    * **Deps**: TASK-005
    * **Estimate**: M
    * **Test Plan**: Unit tests perpendicularity, safety enforcement, tessellation count; instrumented fast timeScale change.

* [X] `TASK-007` — Fleet logic & rendering (P1)

    * **Description**: N ships spaced along route; advance by speed (km/h scaled), wrap at end; render markers.
    * **Maps to**: REQ-005
    * **Acceptance**:
        * Initial t spacing ≈ 1/N.
        * Position progression matches speed * dt / routeLength.
        * shipCount change recomputes spacing next frame.
        * Visual wrap seamless (no >1 frame gap).
    * **Artifacts**: domain/FleetManager.kt, rendering/FleetRenderer.kt
    * **Deps**: TASK-006
    * **Estimate**: S
    * **Test Plan**: Unit tests spacing & progression; instrumented continuity check.

* [X] `TASK-008` — Camera gesture controls (P0)

    * **Description**: Drag orbit (azimuth/elevation clamp) & pinch zoom (exponential) anchored on Earth.
    * **Maps to**: REQ-007
    * **Acceptance**:
        * Horizontal drag changes azimuth; vertical drag changes elevation (clamped [-80°, +80°]).
        * Pinch adjusts radius within bounds exponentially.
    * **Artifacts**: ui/GestureHandlers.kt, integration wiring.
    * **Deps**: TASK-004
    * **Estimate**: M
    * **Test Plan**: Unit test zoom mapping; instrumented gesture delta verification.

* [ ] `TASK-009` — Billboard labels (P1)

    * **Description**: Project planets, draw camera-facing labels, resolve overlaps via vertical offset of farther label.
    * **Maps to**: REQ-006
    * **Acceptance**:
        * 9 labels with correct abbreviations.
        * Overlap (< threshold px) offsets farther label vertically.
    * **Artifacts**: ui/LabelRenderer.kt, projection helper.
    * **Deps**: TASK-004
    * **Estimate**: M
    * **Test Plan**: Unit test overlap resolver; screenshot diff.

* [X] `TASK-010` — Settings overlay & live config (P2)

    * **Description**: Bottom sheet controls (timeScale, shipCount, curvatureFactor) applying changes next frame.
    * **Maps to**: REQ-008, REQ-005, REQ-004
    * **Acceptance**:
        * Overlay shows current values.
        * Changing control mutates simulation next frame.
        * shipCount change triggers fleet re-spacing.
    * **Artifacts**: ui/SettingsSheet.kt, ConfigViewModel.kt
    * **Deps**: TASK-007, TASK-006
    * **Estimate**: S
    * **Test Plan**: Unit test config reducer; instrumentation overlay modify check.

* [X] `TASK-011` — Adaptive quality & performance thresholds (P0)

    * **Description**: Monitor frame times; ≥3 consecutive >25ms frames reduce route segments 25% (floor); restore gradually; emit events.
    * **Maps to**: REQ-010, REQ-004
    * **Acceptance**:
        * Synthetic >25ms frames trigger single reduction.
        * Sustained stable frames restore toward original.
        * Quality changes flagged for telemetry.
    * **Artifacts**: domain/AdaptiveQualityController.kt + renderer hook.
    * **Deps**: TASK-006
    * **Estimate**: M
    * **Test Plan**: Unit tests degrade & recover sequences.

* [X] `TASK-012` — GL resource resilience (P0)

    * **Description**: Handle context loss; rebuild shaders, buffers; identical rendering after recreation.
    * **Maps to**: REQ-009
    * **Acceptance**:
        * Simulated surface recreation rebuilds without crash.
        * Post-recreation frame pixel hash within tolerance of pre.
    * **Artifacts**: rendering/ResourceManager.kt, renderer updates.
    * **Deps**: TASK-003, TASK-006
    * **Estimate**: S
    * **Test Plan**: Instrumented pause/resume; unit test resource lifecycle (mock).

* [X] `TASK-013` — Telemetry logging (P1)

    * **Description**: In-memory event logger (session UUID) capturing specified events.
    * **Maps to**: REQ-009, REQ-010
    * **Acceptance**:
        * Events app_launch, gl_init_success, gl_init_failure, frame_drop, interaction_drag, interaction_pinch, settings_open, settings_change recorded.
        * frame_drop recorded on adaptive trigger.
    * **Artifacts**: telemetry/TelemetryLogger.kt + integration points.
    * **Deps**: TASK-002, TASK-011
    * **Estimate**: S
    * **Test Plan**: Unit test ring buffer & ordering; instrumentation event emission.

* [X] `TASK-014` — Validation tests & docs refinement (P0)

    * **Description**: Route safety edge tests (collinear, near threshold), performance timing harness tests, README traceability updates (no requirement changes).
    * **Maps to**: REQ-004, REQ-010, (traceability all)
    * **Acceptance**:
        * Safety tests pass enforcing clearance.
        * Perf harness asserts adaptive triggers behavior.
        * README updated with implemented class references only.
    * **Artifacts**: tests/{RouteSafetyTest.kt, PerformanceTests.kt}, README.md
    * **Deps**: TASK-011, TASK-006
    * **Estimate**: S
    * **Test Plan**: Run unit tests; manual README review.

