# Photographic Student Room Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task-by-task in the current authorized session.

**Goal:** Ship a photographed 2.5D room and reversible approach to the same live 3×3 book editor.

**Architecture:** Shared photographic cabinet/front coordinates, cached homography and inverse, a single progress controller, independent room/window/foreground transforms, existing Java Views and weather/account services.

**Tech Stack:** Java 17, Android View/Canvas/Matrix/ValueAnimator, WebP, existing Open-Meteo and Google Drive integration, JUnit/Robolectric native graphics.

**Spec:** `docs/superpowers/specs/2026-10-23-student-room-design.md`

## Global Constraints

- minSdk 24/target 35; retain Java/native Views, 3×3 cells and seven slots per cell.
- Preserve sites/categories/notes/favourites/order, persisted finish indices, Google-only accounts and automatic sync.
- Use high quality image assets; no primitive final furniture substitute, no GLB dependency.
- Base reversible transition duration 550 ms; one canonical cabinet Matrix and inverse for rendering/hits.
- Keep city-based time, cached/offline Open-Meteo, lamp control, lifecycle-managed video and motion setting.
- No physical-device/60 fps claim from software-only tests.

## Review Focus

- A reverse request halfway through motion starts at the current pose and cannot run a stale completion callback (Task 2).
- Extremely tall/wide displays retain the cabinet and important room furniture with no empty parallax strips (Task 1).
- A 71+ item/category library pages without book overlap or incorrect destination cell drops (Task 3).
- Restore/cloud updates while the room is visible rebind the live content without resetting the camera or losing account data (Task 3).
- Rotation, pause/destroy and failed weather/video fetches leave a usable static room with truthful status (Task 4).

### Task 1: Production assets and shared geometry

Files: new `RoomScene.java`, `CabinetGeometry.java`, room asset metadata; update `ThemeAssets.java`, `SceneTheme.java`, `BookshelfView.java`, `LibraryRules.java`; geometry tests and asset validation.

Interfaces: `CabinetGeometry.cell(int) -> float[4]`; RoomScene exposes source canvas, normalized cabinet/window/lamp/foreground metadata and stable image-to-viewport transforms.

- [x] Write failing tests for all nine cell bounds, independent room image anchors, aspect ratios and parallax coverage; run them to confirm failures.
- [x] Prepare photographed clean day/night room, alpha foreground and cabinet assets; inspect all outputs; encode WebP without altering originals.
- [x] Implement metadata, shared opening geometry and device-sized asset decoding/cache; retain existing asset fallback paths.
- [x] Run geometry/asset checks and record any missing artwork with exact specifications.

### Task 2: One reversible transition and correct room hits

Files: new `RoomTransition.java`; update `ProjectedCabinetView.java`, `LibraryStage.java`, MainActivity transition methods; transition and native gesture tests.

Interfaces: `RoomTransition.progress()/target()/request(float, boolean)/dispose()`; projected cabinet takes live `BookshelfView` and room quad, exposes canonical hit-cell mapping using its draw Matrix inverse.

- [x] Write and run failing tests for progress continuity on reversal, repeated requests, stale completion suppression, nine transformed touch cells and outside hits.
- [x] Draw the shared photographed frame and live book children through the cached matrix; hand off only at identical endpoint geometry.
- [x] Replace separate position/projection animators with one progress update; preserve interaction guards used by sync.
- [x] Connect room tap, pinch-in, Back, lifecycle restoration and layout rebase. Run transition/gesture tests.

### Task 3: Native book editor integration and persistence

Files: `MainActivity.java`, `BookshelfView.java`, selected existing account/sync tests and new room integration tests.

- [x] Add regression tests for 71+ books per category, live room updates, resize/handoff and persisted library/room state.
- [x] Use the same frame/openings in the native editor, keep drag/drop/info/add/delete/search/paging, rebind projected content after populate.
- [x] Keep account/backup formats and services; use existing deferred remote application during gestures/animations.
- [x] Run the full account, sync and persistence unit suite and inspect actual empty/filled room/editor renders.

### Task 4: Layered room, atmosphere and quality options

Files: new room backdrop/foreground compositor; `StudyLiveView.java`, `MainActivity.java`, asset/video scripts and atmosphere tests.

- [x] Test no cabinet/book reload on weather changes, lamp/day/night states, layer alignment and reduced-effects preference.
- [x] Compose window/room/shadow/cabinet/foreground/light using common scene metadata; apply bounded depth-specific parallax from progress.
- [x] Register existing weather/video effects to the new window; add relit plates and matching media loops; retain stable offline/static fallback.
- [x] Run native day/night/lamp/portrait/landscape renders and lifecycle tests; record performance measurement limits.


### Task 5: Review, build and delivery

Files: version/build/workflow, docs/asset and layer reports, release artifacts.

- [x] Self-review against every spec section; resolve geometry, touch, animation, lifecycle and persistence defects. Native screenshot review additionally found a TextView scroll-space label bug; add parent/projected pixel regression and on-device visible-ink checks.
- [x] Run Java/assets, full Android unit tests, debug/preview/release and instrumentation compilation, lint and release certificate verification.
- [x] Commit source/notes and signed APK to an authorized distribution branch, run GitHub Actions checks and artifact upload; download and compare the public APK.
- [x] Deliver APK and preview links, validation results and exact remaining artwork/device-test limitations.

Final evidence: source `84fb330994cc39ad6403a81da8ba69e441d01f8c`, native QA run 39925249890, signed R8 installation run 39926760328, 53 unit tests, public + Actions APK downloads byte-identical. Source review: PR #1. No physical-device FPS or live Google-account claim.
