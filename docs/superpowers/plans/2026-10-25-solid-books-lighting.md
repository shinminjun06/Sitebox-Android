# Solid Books and Natural Window Lighting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Real closed 3D books, physically coherent window light/shadows, and a perspective-correct exterior in the existing SiteBox app.
**Architecture:** Shared pure Java mesh builders feed bounded Filament buffers. BookArtworkView/native controls remain data/input owners; ModelBookLayer renders actual book geometry and a cached title atlas. ModelEnvironment owns the aperture, outdoor meshes and weather materials. RoomLighting owns consistent light parameters.
**Tech Stack:** Java17, Android Views/API24+, Filament/gltfio1.69, compileSDK35.
**Spec:** docs/superpowers/specs/2026-10-25-solid-books-lighting-design.md

## Global Constraints
Preserve GLB bytes, JSON/account/sync, 3×3 grid and7/page, source native controls, shared camera, texture-upload lifetime and post-endFrame retirement. Do not merge main. Use current verification signing key, package com.sitebox.app; new3.3.0 versionCode30. No extra external console setup.

## Review Focus
- Beveled cover/page meshes must be closed, have finite normals and stay inside each cubby.
- Camera and UV/input bounds stay aligned for short, translated and favorite titles on every layout.
- Replacing the back wall must leave an actual hole without removing the left wall/ceiling.
- Lighting changes book/room surfaces consistently; lamp-off must preserve outside sky.
- Rebind, keyboard, drag and rotation must not reuse released sampler/buffer handles.

### Task 1: Real book geometry
Files: ModelMesh.java, SolidBookGeometry.java, ModelBookLayer.java, BookArtworkView.java, RoomModelGeometry.java; tests SolidBookGeometryTest.java.
- [x] RED: test closed volume/depth, outward normals, UV crop, stable dimensions and cubby clearance.
- [x] Implement mesh builder and shared body placement, then GPU integration using the existing safe texture upload/retirement protocol.
- [x] GREEN: gradle :app:testDebugUnitTest --tests '*SolidBookGeometryTest'; compileDebug.

### Task 2: Aperture and exterior
Files: EnvironmentGeometry.java, ModelEnvironment.java, ModelRoomView.java; tests EnvironmentGeometryTest.java.
- [x] RED: rays pass through the real window, strike surrounding wall; exterior has depth and valid finite buffers.
- [x] Implement plaster aperture, merged roof/building/tree detail and sky/glass/weather states. Hide only old back-wall/photo entities.
- [x] GREEN: geometry tests, compileDebug, runtime integration.

### Task 3: Directional lighting
Files: RoomLighting.java, ModelRoomView.java, ModelEnvironment.java; tests RoomLightingTest.java.
- [x] RED: sun enters the aperture, day/night/overcast bounds are finite, shadowed lamp follows actual shade and window is independent of lamp.
- [x] Implement actual wall/window shadows, bounded ambient, shadowed desk spotlight, lit book materials and material roughness.
- [x] GREEN: geometry/lighting and full existing host suite.

### Task 4: Native verification and delivery
Files: ModelSmokeInstrumentation.java, tests/check_model_lighting.py, docs/FULL_ROOM_3D.md, app/build.gradle, existing distribution helpers.
- [ ] Native assertions and captures prove solid book geometry, aperture and real shadowed lights plus all prior CRUD/sync/gestures/weather/lifecycle checks.
- [ ] Fresh whole-branch review, fix important findings, sign3.3.0, full build/lint/native QA, fresh signed APK install.
- [ ] Upload GitHub distribution and redownload both public and Actions APK; document actual measured results and remaining limits.
