# Copilot Project Guide

Purpose: Assist implementation of spec in README.md via tasks in tasks.md. Keep suggestions minimal, atomic, spec-aligned.

## Identity
GoToMars – Kotlin Android app showing real-scale (distance) Solar System with Earth↔Mars transfer, OpenGL ES + Compose.

## Workflow (Spec-First)
1. Only implement open task (TASK-###) from tasks.md; one task per PR/commit.
2. Any behavior change → update README.md (requirements) first, then code.
3. No new deps unless README updated (Architecture / NFR section).
4. Commit: `feat|fix|perf|refactor(scope): summary (TASK-###, REQ-###[, REQ-###])`.

## Stack
Kotlin (JDK 11), Android minSdk 31 / compile 36, Jetpack Compose + Material3, OpenGL ES 2.0 (GLSurfaceView), JUnit4 + AndroidX test. No network / persistence libs.

## Structure (target)
```
app/src/main/java/com/ms/gotomars/
  domain/ (models, math, OrbitCalculator, RouteBuilder, FleetManager, AdaptiveQualityController)
  data/ (PlanetCatalog)
  rendering/ (Renderer, GlProgram, SphereMesh, PlanetRenderer, RouteRenderer, FleetRenderer, Camera, ResourceManager)
  ui/ (SurfaceComposable, GestureHandlers, SettingsSheet, LabelRenderer, ConfigViewModel)
  telemetry/ (TelemetryLogger)
```
Tests: unit (domain, adaptive, route), androidTest (smoke, gestures).

## Build & Test (Windows)
Run the following commands in the project root directory:
```
gradlew app:assembleDebug
gradlew app:testDebugUnitTest
gradlew app:connectedDebugAndroidTest
```

## Conventions
* Pure domain logic (no Android refs); immutable data classes.
* Use Double for math; convert to Float for GL buffers.
* Precompute & reuse meshes/buffers; no per-frame allocations.
* One public class per file; descriptive names; constants UPPER_SNAKE_CASE.
* Route & orbit math: analytic (no incremental drift). Time from monotonic clock.
* Adaptive quality adjusts route segments (never below documented floor) only after 3 slow frames.
* Telemetry: in-memory ring buffer; drop oldest.

## Tasks Execution
Follow dependency order in tasks.md. Do not batch multiple tasks. Tests + minimal docs updates inside same change.

## Performance Targets
≥55 FPS median idle; input latency <32 ms; route segments ≥64 pre-adaptive.

## Anti-Patterns
Recomputing meshes per frame; coupling UI state directly to GL objects; adding libs silently; integrating mutable global singletons for domain; frame-time based angle accumulation.

## Shell & OS conventions
- Default OS: Windows 11. Default shell: Windows PowerShell 5.1 (unless stated).
- When writing runnable commands:
    - Prefer multi-line commands or `;` as the separator (PowerShell 5.1).
    - Avoid `&&` unless you confirm PowerShell 7+ or Bash.
    - If success-conditional chaining is required, show both forms:
        - PowerShell 5.1: `build; if ($?) { test }`
        - Bash/cmd/PS 7+: `build && test`
- On Windows use `gradlew` (not `./gradlew`).
