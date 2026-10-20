# SiteBox Implementation Plan

**Goal:** Deliver an installable image-based school-library Android app preserving user data and native book dragging.
**Architecture:** Retain Activity data/preferences, extract pure ordering and URL rules, add a bitmap repository and fraction-based furniture view. Raster assets supply visual materials.
**Tech Stack:** Android Java 17, API 24–35, Gradle 8.9/AGP 8.7.3, generated PNG/WebP assets.
**Spec:** docs/superpowers/specs/2026-10-20-school-library-design.md

## Global constraints
- Preserve com.sitebox.app and MainActivity preference identity.
- Preserve CRUD, notes, preview, search, favorites, category renaming and native drag order.
- 3×3 equal cells; independent 3 shelf × 3 book themes.
- No heavy graphics libraries or live procedural grain.

## Review focus
- Filtered or same-category drops must preserve hidden books and relative order.
- Empty cells, populated pages and self-drops must not lose records.
- Small phones/landscape/keyboard must scroll without overlapping header.
- Light book titles and favorite icons must remain visible.
- Missing artwork must retain interactive books and live titles.

## Tasks
- [ ] Add pure-Java regression tests for insertion, URL scheme validation and equal cell geometry; run red.
- [ ] Implement LibraryRules and image-based ThemeAssets/BookshelfView; run tests green.
- [ ] Integrate compact responsive header, insets, dialog actions and persistent themes in MainActivity.
- [ ] Bundle reviewed generated graphics and JSON manifests; remove procedural build generation.
- [ ] Build/test/lint; inspect APK contents; review visuals and functional code, fix findings and build again.
- [ ] Commit/push, verify exact GitHub Actions run, deliver APK and honest QA/provenance report.
