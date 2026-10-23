# Original photo and shelf-only correction

The user clarified that the left wall/corner looks missing, that the standalone cabinet looks like a different 3D scene, and that the foreground desk must come from the uploaded target photo itself. They explicitly selected actual stored books inside the shelf area.

Use the uploaded JPEG byte-for-byte as the room. Do not render a separately generated desk, outside landscape, cabinet body or floor shadow over it. Use only a clipped empty 3×3 shelf crop, matching the photographed shelf plane; overlay the actual native book children. Preserve the original desk/wall pixels outside that polygon. Shared photo lighting and cell interpolation preserve continuity into the existing 3×3 native editor. Keep time/weather and cloud data; replace decoder scenery with transparent weather effects over the original window.

- [x] Investigate old clipping/projection and reproduce original-photo and live-book regressions.
- [x] Copy exact uploaded photo and remove independent foreground/furniture/shadow compositing.
- [x] Prepare a shelf-only photographic crop, live cells and common lighting/transition geometry.
- [ ] Inspect empty/filled/day/night/approach renders; run tests, build and lint.
- [ ] Run Android 15 and signed APK installation checks, publish verified download links.
