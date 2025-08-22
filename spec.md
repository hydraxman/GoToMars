# GoToMars

Authoritative spec (Spec-First). Any functional change MUST update this file before code.

---
## 1. Overview
**Problem**: Understanding real-scale Earth↔Mars travel (vast distances, orbital motion, transfer trajectories, timing) is unintuitive with flat 2D diagrams or non‑scaled visuals.

**Scope (In)**:
* Real-scale (distance) 3D visualization of Sun + 8 planets in ecliptic plane (radii visually up-scaled for legibility; orbital distances in AU kept proportional)
* Animated planetary orbits with plausible relative angular velocities (“Kepler‑lite”: circular simplification for phase illustration)
* Focused Earth↔Mars transfer depiction with sample convoy of ships on a curved transfer path
* Interactive camera (orbit, pinch zoom anchored on Earth)
* In‑scene billboard labels for celestial bodies
* Fleet animation along dynamically recomputed Earth→Mars route (quadratic Bézier construction)
* Adjustable simulation constants (time scale, ship count, spacing, route curvature factor)

**Scope (Out / Deferred)**:
* Accurate elliptical orbits / full ephemeris import
* Launch window optimization, delta‑v computation
* Atmospheric entry / landing phases
* Multiplayer / networking
* Persisted user preferences (initial release uses in‑memory config)
* AR mode, VR integration
* Accessibility adaptations (TTS, high‑contrast) – slated for later

**Stakeholders & Personas**:
* Student (Sam): Learns planetary mechanics visually.
* Educator (Elena): Demonstrates transfer concepts in class live.
* Space Enthusiast (Eli): Explores orbits & convoy timing.
* Mission Concept Analyst (Mira): Rapidly communicates spatial relationships in early discussions.
* Product Owner (PO): Ensures clarity, performance, pedagogical value.

---
## 2. User Stories & Requirements
(Max 10) Each functional requirement is numbered REQ-###.

### REQ-001 Planetary System Rendering
**User Story**: As a learner I want to see the Sun and all 8 planets positioned at real-scale orbital distances so that I grasp how spread out the Solar System is.
**Acceptance Criteria (EARS)**:
* WHEN the app launches THE SYSTEM SHALL render Sun + Mercury..Neptune spheres positioned by orbital radius (AU scaled to scene units) on the ecliptic plane.
* WHEN rendering spheres THE SYSTEM SHALL apply visual radius scaling (uniform factor) while preserving distance scale.
* WHEN frames are drawn THE SYSTEM SHALL ensure back-face culling, depth testing, outward normals, and complete pole coverage (no hollow / clipped poles).
**Priority**: P0

### REQ-002 Default Earth–Mars Corridor View
**User Story**: As a new user I want the camera to frame Earth and Mars with the Sun context so that I immediately understand their relative positions.
**Acceptance Criteria**:
* WHEN the first frame after initialization is displayed THE SYSTEM SHALL position the camera so Earth & Mars are both visible (unless collinear occultation by Sun) with minimal empty space.
* IF the Sun lies exactly between Earth and Mars (within angular threshold) THE SYSTEM SHALL bias camera azimuth to avoid total overlap.
**Priority**: P0

### REQ-003 Animated Orbits
**User Story**: As a user I want planets to move smoothly around the Sun so that elapsed time and relative motion are intuitively perceived.
**Acceptance Criteria**:
* WHEN simulation time advances THE SYSTEM SHALL update each planet’s angular position using circular period constants (approx real orbital periods) scaled by a global timeScale.
* WHILE the app is in foreground THE SYSTEM SHALL animate at target ~60 FPS (device permitting) with deterministic motion tied to a monotonic clock * timeScale.
**Priority**: P0

### REQ-004 Transfer Route Curve
**User Story**: As a learner I want to see a realistic-looking curved Earth→Mars path avoiding direct Sun intersection so that I grasp why transfers arc around the Sun.
**Acceptance Criteria**:
* WHEN Earth and Mars positions are known THE SYSTEM SHALL compute a quadratic Bézier in ecliptic plane with start=Earth, end=Mars, control point at intersection of the perpendicular bisector of the segment Earth–Mars and Mars’s orbital circle (choosing nearer intersection that increases Sun clearance angle).
* IF the computed control point produces a path whose minimum Sun distance is below a safety threshold THE SYSTEM SHALL radially offset the control point outward along its vector from Sun until threshold met.
* WHEN drawing the route THE SYSTEM SHALL tessellate it into >= 64 segments for smoothness.
**Priority**: P0

### REQ-005 Fleet Animation
**User Story**: As a user I want multiple ships spaced along the route so that travel staging and convoy concepts are visible.
**Acceptance Criteria**:
* WHEN ships are initialized THE SYSTEM SHALL create N ship instances (default 5) with evenly spaced parametric positions along the route.
* WHILE simulation time advances THE SYSTEM SHALL progress each ship by (shipSpeed * dt) wrapping to start when reaching end.
* IF ship count changes THE SYSTEM SHALL recompute spacing without app restart.
**Priority**: P1

### REQ-006 Billboard Labels
**User Story**: As a user I want short labels for each body that always face the camera so that I can identify planets quickly.
**Acceptance Criteria**:
* WHEN a frame is rendered THE SYSTEM SHALL draw a 2D overlay or camera-facing quad label (Sun, Me, V, E, Ma, J, Sa, U, N) at each body’s projected position.
* IF a label would overlap another (screen distance < threshold) THE SYSTEM SHALL vertically offset the further body’s label slightly.
**Priority**: P1

### REQ-007 Camera Interaction
**User Story**: As a user I want to orbit and zoom smoothly around Earth so that I can inspect spatial relationships from different angles.
**Acceptance Criteria**:
* WHEN user performs a touch-drag gesture THE SYSTEM SHALL rotate camera azimuth/elevation around Earth within clamped elevation (e.g., -80°..+80°).
* WHEN user performs pinch gesture THE SYSTEM SHALL adjust camera radius exponentially (pinch scale mapped to distance) anchored on Earth, clamped to min/max.
* IF frame rate falls below threshold (e.g., <40 FPS) WHILE interacting THE SYSTEM SHALL reduce route tessellation adaptively.
**Priority**: P0

### REQ-008 Configurable Simulation Constants
**User Story**: As an educator I want to tweak time scale, ship count, and route curvature live so that I can tailor demonstrations.
**Acceptance Criteria**:
* WHEN user opens the (temporary) overlay settings panel THE SYSTEM SHALL present controls for timeScale, shipCount, curvatureFactor.
* WHEN a control is changed THE SYSTEM SHALL apply the new value next frame (no restart) and persist only in-memory.
**Priority**: P2

### REQ-009 Rendering Pipeline Integrity
**User Story**: As a developer I want a robust GLES 2.0 pipeline so that spheres and paths render correctly without artifacts.
**Acceptance Criteria**:
* WHEN sphere meshes are generated THE SYSTEM SHALL include latitude/longitude seams closed with proper index ordering (CCW) and normals normalized.
* WHEN GL surface is created THE SYSTEM SHALL compile & link shaders (vertex/color or vertex+fragment) and validate status before first draw; failures logged with shader info log.
* IF context is lost (onResume) THE SYSTEM SHALL recreate GL resources (programs, buffers) before resuming animation.
**Priority**: P0

### REQ-010 Performance & Frame Budget (Functional Aspect)
**User Story**: As a user I want smooth visualization so that learning isn’t hindered by stutter.
**Acceptance Criteria**:
* WHEN typical device (baseline mid‑tier 2023) runs the scene THE SYSTEM SHALL sustain ≥ 55 FPS median during idle orbit.
* WHILE interacting (drag/pinch) THE SYSTEM SHALL keep input latency < 32 ms median.
* IF frame time exceeds 25 ms for 3 consecutive frames THE SYSTEM SHALL trigger adaptive quality (reduce ship trail/route segment count by 25% until stable).
**Priority**: P0

---
## 3. UX & API & Data Contracts
**Screens**:
1. Main Solar Corridor Screen (Compose host containing GLSurfaceView + minimal overlay controls toggle)
2. (Overlay) Settings Panel Sheet (time scale, ships, curvature) – ephemeral; no navigation stack beyond this

**Navigation**: Single-activity; modal bottom sheet for settings.

**States**:
* Loading: Brief shader/mesh init (show simple progress indicator over dark background) < 1s target.
* Error: GL init failure → Snackbar + retry button (recreate surface).
* Empty: Not applicable.

**Models** (Kotlin data classes / records):
* Planet(id: PlanetId, name: String, semiMajorAxisAu: Double, radiusKm: Double, orbitalPeriodDays: Double, color: ColorRGB, label: String)
* Ship(id: Int, paramT: Double)
* Route(control: Vec3, segments: List<Vec3>)
* SimulationConfig(timeScale: Double, shipCount: Int, shipSpeedKmPerHour: Double, curvatureFactor: Double)

**Serialization / Persistence**: None initial release (in-memory only). Potential future: DataStore for config.

**Remote API**: None (all deterministic local computation). Error Codes: N/A. Timeouts: N/A. Retries: N/A.

**Events (UI intents)**: Drag(deltaX, deltaY), Pinch(scaleDelta), ToggleSettings, UpdateConfig(field,value), RetryInit.

---
## 4. Architecture & Design
**Layering**:
* UI (Compose): manages gestures, overlays, projects labels.
* Rendering (GL Engine): scene graph-ish objects (Planets, Route, Ships) + renderer loop.
* Domain: simulation time advancement, orbital position formulae, route recomputation, adaptive quality policy.
* Data: static planet definitions provider.

**DI**: Simple manual wiring (object singletons) – no external DI framework for initial scope.

**Threading**:
* GL Thread (from GLSurfaceView) executes rendering + simulation stepping (deterministic advancement with deltaTime from a monotonic clock), minimal work allocation to avoid GC.
* Main Thread handles Compose state & gesture collection; posts config changes to renderer via thread-safe queue.

**State Management**:
* Remembered Compose state (SimulationConfigState) mirrored into renderer atomic snapshot.
* One-way data flow: Gestures -> ViewModel/State -> Renderer command queue.

**Offline / Sync**: Not applicable (purely local).

**WorkManager / Background**: Not used; rendering pauses when app backgrounded (GL surface paused) and simulation time does not accumulate.

**Adaptive Quality Strategy**:
* Monitor moving average frame time (EMA). If > threshold: decrement tessellation / route segments, else attempt gradual restoration.

**Telemetry**:
* Events: app_launch, gl_init_success|failure, frame_drop (with frameTimeMs), interaction_drag, interaction_pinch, settings_open, settings_change(field).
* IDs: anonymized session UUID (ephemeral), no PII.
* Privacy: No external transmission (local log only) initial release.

**Error Handling**:
* Shader compile/link failure: log + user retry.
* Context loss: auto resource rebuild.

---
## 5. Non-Functional Requirements
* Performance: Cold start (splash to first rendered frame) < 1500 ms on baseline; steady ~60 FPS target.
* Reliability: Recover from GL context loss without crash; zero uncaught exceptions in normal flow.
* Security/Privacy: No network; minimal telemetry stored in volatile memory only.
* Battery: Suspend rendering (requestRenderOnDemand) if no interaction and planets off-screen delta < small angle for >30s (future optimization; optional for v1).
* Testability: Orbital math & route computation isolated in pure functions with unit tests; frame adaptation logic tested with simulated frame times.
* Observability: Basic structured logging (tagged) + frame time stats.
* Accessibility (Future): Color contrast configuration, alternative label mode.

---
## 6. Other Constraints
**Top 5 Risks & Mitigations**:
1. Performance drops on lower-end devices → Mitigation: adaptive tessellation + profiling early.
2. Numerical instability / drift in long sessions → Mitigation: derive positions from absolute simulation time not incremental integration.
3. Gesture jank due to main-thread work → Mitigation: keep recompute tasks off main (only lightweight state updates).
4. Over-complication of future accurate orbital physics → Mitigation: modular domain layer with pluggable orbit model.
5. Poor educational clarity (visual clutter) → Mitigation: label overlap management + adjustable ship count.

**North-Star Metrics**:
* Average interactive session duration (time with >1 user gesture per minute)
* Number of educational demonstrations (settings panel opened + ≥2 config changes) per session

**Guardrail Metrics**:
* Median frame rate, 95th percentile frame time
* Average battery drain % per 10 min session
* Memory footprint (RSS) < target (e.g., < 200 MB)

**A/B Exposure Plan**: Not in initial release; later could test different default curvatureFactor or ship counts.

**Open Questions**:
* Should route reflect Hohmann timing (phase angle gating) or remain illustrative always? (Current: always visible.)
* Include asteroid belt / moons later? Prioritization TBD.
* Add educational annotation overlays (distances, time to arrival)?
* Persistence of user config required for MVP?
* Support landscape-only vs both orientations?

---
**Traceability Matrix (Initial)**:
* Rendering planets: REQ-001
* Default camera: REQ-002
* Orbit animation: REQ-003
* Transfer path: REQ-004
* Fleet: REQ-005
* Labels: REQ-006
* Camera controls: REQ-007
* Config panel: REQ-008
* GL pipeline integrity: REQ-009
* Performance functional thresholding: REQ-010

End of spec.

